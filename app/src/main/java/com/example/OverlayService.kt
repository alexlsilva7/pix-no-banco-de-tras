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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhonelinkErase
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.filled.WifiTetheringOff
import androidx.compose.material.icons.filled.List
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.utils.getLocalIpAddress
import androidx.compose.material.icons.filled.Bluetooth

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class OverlayService : AccessibilityService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    // Vasculha a tela lendo textos do Android de forma leve
    private fun findKeywordInNode(node: android.view.accessibility.AccessibilityNodeInfo?, keywords: List<String>): Boolean {
        if (node == null) return false
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        
        if (keywords.any { text.contains(it) || desc.contains(it) }) {
            return true
        }
        
        for (i in 0 until node.childCount) {
            if (findKeywordInNode(node.getChild(i), keywords)) return true
        }
        return false
    }

    private fun logAllScreenTexts(node: android.view.accessibility.AccessibilityNodeInfo?, depth: Int = 0) {
        if (node == null) return
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        if (text.isNotEmpty() || desc.isNotEmpty()) {
            // Imprime no console com recuo de árvore para fácil leitura
            val indent = "  ".repeat(depth)
            Log.d("PixDebugScanner", "${indent}Texto: '$text' | Desc: '$desc'")
        }
        for (i in 0 until node.childCount) {
            logAllScreenTexts(node.getChild(i), depth + 1)
        }
    }

    // EXTRAÇÃO DE QR CODE - MÉTODO ZXING (Legado)
    private fun decodeQrCodeZxing(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = com.google.zxing.RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = com.google.zxing.BinaryBitmap(
                com.google.zxing.common.HybridBinarizer(source)
            )

            val hints = mapOf(
                com.google.zxing.DecodeHintType.POSSIBLE_FORMATS to listOf(com.google.zxing.BarcodeFormat.QR_CODE),
                com.google.zxing.DecodeHintType.TRY_HARDER to true
            )
            com.google.zxing.MultiFormatReader().decode(binaryBitmap, hints).text
        } catch (e: com.google.zxing.NotFoundException) {
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // EXTRAÇÃO DE QR CODE - MÉTODO GOOGLE ML KIT (Alta Tolerância e Velocidade)
    private suspend fun decodeQrCodeMlKit(bitmap: Bitmap): String? = suspendCancellableCoroutine { continuation ->
        try {
            val image = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)
            val options = com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
                .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE)
                .build()
            val scanner = com.google.mlkit.vision.barcode.BarcodeScanning.getClient(options)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val qrCode = barcodes.firstOrNull { it.rawValue != null }?.rawValue
                    if (continuation.isActive) continuation.resume(qrCode)
                }
                .addOnFailureListener { e ->
                    if (continuation.isActive) continuation.resume(null)
                }
                .addOnCompleteListener {
                    scanner.close()
                }
        } catch (e: Exception) {
            e.printStackTrace()
            if (continuation.isActive) continuation.resume(null)
        }
    }

    // LÓGICA HÍBRIDA DE EXTRAÇÃO COM SISTEMA DE FALLBACK AUTOMÁTICO
    private suspend fun decodeQrCodeWithFallback(bitmap: Bitmap): String? {
        val primary = qrEngineFlow.value
        return if (primary == "MLKIT") {
            val mlkitResult = decodeQrCodeMlKit(bitmap)
            if (mlkitResult != null) {
                Log.d("PixDebugScanner", "QR Code decodificado pelo mecanismo primário (Google ML Kit)")
                mlkitResult
            } else {
                Log.d("PixDebugScanner", "ML Kit falhou. Iniciando fallback para ZXing...")
                decodeQrCodeZxing(bitmap)
            }
        } else {
            val zxingResult = decodeQrCodeZxing(bitmap)
            if (zxingResult != null) {
                Log.d("PixDebugScanner", "QR Code decodificado pelo mecanismo primário (ZXing)")
                zxingResult
            } else {
                Log.d("PixDebugScanner", "ZXing falhou. Iniciando fallback para Google ML Kit...")
                decodeQrCodeMlKit(bitmap)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkgName = event.packageName?.toString()
        if (pkgName == packageName) return

        val eventTypeStr = AccessibilityEvent.eventTypeToString(event.eventType)
        val currentEvents = debugEventTypes.value.toMutableList()
        currentEvents.add(0, eventTypeStr)
        if (currentEvents.size > 5) {
            currentEvents.removeAt(currentEvents.size - 1)
        }
        debugEventTypes.value = currentEvents

        if (!pkgName.isNullOrEmpty() && debugPackageName.value != pkgName) {
            debugPackageName.value = pkgName
        }

        if (isAutoScanEnabled.value) {
            val target = targetPackageFlow.value
            val currentPkg = debugPackageName.value
            
            if (target.isEmpty() || target == currentPkg) {
                // Palavras-chave para a tela de cobrança da Uber/99
                val keywords = listOf("dinheiro ou pix", "expira em", "cobrar do", "copiar codigo")
                
                val rootNode = rootInActiveWindow
                
                // --- ADICIONE ESTA LINHA PARA IMPRIMIR NO LOGCAT ---
                logAllScreenTexts(rootNode)
                
                val isChargingScreen = findKeywordInNode(rootNode, keywords)
                
                Log.d("PixDebugScanner", "Tela de cobrança detectada? $isChargingScreen | Pacote ativo: $currentPkg")
                rootNode?.recycle() // Importante liberar memória

                val now = System.currentTimeMillis()
                val intervalMs = autoScanIntervalFlow.value * 60 * 1000L

                if (isChargingScreen) {
                    if (now - lastQrCodeFoundTime >= intervalMs) {
                        if (now - lastCaptureTime >= 2000) {
                            lastCaptureTime = now
                            isAutoScanPaused.value = false
                            captureScreenAndSend(isSilent = true)
                        }
                    } else {
                        isAutoScanPaused.value = true
                    }
                } else {
                    // Motorista saiu da tela de cobrança, resetar para a próxima corrida
                    if (isAutoScanPaused.value) {
                        lastQrCodeFoundTime = 0
                        isAutoScanPaused.value = false
                        showOverlayToast("Auto-Scan pronto para próxima corrida")
                    }
                }
            } else {
                isAutoScanPaused.value = true
            }
        }
    }

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

    private val isAutoScanEnabled = kotlinx.coroutines.flow.MutableStateFlow(false)
    private val isAutoScanPaused = kotlinx.coroutines.flow.MutableStateFlow(false)
    private val debugPackageName = kotlinx.coroutines.flow.MutableStateFlow<String>("Aguardando...")
    private val debugEventTypes = kotlinx.coroutines.flow.MutableStateFlow<List<String>>(emptyList())
    private val targetPackageFlow = kotlinx.coroutines.flow.MutableStateFlow("")
    private val isDebugMonitorEnabledFlow = kotlinx.coroutines.flow.MutableStateFlow(false)
    private val autoScanIntervalFlow = kotlinx.coroutines.flow.MutableStateFlow(10)
    private val qrScaleFactorFlow = kotlinx.coroutines.flow.MutableStateFlow(0.5f)
    private val qrEngineFlow = kotlinx.coroutines.flow.MutableStateFlow("MLKIT")
    private lateinit var prefsListener: android.content.SharedPreferences.OnSharedPreferenceChangeListener
    private var autoScanJob: Job? = null
    private var lastQrCodeFoundTime: Long = 0
    private var lastCaptureTime: Long = 0
    private val overlayToastMessage = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private val scannedLogsFlow = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    private fun showOverlayToast(message: String) {
        scope.launch(Dispatchers.Main) {
            overlayToastMessage.value = message
            delay(3000)
            if (overlayToastMessage.value == message) {
                overlayToastMessage.value = null
            }
        }
    }

    private fun toggleAutoScan() {
        isAutoScanEnabled.value = !isAutoScanEnabled.value
        if (isAutoScanEnabled.value) {
            lastQrCodeFoundTime = 0 // Reset pause
            isAutoScanPaused.value = false
            showOverlayToast("Auto-Scan ativado")
        } else {
            isAutoScanPaused.value = false
            showOverlayToast("Auto-Scan desativado")
        }
    }

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

        val prefs = getSharedPreferences("PixPrefs", Context.MODE_PRIVATE)
        targetPackageFlow.value = prefs.getString("TARGET_PACKAGE", "") ?: ""
        isDebugMonitorEnabledFlow.value = prefs.getBoolean("DEBUG_MONITOR_ENABLED", false)
        autoScanIntervalFlow.value = (prefs.getString("AUTO_SCAN_INTERVAL", "10") ?: "10").toIntOrNull() ?: 10
        qrScaleFactorFlow.value = prefs.getFloat("QR_SCALE_FACTOR", 0.5f)
        qrEngineFlow.value = prefs.getString("QR_ENGINE", "MLKIT") ?: "MLKIT"

        prefsListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            when (key) {
                "TARGET_PACKAGE" -> targetPackageFlow.value = sharedPreferences?.getString(key, "") ?: ""
                "DEBUG_MONITOR_ENABLED" -> isDebugMonitorEnabledFlow.value = sharedPreferences?.getBoolean(key, false) ?: false
                "AUTO_SCAN_INTERVAL" -> autoScanIntervalFlow.value = (sharedPreferences?.getString(key, "10") ?: "10").toIntOrNull() ?: 10
                "QR_SCALE_FACTOR" -> qrScaleFactorFlow.value = sharedPreferences?.getFloat(key, 0.5f) ?: 0.5f
                "QR_ENGINE" -> qrEngineFlow.value = sharedPreferences?.getString(key, "MLKIT") ?: "MLKIT"
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)

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
            TcpServer.startServer(this@OverlayService, port)
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
                val prefs = remember { getSharedPreferences("PixPrefs", Context.MODE_PRIVATE) }
                val hotspotSsid = remember { prefs.getString("HOTSPOT_SSID", "AL€X") ?: "AL€X" }
                val hotspotPassword = remember { prefs.getString("HOTSPOT_PASSWORD", "qwertyuiop") ?: "qwertyuiop" }
                
                val connectedClientsList by TcpServer.connectedClients.collectAsState()
                val passengerBattery by TcpServer.clientBatteryState.collectAsState()
                val isServerRunning by TcpServer.isServerRunningState.collectAsState()
                val autoScanState by isAutoScanEnabled.collectAsState()
                val autoScanPausedState by isAutoScanPaused.collectAsState()
                val toastMsg by overlayToastMessage.collectAsState()
                val currentPkg by debugPackageName.collectAsState()
                val currentEvents by debugEventTypes.collectAsState()
                val isDebugEnabled by isDebugMonitorEnabledFlow.collectAsState()
                val scannedLogs by scannedLogsFlow.collectAsState()
                
                MyApplicationTheme {
                    OverlayWidget(
                        connectedClients = connectedClientsList.size,
                        passengerBattery = passengerBattery,
                        isServerRunning = isServerRunning,
                        isAutoScanEnabled = autoScanState,
                        isAutoScanPaused = autoScanPausedState,
                        toastMessage = toastMsg,
                        debugPkgName = currentPkg,
                        debugEventNames = currentEvents,
                        isDebugEnabled = isDebugEnabled,
                        scannedLogs = scannedLogs,
                        onAction = { action ->
                            when (action) {
                                is OverlayAction.Close -> hideBubble()
                                is OverlayAction.ToggleAutoScan -> toggleAutoScan()
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
                                        val wifiPayload = "WIFI:S:$hotspotSsid;T:WPA;P:$hotspotPassword;H:false;;"
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
                                        val wifiPayload = "WIFI:S:$hotspotSsid;T:WPA;P:$hotspotPassword;H:false;;"
                                        TcpServer.sendCommandAndText("CMD_EXIBIR_WIFI", wifiPayload)
                                    }
                                }
                                is OverlayAction.ToggleServer -> {
                                    scope.launch {
                                        if (TcpServer.isRunning) {
                                            TcpServer.stopServer()
                                        } else {
                                            val prefs = getSharedPreferences("PixPrefs", Context.MODE_PRIVATE)
                                            val portString = prefs.getString("PORT", "8080") ?: "8080"
                                            val port = portString.toIntOrNull() ?: 8080
                                            TcpServer.startServer(this@OverlayService, port)
                                        }
                                    }
                                }
                                is OverlayAction.OpenApp -> {
                                    try {
                                        val intent = Intent(this@OverlayService, MainActivity::class.java).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                        }
                                        startActivity(intent)
                                    } catch (e: Exception) {
                                        Log.e("OverlayService", "Erro ao abrir MainActivity: ${e.message}")
                                    }
                                }
                                is OverlayAction.ScanLogs -> {
                                    val rootNode = rootInActiveWindow
                                    val sb = java.lang.StringBuilder()
                                    fun traverse(node: android.view.accessibility.AccessibilityNodeInfo?, depth: Int = 0) {
                                        if (node == null) return
                                        val text = node.text?.toString() ?: ""
                                        val desc = node.contentDescription?.toString() ?: ""
                                        if (text.isNotEmpty() || desc.isNotEmpty()) {
                                            val indent = "  ".repeat(depth)
                                            sb.append("$indent- Texto: '$text' | Desc: '$desc'\n")
                                        }
                                        for (i in 0 until node.childCount) {
                                            traverse(node.getChild(i), depth + 1)
                                        }
                                    }
                                    traverse(rootNode)
                                    if (sb.isEmpty()) {
                                        sb.append("Nenhum texto encontrado na tela.")
                                    }
                                    scannedLogsFlow.value = sb.toString()
                                    rootNode?.recycle()
                                }
                                is OverlayAction.CloseLogsDialog -> {
                                    scannedLogsFlow.value = null
                                }
                                is OverlayAction.ToggleBluetoothServer -> {
                                    scope.launch {
                                        if (com.example.network.BluetoothServerHelper.isBluetoothServerRunning.value) {
                                            com.example.network.BluetoothServerHelper.stopBluetoothServer()
                                        } else {
                                            val ipAddress = getLocalIpAddress()
                                            com.example.network.BluetoothServerHelper.startBluetoothServer(
                                                context = this@OverlayService,
                                                ssid = hotspotSsid,
                                                pass = hotspotPassword,
                                                ip = ipAddress
                                            )
                                        }
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
        com.example.network.BluetoothServerHelper.stopBluetoothServer()
        com.example.network.UdpDiscovery.stopDiscoveryServer()
        scope.cancel()
    }

    private fun captureScreenAndSend(isSilent: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            scope.launch {
                if (!isSilent) {
                    withContext(Dispatchers.Main) {
                        if (::composeView.isInitialized) {
                            composeView.visibility = android.view.View.INVISIBLE
                        }
                    }
                }

                // Timeout de 3 segundos para restaurar visibilidade se takeScreenshot falhar silenciosamente
                val timeoutJob = scope.launch(Dispatchers.Main) {
                    delay(3000)
                    if (!isSilent && ::composeView.isInitialized && composeView.visibility == android.view.View.INVISIBLE) {
                        composeView.visibility = android.view.View.VISIBLE
                        showOverlayToast("Tempo limite de captura excedido.")
                    }
                }

                delay(200)

                takeScreenshot(android.view.Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
                    override fun onSuccess(screenshotResult: AccessibilityService.ScreenshotResult) {
                        timeoutJob.cancel()
                        scope.launch(Dispatchers.Default) {
                            try {
                                val hardwareBuffer = screenshotResult.hardwareBuffer
                                val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, screenshotResult.colorSpace)
                                    ?.copy(Bitmap.Config.ARGB_8888, false)
                                hardwareBuffer.close()

                                if (bitmap != null) {
                                    // Processamento com fallback e prioridade configurada
                                    var qrText = decodeQrCodeWithFallback(bitmap)
                                    
                                    if (qrText == null) {
                                        val scaleFactor = qrScaleFactorFlow.value
                                        if (scaleFactor < 1.0f) {
                                            val scaledBitmap = Bitmap.createScaledBitmap(
                                                bitmap,
                                                (bitmap.width * scaleFactor).toInt(),
                                                (bitmap.height * scaleFactor).toInt(),
                                                false
                                            )
                                            qrText = decodeQrCodeWithFallback(scaledBitmap)
                                            scaledBitmap.recycle()
                                        }
                                    }
                                    bitmap.recycle()

                                    if (qrText != null) {
                                        try {
                                            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                vibrator.vibrate(android.os.VibrationEffect.createOneShot(120, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                                            } else {
                                                @Suppress("DEPRECATION")
                                                vibrator.vibrate(120)
                                            }
                                            val toneG = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 100)
                                            toneG.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 100)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }

                                        withContext(Dispatchers.Main) {
                                            if (!isSilent && ::composeView.isInitialized) {
                                                composeView.visibility = android.view.View.VISIBLE
                                            }
                                            if (!isSilent) {
                                                showOverlayToast("QR Code extraído e enviado!")
                                            } else {
                                                lastQrCodeFoundTime = System.currentTimeMillis()
                                                isAutoScanPaused.value = true
                                                showOverlayToast("QR Code detectado automaticamente!")
                                            }
                                        }
                                        TcpServer.sendCommandAndText("CMD_EXIBIR_PIX", qrText)
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            if (!isSilent && ::composeView.isInitialized) {
                                                composeView.visibility = android.view.View.VISIBLE
                                            }
                                            if (!isSilent) showOverlayToast("Nenhum QR Code encontrado na tela.")
                                        }
                                    }
                                } else {
                                    restoreViewAndShowError(isSilent)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                restoreViewAndShowError(isSilent, "Erro ao processar imagem: ${e.message}")
                            }
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        timeoutJob.cancel()
                        val errorMsg = when(errorCode) {
                            1 -> "Erro Interno (1)"
                            2 -> "Sem acesso de acessibilidade (2)"
                            3 -> "Tempo muito curto (3)"
                            4 -> "Display inválido (4)"
                            else -> "Erro desconhecido ($errorCode)"
                        }
                        restoreViewAndShowError(isSilent, "Falha na captura: $errorMsg")
                    }
                })
            }
        } else {
            showOverlayToast("Captura de tela via Acessibilidade disponível apenas no Android 11+")
        }
    }

    private fun restoreViewAndShowError(isSilent: Boolean = false, errorMessage: String? = null) {
        scope.launch(Dispatchers.Main) {
            if (!isSilent && ::composeView.isInitialized) {
                composeView.visibility = android.view.View.VISIBLE
            }
            if (!isSilent) showOverlayToast(errorMessage ?: "Falha na captura.")
        }
    }
}

