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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Wifi
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
                val connectedClientsList by TcpServer.connectedClients.collectAsState()
                MyApplicationTheme {
                    OverlayWidget(
                        connectedClients = connectedClientsList.size,
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
                                TcpServer.sendCommandAndText("CMD_EXIBIR_MEU_PIX", pixPayload)
                            }
                        },
                        onEnviarMeuWifi = {
                            scope.launch {
                                val wifiPayload = "WIFI:S:AL€X;T:WPA;P:qwertyuiop;H:false;;"
                                TcpServer.sendCommandAndText("CMD_EXIBIR_WIFI", wifiPayload)
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
    onEnviarMeuPix: () -> Unit = {},
    onEnviarMeuWifi: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    
    val menuContainerColor = Color(0xDD1E1E1E) // Semi-transparent dark
    val menuBorderColor = Color(0x55FFFFFF)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(16.dp)
    ) {
        // Main Bubble
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 12.dp,
            modifier = Modifier
                .size(64.dp)
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
                .border(2.dp, if (connectedClients > 0) Color(0xFF4CAF50) else Color.Transparent, CircleShape)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    Icons.Default.QrCode, 
                    contentDescription = "Menu", 
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
                
                // Connection indicator badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp)
                        .background(if (connectedClients > 0) Color(0xFF4CAF50) else Color(0xFFE53935), CircleShape)
                        .border(1.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text = connectedClients.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        }

        // Expanded Menu
        if (expanded) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .background(menuContainerColor, RoundedCornerShape(24.dp))
                    .border(1.dp, menuBorderColor, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Linha de Cima
                Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MenuActionButton(
                        icon = Icons.Default.CameraAlt,
                        label = "Capturar",
                        tint = Color(0xFF64B5F6),
                        onClick = { expanded = false; onExpandedChanged(false); onCapture() }
                    )
                    MenuActionButton(
                        icon = Icons.Default.Payments,
                        label = "Meu Pix",
                        tint = Color(0xFF81C784),
                        onClick = { expanded = false; onExpandedChanged(false); onEnviarMeuPix() }
                    )
                    MenuActionButton(
                        icon = Icons.Default.Wifi,
                        label = "Wi-Fi",
                        tint = Color(0xFFBA68C8),
                        onClick = { expanded = false; onExpandedChanged(false); onEnviarMeuWifi() }
                    )
                }
                
                // Divisor Horizontal
                Box(modifier = Modifier.size(140.dp, 1.dp).background(Color.Gray.copy(alpha = 0.5f)))
                
                // Linha de Baixo
                Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MenuActionButton(
                        icon = Icons.Default.Delete,
                        label = "Limpar",
                        tint = Color(0xFFFFB74D),
                        onClick = { expanded = false; onExpandedChanged(false); onLimparTela() }
                    )
                    MenuActionButton(
                        icon = Icons.Default.PhonelinkErase,
                        label = "Apagar",
                        tint = Color(0xFFE57373),
                        onClick = { expanded = false; onExpandedChanged(false); onApagarTela() }
                    )
                    MenuActionButton(
                        icon = Icons.Default.Close,
                        label = "Fechar",
                        tint = Color.White,
                        onClick = { expanded = false; onExpandedChanged(false); onClose() }
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    androidx.compose.foundation.layout.Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Icon(
            imageVector = icon, 
            contentDescription = label, 
            tint = tint,
            modifier = Modifier.size(28.dp)
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
        androidx.compose.material3.Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )
    }
}
