package com.example

import android.util.Log
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
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
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
    private var isLifecycleInitialized = false

    override fun onCreate() {
        super.onCreate()
        if (!isLifecycleInitialized) {
            try {
                savedStateRegistryController.performRestore(null)
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            } catch (e: Exception) {
                Log.e("OverlayService", "Erro ao inicializar ciclo de vida: ${e.message}")
            }
            isLifecycleInitialized = true
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
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        scope.launch {
            val prefs = getSharedPreferences("PixPrefs", Context.MODE_PRIVATE)
            val portString = prefs.getString("PORT", "8080") ?: "8080"
            val port = portString.toIntOrNull() ?: 8080
            TcpServer.startServer(port)
        }
        scope.launch {
            com.example.network.UdpDiscovery.startDiscoveryServer()
        }

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

    private fun createComposeView(): ComposeView {
        val view = ComposeView(this)
        view.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val connectedClientsList by TcpServer.connectedClients.collectAsState()
                MyApplicationTheme {
                    OverlayWidget(
                        connectedClients = connectedClientsList.size,
                        onAction = { action ->
                            when (action) {
                                is OverlayAction.Close -> hideBubble()
                                is OverlayAction.Drag -> {
                                    windowParams.x = (windowParams.x + action.dx).toInt()
                                    windowParams.y = (windowParams.y + action.dy).toInt()
                                    if (isViewAdded) {
                                        try {
                                            windowManager.updateViewLayout(view, windowParams)
                                        } catch (e: Exception) {
                                            Log.e("OverlayService", "Erro no arrasto: ${e.message}")
                                        }
                                    }
                                }
                                is OverlayAction.Capture -> captureScreenAndSend()
                                is OverlayAction.ClearScreen -> {
                                    scope.launch {
                                        TcpServer.sendCommand("CMD_LIMPAR_TELA")
                                    }
                                }
                                is OverlayAction.TurnOffScreen -> {
                                    scope.launch {
                                        TcpServer.sendCommand("CMD_APAGAR_TELA")
                                    }
                                }
                                is OverlayAction.SendWelcome -> {
                                    scope.launch {
                                        val wifiPayload = "WIFI:S:AL'X;T:WPA;P:qwertyuiop;H:false;;"
                                        TcpServer.sendCommandAndText("CMD_EXIBIR_BEM_VINDO", wifiPayload)
                                    }
                                }
                                is OverlayAction.SendThanks -> {
                                    scope.launch {
                                        TcpServer.sendCommandAndText("CMD_EXIBIR_OBRIGADO", "")
                                    }
                                }
                                is OverlayAction.ExpandChanged -> {
                                    if (isViewAdded) {
                                        try {
                                            windowManager.updateViewLayout(view, windowParams)
                                        } catch (e: Exception) {
                                            Log.e("OverlayService", "Erro ao atualizar layout: ${e.message}")
                                        }
                                    }
                                }
                                is OverlayAction.SendMyPix -> {
                                    scope.launch {
                                        val pixPayload = "00020101021126360014br.gov.bcb.pix0114+55879815049025204000053039865802BR5919Alex Lopes da Silva6011GaranhunsPE62070503***6304539E"
                                        TcpServer.sendCommandAndText("CMD_EXIBIR_MEU_PIX", pixPayload)
                                    }
                                }
                                is OverlayAction.SendWifi -> {
                                    scope.launch {
                                        val wifiPayload = "WIFI:S:AL€X;T:WPA;P:qwertyuiop;H:false;;"
                                        TcpServer.sendCommandAndText("CMD_EXIBIR_WIFI", wifiPayload)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
        return view
    }

    private fun showBubble() {
        if (isViewAdded && ::composeView.isInitialized) {
            hideBubble()
        }

        composeView = createComposeView()

        try {
            windowManager.addView(composeView, windowParams)
            isViewAdded = true
        } catch (e: Exception) {
            Log.e("OverlayService", "Erro ao exibir bolha: ${e.message}")
            isViewAdded = false
        }
    }

    private fun hideBubble() {
        if (::composeView.isInitialized && isViewAdded) {
            try {
                windowManager.removeViewImmediate(composeView)
            } catch (e: Exception) {
                Log.e("OverlayService", "Erro ao ocultar bolha: ${e.message}")
            } finally {
                isViewAdded = false
            }
        }
        if (::composeView.isInitialized) {
            try {
                composeView.disposeComposition()
            } catch (e: Exception) {
                Log.e("OverlayService", "Erro ao descartar composicao: ${e.message}")
            }
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        hideBubble()
        return super.onUnbind(intent)
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
                    if (::composeView.isInitialized) {
                        composeView.visibility = android.view.View.INVISIBLE
                    }
                }

                // Timeout de 3 segundos para restaurar visibilidade se takeScreenshot falhar silenciosamente
                val timeoutJob = scope.launch(Dispatchers.Main) {
                    delay(3000)
                    if (::composeView.isInitialized && composeView.visibility == android.view.View.INVISIBLE) {
                        composeView.visibility = android.view.View.VISIBLE
                        Toast.makeText(this@OverlayService, "Tempo limite de captura excedido.", Toast.LENGTH_SHORT).show()
                    }
                }

                delay(200)

                takeScreenshot(android.view.Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
                    override fun onSuccess(screenshotResult: AccessibilityService.ScreenshotResult) {
                        timeoutJob.cancel()
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
                                            if (::composeView.isInitialized) {
                                                composeView.visibility = android.view.View.VISIBLE
                                            }
                                            Toast.makeText(this@OverlayService, "QR Code extraído e enviado!", Toast.LENGTH_SHORT).show()
                                        }
                                        withContext(Dispatchers.IO) {
                                            TcpServer.sendCommandAndText("CMD_EXIBIR_PIX", qrText)
                                        }
                                    }
                                } else {
                                    scope.launch(Dispatchers.Main) {
                                        if (::composeView.isInitialized) {
                                            composeView.visibility = android.view.View.VISIBLE
                                        }
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
                        timeoutJob.cancel()
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
            if (::composeView.isInitialized) {
                composeView.visibility = android.view.View.VISIBLE
            }
            Toast.makeText(this@OverlayService, "Falha na captura.", Toast.LENGTH_SHORT).show()
        }
    }
}

sealed class OverlayAction {
    data object Capture : OverlayAction()
    data object ClearScreen : OverlayAction()
    data object TurnOffScreen : OverlayAction()
    data object SendMyPix : OverlayAction()
    data object SendWifi : OverlayAction()
    data object SendWelcome : OverlayAction()
    data object SendThanks : OverlayAction()
    data object Close : OverlayAction()
    data class Drag(val dx: Float, val dy: Float) : OverlayAction()
    data class ExpandChanged(val expanded: Boolean) : OverlayAction()
}

@Composable
fun OverlayWidget(
    connectedClients: Int,
    onAction: (OverlayAction) -> Unit
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
                        onAction(OverlayAction.Drag(dragAmount.x, dragAmount.y))
                    }
                }
                .clickable { 
                    expanded = !expanded 
                    onAction(OverlayAction.ExpandChanged(expanded))
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
                        onClick = { expanded = false; onAction(OverlayAction.ExpandChanged(false)); onAction(OverlayAction.Capture) }
                    )
                    MenuActionButton(
                        icon = Icons.Default.Payments,
                        label = "Meu Pix",
                        tint = Color(0xFF81C784),
                        onClick = { expanded = false; onAction(OverlayAction.ExpandChanged(false)); onAction(OverlayAction.SendMyPix) }
                    )
                    MenuActionButton(
                        icon = Icons.Default.Wifi,
                        label = "Wi-Fi",
                        tint = Color(0xFFBA68C8),
                        onClick = { expanded = false; onAction(OverlayAction.ExpandChanged(false)); onAction(OverlayAction.SendWifi) }
                    )
                }
                
                // Divisor Horizontal
                Box(modifier = Modifier.size(140.dp, 1.dp).background(Color.Gray.copy(alpha = 0.5f)))
                
                // Linha do Meio
                Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MenuActionButton(
                        icon = Icons.Default.DirectionsCar,
                        label = "Bem-Vindo",
                        tint = Color(0xFF4DD0E1),
                        onClick = { expanded = false; onAction(OverlayAction.ExpandChanged(false)); onAction(OverlayAction.SendWelcome) }
                    )
                    MenuActionButton(
                        icon = Icons.Default.Favorite,
                        label = "Obrigado",
                        tint = Color(0xFFF06292),
                        onClick = { expanded = false; onAction(OverlayAction.ExpandChanged(false)); onAction(OverlayAction.SendThanks) }
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
                        onClick = { expanded = false; onAction(OverlayAction.ExpandChanged(false)); onAction(OverlayAction.ClearScreen) }
                    )
                    MenuActionButton(
                        icon = Icons.Default.PhonelinkErase,
                        label = "Apagar",
                        tint = Color(0xFFE57373),
                        onClick = { expanded = false; onAction(OverlayAction.ExpandChanged(false)); onAction(OverlayAction.TurnOffScreen) }
                    )
                    MenuActionButton(
                        icon = Icons.Default.Close,
                        label = "Fechar",
                        tint = Color.White,
                        onClick = { expanded = false; onAction(OverlayAction.ExpandChanged(false)); onAction(OverlayAction.Close) }
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
