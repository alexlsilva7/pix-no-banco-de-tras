package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import com.example.network.TcpServer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.graphics.BitmapFactory
import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.text.selection.SelectionContainer
import com.example.network.TcpClient
import kotlinx.coroutines.launch
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhonelinkErase
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.filled.WifiTetheringOff
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt


import com.example.utils.*
import com.example.MyDeviceAdminReceiver
import com.example.PixActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.os.BatteryManager
import android.content.IntentFilter

@Composable
fun PassengerScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("PixPrefs", android.content.Context.MODE_PRIVATE) }
    var serverIp by remember { mutableStateOf(prefs.getString("LAST_IP", "192.168.") ?: "192.168.") }
    var isDiscovering by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    var autoReconnect by remember { mutableStateOf(prefs.getBoolean("AUTO_RECONNECT", true)) }
    var connectionTrigger by remember { mutableStateOf(0) }
    
    val receivedImage by TcpClient.receivedImage.collectAsState()
    val qrCodeText by TcpClient.qrCodeText.collectAsState()
    val command by TcpClient.command.collectAsState()
    val isConnected by TcpClient.isConnected.collectAsState()
    
    val dpm = remember { context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    val adminComponent = remember { ComponentName(context, MyDeviceAdminReceiver::class.java) }
    var isAdminActive by remember { mutableStateOf(dpm.isAdminActive(adminComponent)) }
    
    val passengerOrientation = remember { prefs.getString("PASSENGER_ORIENTATION", "LANDSCAPE") ?: "LANDSCAPE" }
    val displayMode = remember { prefs.getString("PASSENGER_DISPLAY_MODE", "FULLSCREEN") ?: "FULLSCREEN" }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidthDp = configuration.screenWidthDp
    val useHorizontalLayout = isLandscape && screenWidthDp >= 640

    val adminLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
        isAdminActive = dpm.isAdminActive(adminComponent)
    }

    DisposableEffect(passengerOrientation) {
        val activity = context.getActivity()
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = when (passengerOrientation) {
            "PORTRAIT" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "REVERSE_PORTRAIT" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            "REVERSE_LANDSCAPE" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            "AUTO" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            else -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        
        val window = activity?.window
        if (window != null) {
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
            val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
        
        onDispose {
            if (originalOrientation != null) {
                activity?.requestedOrientation = originalOrientation
            }
            if (window != null) {
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)
                val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                windowInsetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // REVERTIDO: Voltamos para a escuta de bateria tradicional de 60 segundos
    fun getBatteryPercentage(ctx: Context): Int {
        val batteryStatus = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100 / scale) else -1
    }

    LaunchedEffect(isConnected) {
        if (isConnected) {
            while (TcpClient.isConnected.value) {
                val pct = getBatteryPercentage(context)
                if (pct >= 0) {
                    TcpClient.sendTelemetry("TELEMETRY_BATTERY:$pct")
                }
                kotlinx.coroutines.delay(60000L) // Atualiza a cada 60 segundos de forma leve
            }
        }
    }

    LaunchedEffect(isConnected, autoReconnect, connectionTrigger) {
        if (!isConnected && autoReconnect) {
            while (!TcpClient.isConnected.value && autoReconnect) {
                val currentPort = prefs.getString("PORT", "8080")?.toIntOrNull() ?: 8080
                val lastIp = prefs.getString("LAST_IP", "")
                
                // 1. Tenta reconectar imediatamente no último IP conhecido
                if (!lastIp.isNullOrEmpty() && lastIp != "192.168.") {
                    TcpClient.connect(lastIp, currentPort)
                }

                // Se o connect() acima falhou, a conexão continua false
                if (!TcpClient.isConnected.value && autoReconnect) {
                    // 2. BACKOFF: Aguarda 2 segundos para não sobrecarregar a rede
                    kotlinx.coroutines.delay(2000)
                    
                    // 3. Tenta encontrar o IP na rede (Auto-Discovery)
                    isDiscovering = true
                    val discoveredIp = com.example.network.UdpDiscovery.discoverServerIp()
                    isDiscovering = false
                    
                    if (discoveredIp != null) {
                        serverIp = discoveredIp
                        prefs.edit().putString("LAST_IP", discoveredIp).apply()
                        TcpClient.connect(discoveredIp, currentPort)
                    } else {
                        // 4. Se não achou nada, aguarda mais 2 segundos antes de reiniciar o laço
                        kotlinx.coroutines.delay(2000)
                    }
                }
            }
        }
    }

    val hasContent = receivedImage != null || qrCodeText != null

    LaunchedEffect(hasContent) {
        if (hasContent) {
            val maxBrightness = prefs.getBoolean("MAX_BRIGHTNESS", true)
            context.getActivity()?.let { activity ->
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                if (maxBrightness) {
                    val lp = activity.window.attributes
                    lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                    activity.window.attributes = lp
                }
            }
            val safetyTimeoutMinutes = prefs.getString("SAFETY_TIMEOUT", "2")?.toIntOrNull() ?: 2
            if (safetyTimeoutMinutes > 0) {
                val timeoutMs = safetyTimeoutMinutes * 60_000L
                kotlinx.coroutines.delay(timeoutMs)
                TcpClient.clearImage()
                
                val offScreenBehavior = prefs.getString("OFF_SCREEN_BEHAVIOR", "LOCK") ?: "LOCK"
                if (offScreenBehavior == "LOCK" && dpm.isAdminActive(adminComponent)) {
                    try {
                        dpm.lockNow()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    context.getActivity()?.let { activity ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                            activity.setTurnScreenOn(false)
                            activity.setShowWhenLocked(false)
                        } else {
                            activity.window.clearFlags(
                                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            )
                        }
                        activity.window.clearFlags(
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        )
                        val lp = activity.window.attributes
                        lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                        activity.window.attributes = lp
                    }
                } else {
                    context.getActivity()?.moveTaskToBack(true)
                }
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeTrigger by remember { mutableStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumeTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isWaitingState = isConnected && receivedImage == null && qrCodeText == null && command != "CMD_APAGAR_TELA" && command != "CMD_EXIBIR_OBRIGADO"

    LaunchedEffect(isWaitingState, resumeTrigger) {
        if (isWaitingState) {
            kotlinx.coroutines.delay(10000L)
            val offScreenBehavior = prefs.getString("OFF_SCREEN_BEHAVIOR", "LOCK") ?: "LOCK"
            if (offScreenBehavior == "LOCK" && dpm.isAdminActive(adminComponent)) {
                try {
                    dpm.lockNow()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                context.getActivity()?.let { activity ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        activity.setTurnScreenOn(false)
                        activity.setShowWhenLocked(false)
                    } else {
                        activity.window.clearFlags(
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        )
                    }
                    activity.window.clearFlags(
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    )
                    val lp = activity.window.attributes
                    lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                    activity.window.attributes = lp
                }
            } else {
                context.getActivity()?.moveTaskToBack(true)
            }
        }
    }

    LaunchedEffect(command) {
        if (command == "CMD_APAGAR_TELA") {
            val offScreenBehavior = prefs.getString("OFF_SCREEN_BEHAVIOR", "LOCK") ?: "LOCK"
            if (offScreenBehavior == "LOCK" && dpm.isAdminActive(adminComponent)) {
                try {
                    dpm.lockNow()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                context.getActivity()?.let { activity ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        activity.setTurnScreenOn(false)
                        activity.setShowWhenLocked(false)
                    } else {
                        activity.window.clearFlags(
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        )
                    }
                    activity.window.clearFlags(
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    )
                    val lp = activity.window.attributes
                    lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                    activity.window.attributes = lp
                }
            } else {
                context.getActivity()?.moveTaskToBack(true)
            }
        } else if (command == "CMD_LIMPAR_TELA") {
            TcpClient.clearImage()
            context.getActivity()?.let { activity ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    activity.setTurnScreenOn(false)
                    activity.setShowWhenLocked(false)
                } else {
                    activity.window.clearFlags(
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    )
                }
                activity.window.clearFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                )
                val lp = activity.window.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                activity.window.attributes = lp
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.animation.AnimatedContent(
            targetState = when {
                command == "CMD_APAGAR_TELA" -> "BLACK"
                command == "CMD_EXIBIR_OBRIGADO" -> "OBRIGADO"
                qrCodeText != null -> "QR_CODE"
                receivedImage != null -> "IMAGE"
                isConnected -> "WAITING"
                else -> "CONNECT"
            },
            label = "PassengerStateTransition",
            transitionSpec = {
                androidx.compose.animation.fadeIn(animationSpec = tween(500)).togetherWith(androidx.compose.animation.fadeOut(animationSpec = tween(500)))
            }
        ) { state ->
            when (state) {
                "BLACK" -> {
                    Box(modifier = Modifier.fillMaxSize())
                }
                "OBRIGADO" -> {
                    if (displayMode == "PARTIAL") {
                        // Modo "Parte da Tela" configurado para o "Obrigado"
                        Box(modifier = Modifier.fillMaxSize()) {
                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val screenW = constraints.maxWidth.toFloat()
                                val screenH = constraints.maxHeight.toFloat()
                                val density = LocalDensity.current.density
                                
                                val relX = prefs.getFloat("PARTIAL_QR_X", 0.1f)
                                val relY = prefs.getFloat("PARTIAL_QR_Y", 0.1f)
                                val relW = prefs.getFloat("PARTIAL_QR_WIDTH", 0.35f)
                                val relH = prefs.getFloat("PARTIAL_QR_HEIGHT", 0.5f)
                                
                                val absX = relX * screenW
                                val absY = relY * screenH
                                val absW = relW * screenW
                                val absH = relH * screenH
                                
                                Column(
                                    modifier = Modifier
                                        .offset { IntOffset(absX.roundToInt(), absY.roundToInt()) }
                                        .size((absW / density).dp, (absH / density).dp)
                                        .background(Color(0xFF121212), RoundedCornerShape(16.dp))
                                        .border(1.dp, Color(0xFF23232C), RoundedCornerShape(16.dp))
                                        .padding(12.dp)
                                        .verticalScroll(rememberScrollState()),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Obrigado",
                                        tint = Color(0xFFF06292),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Obrigado!",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Avalie com 5 estrelas",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    androidx.compose.foundation.layout.Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        repeat(5) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Estrela",
                                                tint = Color(0xFFFFD700),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Modo Tela Cheia
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.layout.Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Obrigado",
                                    tint = Color(0xFFF06292),
                                    modifier = Modifier.size(160.dp)
                                )
                                Text(
                                    "Obrigado por viajar comigo!",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = Color.White
                                )
                                Text(
                                    "Por favor, avalie a corrida com 5 estrelas no aplicativo.",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.Gray
                                )
                                androidx.compose.foundation.layout.Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    repeat(5) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Estrela",
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(64.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                "QR_CODE" -> {
                    val currentText = qrCodeText
                    if (currentText != null) {
                        val qrBitmap = remember(currentText) {
                            try {
                                val writer = com.google.zxing.qrcode.QRCodeWriter()
                                val hints = mapOf(com.google.zxing.EncodeHintType.MARGIN to 0)
                                val bitMatrix = writer.encode(currentText, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512, hints)
                                val w = bitMatrix.width
                                val h = bitMatrix.height
                                val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
                                for (x in 0 until w) {
                                    for (y in 0 until h) {
                                        bmp.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                                    }
                                }
                                bmp
                            } catch (e: Exception) {
                                e.printStackTrace()
                                null
                            }
                        }
                        
                        if (displayMode == "PARTIAL") {
                            Box(modifier = Modifier.fillMaxSize()) {
                                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                    val screenW = constraints.maxWidth.toFloat()
                                    val screenH = constraints.maxHeight.toFloat()
                                    val density = LocalDensity.current.density
                                    
                                    val relX = prefs.getFloat("PARTIAL_QR_X", 0.1f)
                                    val relY = prefs.getFloat("PARTIAL_QR_Y", 0.1f)
                                    val relW = prefs.getFloat("PARTIAL_QR_WIDTH", 0.35f)
                                    val relH = prefs.getFloat("PARTIAL_QR_HEIGHT", 0.5f)
                                    
                                    val absX = relX * screenW
                                    val absY = relY * screenH
                                    val absW = relW * screenW
                                    val absH = relH * screenH
                                    
                                    Column(
                                        modifier = Modifier
                                            .offset { IntOffset(absX.roundToInt(), absY.roundToInt()) },
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size((absW / density).dp, (absH / density).dp)
                                                .background(Color.White)
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (qrBitmap != null) {
                                                Image(
                                                    bitmap = qrBitmap.asImageBitmap(),
                                                    contentDescription = "QR Code Pix",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Fit
                                                )
                                            } else {
                                                Text("Erro ao gerar QR Code", color = Color.Red)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        val descriptionText = when {
                                            command == "CMD_EXIBIR_WIFI" || command == "CMD_EXIBIR_BEM_VINDO" -> {
                                                "Wi-Fi: AL€X | Senha: qwertyuiop"
                                            }
                                            command == "CMD_EXIBIR_MEU_PIX" -> {
                                                "Pix: Alex Lopes da Silva | Chave: 87981504902"
                                            }
                                            command == "CMD_EXIBIR_PIX" -> {
                                                val pixData = parsePixPayload(currentText)
                                                if (pixData.amount.isNotEmpty()) {
                                                    "Pix: ${pixData.name} | R$ ${pixData.amount}"
                                                } else {
                                                    "Pix: ${pixData.name}"
                                                }
                                            }
                                            else -> {
                                                "Escaneie o QR Code"
                                            }
                                        }
                                        
                                        Text(
                                            text = descriptionText,
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = (screenW / density).dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            if (command == "CMD_EXIBIR_PIX" || command == "CMD_EXIBIR_MEU_PIX" || command == "CMD_EXIBIR_WIFI" || command == "CMD_EXIBIR_BEM_VINDO") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (useHorizontalLayout) {
                                        // Layout Horizontal: Tela Grande (Row)
                                        androidx.compose.foundation.layout.Row(
                                            modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            androidx.compose.material3.ElevatedCard(
                                                modifier = Modifier
                                                    .weight(1.2f)
                                                    .fillMaxHeight(),
                                                shape = RoundedCornerShape(24.dp),
                                                colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                                                    containerColor = Color.White
                                                )
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(16.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (qrBitmap != null) {
                                                        Image(
                                                            bitmap = qrBitmap.asImageBitmap(),
                                                            contentDescription = "QR Code Pix",
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = ContentScale.Fit
                                                        )
                                                    } else {
                                                        Text("Erro ao gerar QR Code", color = Color.Red)
                                                    }
                                                }
                                            }

                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .verticalScroll(rememberScrollState()),
                                                verticalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                PassengerInfoCard(command, currentText)
                                                
                                                Text(
                                                    if (command == "CMD_EXIBIR_WIFI" || command == "CMD_EXIBIR_BEM_VINDO") "Escaneie o QR Code ao lado para se conectar à rede Wi-Fi." else "Escaneie o QR Code ao lado para realizar o pagamento via Pix.",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color.Gray,
                                                    modifier = Modifier.padding(horizontal = 8.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        // Layout Vertical: Tela Pequena (Column com Scroll)
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState())
                                                .padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            androidx.compose.material3.ElevatedCard(
                                                modifier = Modifier
                                                    .size(280.dp)
                                                    .align(Alignment.CenterHorizontally),
                                                shape = RoundedCornerShape(24.dp),
                                                colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                                                    containerColor = Color.White
                                                )
                                            ) {
                                                Box(
                                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (qrBitmap != null) {
                                                        Image(
                                                            bitmap = qrBitmap.asImageBitmap(),
                                                            contentDescription = "QR Code Pix",
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = ContentScale.Fit
                                                        )
                                                    } else {
                                                        Text("Erro ao gerar QR Code", color = Color.Red)
                                                    }
                                                }
                                            }

                                            PassengerInfoCard(command, currentText)

                                            Text(
                                                if (command == "CMD_EXIBIR_WIFI" || command == "CMD_EXIBIR_BEM_VINDO") "Escaneie o QR Code acima para se conectar à rede Wi-Fi." else "Escaneie o QR Code acima para realizar o pagamento via Pix.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.Gray,
                                                modifier = Modifier.padding(horizontal = 8.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            } else {
                                // QR Code Genérico (e.g. captura de tela)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.foundation.layout.Column(
                                        modifier = Modifier.verticalScroll(rememberScrollState()),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        androidx.compose.material3.ElevatedCard(
                                            modifier = Modifier.size(280.dp),
                                            shape = RoundedCornerShape(24.dp),
                                            colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                                                containerColor = Color.White
                                            )
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (qrBitmap != null) {
                                                    Image(
                                                        bitmap = qrBitmap.asImageBitmap(),
                                                        contentDescription = "QR Code Genérico",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                } else {
                                                    Text("Erro ao gerar QR Code", color = Color.Red)
                                                }
                                            }
                                        }
                                        Text(
                                            "QR Code extraído da tela",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = Color.White
                                        )
                                        Text(
                                            "Escaneie o QR Code acima para visualizar ou pagar.",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.Gray,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                "IMAGE" -> {
                    val currentImage = receivedImage
                    if (currentImage != null) {
                        val bitmap = BitmapFactory.decodeByteArray(currentImage, 0, currentImage.size)
                        if (displayMode == "PARTIAL") {
                            Box(modifier = Modifier.fillMaxSize()) {
                                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                    val screenW = constraints.maxWidth.toFloat()
                                    val screenH = constraints.maxHeight.toFloat()
                                    val density = LocalDensity.current.density
                                    val relX = prefs.getFloat("PARTIAL_QR_X", 0.1f)
                                    val relY = prefs.getFloat("PARTIAL_QR_Y", 0.1f)
                                    val relW = prefs.getFloat("PARTIAL_QR_WIDTH", 0.35f)
                                    val relH = prefs.getFloat("PARTIAL_QR_HEIGHT", 0.5f)
                                    val absX = relX * screenW
                                    val absY = relY * screenH
                                    val absW = relW * screenW
                                    val absH = relH * screenH
                                    
                                    Column(
                                        modifier = Modifier
                                            .offset { IntOffset(absX.roundToInt(), absY.roundToInt()) },
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size((absW / density).dp, (absH / density).dp)
                                                .background(Color.White)
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "QR Code Pix",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "QR Code extraído da tela",
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = (screenW / density).dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "QR Code Pix",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
                "WAITING" -> {
                    val infiniteTransition = rememberInfiniteTransition()
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.05f,
                        targetValue = 0.25f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = alpha),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Aguardando imagem...",
                            color = Color.White.copy(alpha = alpha),
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
                "CONNECT" -> {
                    var offScreenBehavior by remember {
                        mutableStateOf(prefs.getString("OFF_SCREEN_BEHAVIOR", "LOCK") ?: "LOCK")
                    }

                    val settingsContent = @Composable {
                        Text(
                            "Modo Passageiro",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        OutlinedTextField(
                            value = serverIp,
                            onValueChange = { 
                                serverIp = it 
                                if (autoReconnect) {
                                    autoReconnect = false
                                    prefs.edit().putBoolean("AUTO_RECONNECT", false).apply()
                                }
                            },
                            label = { Text("IP do Motorista", color = Color.Gray) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.DarkGray,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = Color.Gray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().clickable {
                                autoReconnect = !autoReconnect
                                prefs.edit().putBoolean("AUTO_RECONNECT", autoReconnect).apply()
                            }.padding(vertical = 4.dp)
                        ) {
                            androidx.compose.material3.Switch(
                                checked = autoReconnect,
                                onCheckedChange = { 
                                    autoReconnect = it 
                                    prefs.edit().putBoolean("AUTO_RECONNECT", it).apply()
                                },
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color.DarkGray
                                )
                            )
                            Text(
                                text = "Auto-Conectar (buscar motorista)",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Text(
                            text = "Ao apagar a tela:",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.titleSmall
                        )
                        
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            androidx.compose.material3.Card(
                                onClick = {
                                    offScreenBehavior = "LOCK"
                                    prefs.edit().putString("OFF_SCREEN_BEHAVIOR", "LOCK").apply()
                                },
                                modifier = Modifier.weight(1f).height(68.dp),
                                colors = androidx.compose.material3.CardDefaults.cardColors(
                                    containerColor = if (offScreenBehavior == "LOCK") MaterialTheme.colorScheme.primaryContainer else Color(0xFF2C2C2C)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = if (offScreenBehavior == "LOCK") MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhonelinkErase,
                                        contentDescription = null,
                                        tint = if (offScreenBehavior == "LOCK") MaterialTheme.colorScheme.primary else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Bloquear Tela",
                                        color = if (offScreenBehavior == "LOCK") MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            
                            androidx.compose.material3.Card(
                                onClick = {
                                    offScreenBehavior = "MINIMIZE"
                                    prefs.edit().putString("OFF_SCREEN_BEHAVIOR", "MINIMIZE").apply()
                                },
                                modifier = Modifier.weight(1f).height(68.dp),
                                colors = androidx.compose.material3.CardDefaults.cardColors(
                                    containerColor = if (offScreenBehavior == "MINIMIZE") MaterialTheme.colorScheme.primaryContainer else Color(0xFF2C2C2C)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = if (offScreenBehavior == "MINIMIZE") MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = null,
                                        tint = if (offScreenBehavior == "MINIMIZE") MaterialTheme.colorScheme.primary else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Minimizar App",
                                        color = if (offScreenBehavior == "MINIMIZE") MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    val actionsContent = @Composable {
                        if (isDiscovering) {
                            val infiniteTransition = rememberInfiniteTransition()
                            val scale by infiniteTransition.animateFloat(
                                initialValue = 0.5f,
                                targetValue = 2.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1500, easing = androidx.compose.animation.core.LinearOutSlowInEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "radar_scale"
                            )
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1500, easing = androidx.compose.animation.core.LinearOutSlowInEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "radar_alpha"
                            )

                            Box(
                                modifier = Modifier.size(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                            this.alpha = alpha
                                        }
                                        .border(2.dp, MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Buscando motorista...", color = Color.LightGray)
                        } else {
                            Button(
                                onClick = {
                                    prefs.edit().putString("LAST_IP", serverIp).apply()
                                    autoReconnect = true
                                    connectionTrigger++
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                Text("Conectar Manual", style = MaterialTheme.typography.bodyMedium)
                            }
                            
                            androidx.compose.material3.OutlinedButton(
                                onClick = {
                                    prefs.edit().putString("LAST_IP", "").apply()
                                    autoReconnect = true
                                    connectionTrigger++
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                Text("Auto Conectar", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            }
                            
                            if (offScreenBehavior == "LOCK") {
                                if (!isAdminActive) {
                                    androidx.compose.material3.OutlinedButton(
                                        onClick = {
                                            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                                putExtra(
                                                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                                    "Necessário para apagar e bloquear a tela remotamente."
                                                )
                                            }
                                            adminLauncher.launch(intent)
                                        },
                                        modifier = Modifier.fillMaxWidth().height(50.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.secondary
                                        )
                                    ) {
                                        Text("Permitir Bloqueio (Admin)", style = MaterialTheme.typography.bodyMedium)
                                    }
                                } else {
                                    Text(
                                        "Admin Ativado",
                                        color = Color.Green,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    androidx.compose.material3.Surface(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(0.85f)
                            .wrapContentHeight(),
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF1E1E1E),
                        tonalElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (useHorizontalLayout) {
                                androidx.compose.foundation.layout.Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left Column - Inputs and Settings
                                    Column(
                                        modifier = Modifier.weight(1.1f),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        settingsContent()
                                    }
                                    
                                    // Right Column - Actions
                                    Column(
                                        modifier = Modifier.weight(0.9f),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        actionsContent()
                                    }
                                }
                            } else {
                                // Vertical layout: portrait or narrow horizontal
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    settingsContent()
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    actionsContent()
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (isConnected && receivedImage == null && qrCodeText == null) {
            androidx.compose.material3.IconButton(
                onClick = { 
                    autoReconnect = false // <-- Adicione isso! Impede o laço while de voltar
                    TcpClient.disconnect() 
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.WifiTetheringOff, contentDescription = "Desconectar", tint = Color.Gray)
            }
        }
    }
}

@Composable
fun PassengerInfoCard(command: String?, currentText: String) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (command == "CMD_EXIBIR_WIFI" || command == "CMD_EXIBIR_BEM_VINDO") {
                if (command == "CMD_EXIBIR_BEM_VINDO") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Bem-Vindo!",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color(0xFF4DD0E1)
                        )
                        Text(
                            "Fique à vontade e conecte-se à internet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                    androidx.compose.material3.HorizontalDivider(color = Color(0xFF2C2C2C))
                }
                // Rede Wi-Fi
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Rede Wi-Fi",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "AL€X",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }

                androidx.compose.material3.HorizontalDivider(color = Color(0xFF2C2C2C))

                // Senha
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Senha",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    SelectionContainer {
                        Text(
                            "qwertyuiop",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                }
            } else if (command == "CMD_EXIBIR_MEU_PIX") {
                // Pix Fixo (Meu Pix)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Nome",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Alex Lopes da Silva",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }

                androidx.compose.material3.HorizontalDivider(color = Color(0xFF2C2C2C))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Instituição",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Mercado Pago",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }

                androidx.compose.material3.HorizontalDivider(color = Color(0xFF2C2C2C))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Chave Pix",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    SelectionContainer {
                        Text(
                            "87981504902",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                }
            } else {
                // Pix Extraído Dinamicamente
                val pixData = parsePixPayload(currentText)
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Nome",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        pixData.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }

                androidx.compose.material3.HorizontalDivider(color = Color(0xFF2C2C2C))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (pixData.amount.isNotEmpty()) "Valor / Cidade" else "Cidade",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        if (pixData.amount.isNotEmpty()) "R$ ${pixData.amount} - ${pixData.city}" else pixData.city,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }

                androidx.compose.material3.HorizontalDivider(color = Color(0xFF2C2C2C))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Chave Pix",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    SelectionContainer {
                        Text(
                            pixData.key,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}