sealed class OverlayAction {
    data object ToggleAutoScan : OverlayAction()
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
    
    // NOVAS AÇÕES ADICIONADAS
    data object ToggleServer : OverlayAction()
    data object OpenApp : OverlayAction()
    data object ScanLogs : OverlayAction()
    data object CloseLogsDialog : OverlayAction()
    data object ToggleBluetoothServer : OverlayAction()
}

@Composable
fun OverlayWidget(
    connectedClients: Int,
    passengerBattery: Int = -1,
    isServerRunning: Boolean,
    isAutoScanEnabled: Boolean = false,
    isAutoScanPaused: Boolean = false,
    toastMessage: String? = null,
    debugPkgName: String = "",
    debugEventNames: List<String> = emptyList(),
    isDebugEnabled: Boolean = false,
    scannedLogs: String? = null,
    onAction: (OverlayAction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    val containerGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xF21C1C26),
            Color(0xF212121A)
        )
    )
    val menuBorderColor = Color(0x22FFFFFF)

    Column(
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
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
                    .border(
                        3.dp, 
                        if (isAutoScanEnabled && isAutoScanPaused) Color(0xFFFFEB3B) 
                        else if (isAutoScanEnabled) Color(0xFF2196F3) 
                        else if (connectedClients > 0) Color(0xFF4CAF50) 
                        else Color(0x33FFFFFF),
                        CircleShape
                    )
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_launcher_foreground), 
                        contentDescription = "Menu", 
                        tint = Color.Unspecified,
                        modifier = Modifier.size(36.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(18.dp)
                            .background(if (connectedClients > 0) Color(0xFF4CAF50) else Color(0xFFE53935), CircleShape)
                            .border(1.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Text(
                            text = connectedClients.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (expanded) {
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .width(300.dp)
                        .background(containerGradient, RoundedCornerShape(24.dp))
                        .border(1.dp, menuBorderColor, RoundedCornerShape(24.dp))
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // REVERTIDO: Exibição apenas da bateria tradicional do passageiro
                    if (passengerBattery >= 0) {
                        val batteryColor = when {
                            passengerBattery <= 20 -> Color(0xFFEF5350)
                            passengerBattery <= 50 -> Color(0xFFFFB74D)
                            else -> Color(0xFF81C784)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                        ) {
                            androidx.compose.material3.Text(
                                text = "🔋 Bateria do Passageiro: $passengerBattery%",
                                color = batteryColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                    }

                    // 1. RECURSOS DE APOIO NO TOPO (Estrutura de 4 colunas)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                MenuActionButton(
                                    icon = Icons.Default.Autorenew,
                                    label = if (isAutoScanEnabled) "Parar Auto" else "Auto Scan",
                                    tint = if (isAutoScanEnabled) Color(0xFFE57373) else Color(0xFF64B5F6),
                                    onClick = { expanded = false; onAction(OverlayAction.ExpandChanged(false)); onAction(OverlayAction.ToggleAutoScan) }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                MenuActionButton(
                                    icon = Icons.Default.DirectionsCar,
                                    label = "Bem-Vindo",
                                    tint = Color(0xFF4DD0E1),
                                    onClick = { expanded = false; onAction(OverlayAction.ExpandChanged(false)); onAction(OverlayAction.SendWelcome) }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                MenuActionButton(
                                    icon = Icons.Default.Favorite,
                                    label = "Obrigado",
                                    tint = Color(0xFFF06292),
                                    onClick = { expanded = false; onAction(OverlayAction.ExpandChanged(false)); onAction(OverlayAction.SendThanks) }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                MenuActionButton(
                                    icon = Icons.Default.Delete,
                                    label = "Limpar",
                                    tint = Color(0xFFFFB74D),
                                    onClick = { expanded = false; onAction(OverlayAction.ExpandChanged(false)); onAction(OverlayAction.ClearScreen) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                MenuActionButton(
                                    icon = Icons.Default.DirectionsCar,
                                    label = "Abrir App",
                                    tint = Color(0xFF4DD0E1),
                                    onClick = { 
                                        expanded = false
                                        onAction(OverlayAction.ExpandChanged(false))
                                        onAction(OverlayAction.OpenApp) 
                                    }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                val isBtServerRunning by com.example.network.BluetoothServerHelper.isBluetoothServerRunning.collectAsState()
                                MenuActionButton(
                                    icon = Icons.Default.Bluetooth,
                                    label = if (isBtServerRunning) "Parar BT" else "Ligar BT",
                                    tint = if (isBtServerRunning) Color(0xFFE57373) else Color(0xFF9575CD),
                                    onClick = { 
                                        onAction(OverlayAction.ToggleBluetoothServer)
                                    }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                MenuActionButton(
                                    icon = if (isServerRunning) Icons.Default.WifiTetheringOff else Icons.Default.WifiTethering,
                                    label = if (isServerRunning) "Parar Ser." else "Ligar Ser.",
                                    tint = if (isServerRunning) Color(0xFFE57373) else Color(0xFF81C784),
                                    onClick = { onAction(OverlayAction.ToggleServer) }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                MenuActionButton(
                                    icon = Icons.Default.Close,
                                    label = "Fechar",
                                    tint = Color.White,
                                    onClick = { expanded = false; onAction(OverlayAction.ExpandChanged(false)); onAction(OverlayAction.Close) }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))

                    // 2. BOTÕES PRINCIPAIS DE DESTAQUE EMBAIXO (Estrutura Horizontal 2x2 Ampliada)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                LargeMenuActionButton(
                                    icon = Icons.Default.CameraAlt,
                                    label = "Capturar",
                                    tint = Color(0xFF64B5F6),
                                    onClick = { expanded = false; onAction(OverlayAction.ExpandChanged(false)); onAction(OverlayAction.Capture) }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                LargeMenuActionButton(
                                    icon = Icons.Default.Payments,
                                    label = "Meu Pix",
                                    tint = Color(0xFF81C784),
                                    onClick = { expanded = false; onAction(OverlayAction.ExpandChanged(false)); onAction(OverlayAction.SendMyPix) }
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                LargeMenuActionButton(
                                    icon = Icons.Default.Wifi,
                                    label = "Wi-Fi",
                                    tint = Color(0xFFBA68C8),
                                    onClick = { expanded = false; onAction(OverlayAction.ExpandChanged(false)); onAction(OverlayAction.SendWifi) }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                LargeMenuActionButton(
                                    icon = Icons.Default.PhonelinkErase,
                                    label = "Apagar",
                                    tint = Color(0xFFE57373),
                                    onClick = { expanded = false; onAction(OverlayAction.ExpandChanged(false)); onAction(OverlayAction.TurnOffScreen) }
                                )
                            }
                        }
                    }
                }
            }
        } // Fecha a Row principal
        
        if (isDebugEnabled && !expanded) {
            Surface(
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    androidx.compose.material3.Text(
                        text = debugPkgName,
                        color = Color.Green,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    debugEventNames.forEach { eventName ->
                        androidx.compose.material3.Text(
                            text = eventName,
                            color = Color(0xFF81C784),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Logs Dialog
        if (scannedLogs != null) {
            Surface(
                color = Color(0xF21C1C26),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(0.9f)
                    .height(300.dp)
                    .border(1.dp, menuBorderColor, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Text(
                            text = "Textos Capturados",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        IconButton(
                            onClick = { onAction(OverlayAction.CloseLogsDialog) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        androidx.compose.material3.Text(
                            text = scannedLogs,
                            color = Color(0xFFE0E0E0),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Custom Toast Box
        androidx.compose.animation.AnimatedVisibility(
            visible = toastMessage != null,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically()
        ) {
            if (toastMessage != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xDD333333),
                    modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                ) {
                    androidx.compose.material3.Text(
                        text = toastMessage,
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
    ) {
        Icon(
            imageVector = icon, 
            contentDescription = label, 
            tint = tint,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun LargeMenuActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .background(tint.copy(alpha = 0.1f))
            .border(1.dp, tint.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
    ) {
        Icon(
            imageVector = icon, 
            contentDescription = label, 
            tint = tint,
            modifier = Modifier.size(48.dp)
        )
    }
}
