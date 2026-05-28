package com.example

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.TakeScreenshotCallback
import android.view.accessibility.AccessibilityEvent
import android.hardware.HardwareBuffer
import android.graphics.ColorSpace
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import android.os.Build
import android.util.DisplayMetrics
import androidx.core.app.NotificationCompat
import android.app.Activity
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.*
import com.example.network.TcpServer
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhonelinkErase
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme

class OverlayService : AccessibilityService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    private lateinit var windowManager: WindowManager
    private lateinit var composeView: ComposeView

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val CHANNEL_ID = "OverlayChannel"
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private var isViewAdded = false
    private lateinit var windowParams: WindowManager.LayoutParams

    override fun onServiceConnected() {
        super.onServiceConnected()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        scope.launch {
            val prefs = getSharedPreferences("PixPrefs", Context.MODE_PRIVATE)
            val portString = prefs.getString("PORT", "8080") ?: "8080"
            val port = portString.toIntOrNull() ?: 8080
            TcpServer.startServer(port)
        }
        scope.launch {
            com.example.network.UdpDiscovery.startDiscoveryServer()
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val connectedClients by TcpServer.connectedClientsCount.collectAsState()
                MyApplicationTheme {
                    OverlayWidget(
                        connectedClients = connectedClients,
                        onClose = { hideBubble() },
                        onDrag = { dx, dy ->
                            windowParams.x = (windowParams.x + dx).toInt()
                            windowParams.y = (windowParams.y + dy).toInt()
                            if (isViewAdded) windowManager.updateViewLayout(composeView, windowParams)
                        },
                        onCapture = { 
                            captureScreenAndSend()
                        },
                        onLimparTela = {
                            scope.launch {
                                TcpServer.sendCommand("CMD_LIMPAR_TELA")
                            }
                        },
                        onApagarTela = {
                            scope.launch {
                                TcpServer.sendCommand("CMD_APAGAR_TELA")
                            }
                        },
                        onExpandedChanged = {
                            if (isViewAdded) windowManager.updateViewLayout(composeView, windowParams)
                        },
                        onEnviarMeuPix = {
                            scope.launch {
                                val pixPayload = "00020101021126360014br.gov.bcb.pix0114+55879815049025204000053039865802BR5919Alex Lopes da Silva6011GaranhunsPE62070503***6304539E"
                                TcpServer.sendCommandAndText("CMD_EXIBIR_PIX", pixPayload)
                            }
                        }
                    )
                }
            }
        }

        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeViewModelStoreOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)

        scope.launch {
            TcpServer.isServerRunningState.collect { isRunning ->
                withContext(Dispatchers.Main) {
                    if (isRunning) {
                        showBubble()
                    } else {
                        hideBubble()
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.getStringExtra("action")) {
            "SHOW_BUBBLE" -> showBubble()
            "HIDE_BUBBLE" -> hideBubble()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun showBubble() {
        if (::composeView.isInitialized && !isViewAdded) {
            windowManager.addView(composeView, windowParams)
            isViewAdded = true
        }
    }

    private fun hideBubble() {
        if (::composeView.isInitialized && isViewAdded) {
            windowManager.removeView(composeView)
            isViewAdded = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        hideBubble()
        store.clear()
        TcpServer.stopServer()
        com.example.network.UdpDiscovery.stopDiscoveryServer()
        scope.cancel()
    }

    private fun captureScreenAndSend() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            scope.launch {
                withContext(Dispatchers.Main) {
                    composeView.visibility = android.view.View.INVISIBLE
                }
                delay(200)

                takeScreenshot(android.view.Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
                    override fun onSuccess(screenshotResult: AccessibilityService.ScreenshotResult) {
                        try {
                            val hardwareBuffer = screenshotResult.hardwareBuffer
                            val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, screenshotResult.colorSpace)
                                ?.copy(Bitmap.Config.ARGB_8888, false)
                            hardwareBuffer.close()

                            if (bitmap != null) {
                                // Decode QR code from screenshot using ZXing
                                val width = bitmap.width
                                val height = bitmap.height
                                val pixels = IntArray(width * height)
                                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                                bitmap.recycle()

                                val source = com.google.zxing.RGBLuminanceSource(width, height, pixels)
                                val binaryBitmap = com.google.zxing.BinaryBitmap(
                                    com.google.zxing.common.HybridBinarizer(source)
                                )

                                val qrText: String? = try {
                                    val hints = mapOf(
                                        com.google.zxing.DecodeHintType.POSSIBLE_FORMATS to listOf(com.google.zxing.BarcodeFormat.QR_CODE),
                                        com.google.zxing.DecodeHintType.TRY_HARDER to true
                                    )
                                    val result = com.google.zxing.MultiFormatReader().decode(binaryBitmap, hints)
                                    result.text
                                } catch (e: com.google.zxing.NotFoundException) {
                                    null
                                }

                                if (qrText != null) {
                                    scope.launch {
                                        withContext(Dispatchers.Main) {
                                            composeView.visibility = android.view.View.VISIBLE
                                            Toast.makeText(this@OverlayService, "QR Code extraído e enviado!", Toast.LENGTH_SHORT).show()
                                        }
                                        withContext(Dispatchers.IO) {
                                            TcpServer.sendCommandAndText("CMD_EXIBIR_PIX", qrText)
                                        }
                                    }
                                } else {
                                    scope.launch(Dispatchers.Main) {
                                        composeView.visibility = android.view.View.VISIBLE
                                        Toast.makeText(this@OverlayService, "Nenhum QR Code encontrado na tela.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                restoreViewAndShowError()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            restoreViewAndShowError()
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        restoreViewAndShowError()
                    }
                })
            }
        } else {
            Toast.makeText(this, "Captura de tela via Acessibilidade disponível apenas no Android 11+", Toast.LENGTH_LONG).show()
        }
    }

    private fun restoreViewAndShowError() {
        scope.launch(Dispatchers.Main) {
            composeView.visibility = android.view.View.VISIBLE
            Toast.makeText(this@OverlayService, "Falha na captura.", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun OverlayWidget(
    connectedClients: Int,
    onClose: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onCapture: () -> Unit,
    onLimparTela: () -> Unit,
    onApagarTela: () -> Unit,
    onExpandedChanged: (Boolean) -> Unit,
    onEnviarMeuPix: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 8.dp,
            modifier = Modifier
                .size(60.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                }
                .clickable { 
                    expanded = !expanded 
                    onExpandedChanged(expanded)
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.QrCode, contentDescription = "Menu", tint = Color.White)
                
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(18.dp)
                        .background(if (connectedClients > 0) Color.Green else Color.Red, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text = connectedClients.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        }

        AnimatedVisibility(visible = expanded) {
            Row(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(30.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(30.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCapture) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Capturar QR Code", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onLimparTela) {
                    Icon(Icons.Default.Delete, contentDescription = "Limpar Tela", tint = MaterialTheme.colorScheme.error)
                }
                IconButton(onClick = onApagarTela) {
                    Icon(Icons.Default.PhonelinkErase, contentDescription = "Apagar Tela", tint = MaterialTheme.colorScheme.secondary)
                }
                IconButton(onClick = onEnviarMeuPix) {
                    Icon(Icons.Default.Payments, contentDescription = "Enviar Meu Pix", tint = Color(0xFF4CAF50))
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar")
                }
            }
        }
    }
}
