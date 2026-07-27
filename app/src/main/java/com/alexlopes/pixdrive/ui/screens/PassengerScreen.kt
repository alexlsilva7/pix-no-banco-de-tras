package com.alexlopes.pixdrive.ui.screens

import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import com.alexlopes.pixdrive.network.TcpServer
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
import androidx.compose.foundation.layout.aspectRatio
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
import com.alexlopes.pixdrive.network.TcpClient
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.material.icons.filled.Bluetooth
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
import com.alexlopes.pixdrive.ui.theme.MyApplicationTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt


import android.annotation.SuppressLint
import com.alexlopes.pixdrive.utils.*
import com.alexlopes.pixdrive.MyDeviceAdminReceiver
import com.alexlopes.pixdrive.PixActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.os.BatteryManager
import android.content.IntentFilter
import com.alexlopes.pixdrive.network.PassengerService

private fun startPassengerConnection(
    context: Context,
    ip: String,
    port: Int,
    autoReconnect: Boolean
) {
    val intent = Intent(context, PassengerService::class.java).apply {
        action = PassengerService.ACTION_START
        putExtra(PassengerService.EXTRA_IP, ip)
        putExtra(PassengerService.EXTRA_PORT, port)
        putExtra(PassengerService.EXTRA_AUTO_RECONNECT, autoReconnect)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

private fun stopPassengerConnection(context: Context) {
    com.alexlopes.pixdrive.network.UdpDiscovery.stopClientDiscovery()
    context.startService(
        Intent(context, PassengerService::class.java).apply {
            action = PassengerService.ACTION_STOP
        }
    )
}

@Composable
private fun IpKeypadDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "⌫")
    )

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(
            modifier = Modifier
                .widthIn(max = 270.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E1E1E),
            tonalElevation = 10.dp
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "IP do motorista",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
                androidx.compose.material3.Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF292929),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = value.ifEmpty { "0.0.0.0" },
                            color = if (value.isEmpty()) Color.Gray else Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }

                keys.forEach { rowKeys ->
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowKeys.forEach { key ->
                            androidx.compose.material3.OutlinedButton(
                                onClick = {
                                    value = when (key) {
                                        "⌫" -> value.dropLast(1)
                                        "." -> if (
                                            value.isNotEmpty() &&
                                            !value.endsWith(".") &&
                                            value.count { it == '.' } < 3 &&
                                            value.length < 15
                                        ) "$value." else value
                                        else -> if (value.length < 15) value + key else value
                                    }
                                },
                                modifier = Modifier.weight(1f).height(34.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text(key, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }

                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.material3.TextButton(
                        onClick = { value = "" },
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Text("Limpar")
                    }
                    androidx.compose.material3.TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = { onConfirm(value.trim('.')) },
                        enabled = value.isNotBlank(),
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

@Composable
fun PassengerScreen(
    onSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("PixPrefs", android.content.Context.MODE_PRIVATE) }
    var serverIp by remember { mutableStateOf(prefs.getString("LAST_IP", "192.168.") ?: "192.168.") }
    val isDiscovering by com.alexlopes.pixdrive.network.UdpDiscovery.isClientListeningState.collectAsState()
    val scope = rememberCoroutineScope()
    
    var autoReconnect by remember { mutableStateOf(prefs.getBoolean("AUTO_RECONNECT", false)) }
    
    val receivedImage by TcpClient.receivedImage.collectAsState()
    val qrCodeText by TcpClient.qrCodeText.collectAsState()
    val command by TcpClient.command.collectAsState()
    val isConnected by TcpClient.isConnected.collectAsState()
    
    val dpm = remember { context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    val adminComponent = remember { ComponentName(context, MyDeviceAdminReceiver::class.java) }
    
    val passengerOrientation = remember { prefs.getString("PASSENGER_ORIENTATION", "LANDSCAPE") ?: "LANDSCAPE" }
    val displayMode = remember { prefs.getString("PASSENGER_DISPLAY_MODE", "FULLSCREEN") ?: "FULLSCREEN" }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidthDp = configuration.screenWidthDp
    val useHorizontalLayout = isLandscape && screenWidthDp >= 640

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

    // Gerenciador do ciclo de vida em background do Serviço persistente
    LaunchedEffect(autoReconnect) {
        val currentPort = prefs.getString("PORT", "8080")?.toIntOrNull() ?: 8080
        val lastIp = prefs.getString("LAST_IP", "") ?: ""
        
        if (autoReconnect) {
            startPassengerConnection(context, lastIp, currentPort, autoReconnect = true)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Se o usuário fechar a tela, encerra o serviço se não estiver de fato conectado
            if (!TcpClient.isConnected.value) {
                val intent = Intent(context, PassengerService::class.java).apply {
                    action = PassengerService.ACTION_STOP
                }
                context.startService(intent)
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

    val qrCodeScale = remember(resumeTrigger) { prefs.getFloat("QR_CODE_SIZE_SCALE", 1.0f) }
    val partialQrColorStyle = remember(resumeTrigger) {
        prefs.getString("PARTIAL_QR_COLOR_STYLE", "WHITE_BG_BLACK_QR") ?: "WHITE_BG_BLACK_QR"
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
        AnimatedContent(
            targetState = when {
                command == "CMD_APAGAR_TELA" -> "BLACK"
                command == "CMD_EXIBIR_OBRIGADO" -> "OBRIGADO"
                qrCodeText != null -> "QR_CODE_${qrCodeText.hashCode()}"
                receivedImage != null -> "IMAGE_${receivedImage?.contentHashCode() ?: 0}"
                isConnected -> "WAITING"
                else -> "CONNECT"
            },
            label = "PassengerStateTransition",
            transitionSpec = {
                // Aplica a animação de Escala + Fade quando entra um QR Code, Imagem ou Obrigado
                if (targetState.startsWith("QR_CODE") || targetState == "OBRIGADO" || targetState.startsWith("IMAGE")) {
                    (fadeIn(animationSpec = tween(450, easing = FastOutSlowInEasing)) + scaleIn(
                        initialScale = 0.75f, // Começa em 75% do tamanho e cresce suavemente
                        animationSpec = tween(450, easing = FastOutSlowInEasing)
                    )).togetherWith(
                        fadeOut(animationSpec = tween(300)) + scaleOut(
                            targetScale = 0.9f,
                            animationSpec = tween(300)
                        )
                    )
                } else {
                    // Transição padrão de Fade In/Out para telas de conexão/espera
                    fadeIn(animationSpec = tween(400)).togetherWith(
                        fadeOut(animationSpec = tween(400))
                    )
                }
            }
        ) { stateKey ->
            val state = when {
                stateKey == "BLACK" -> "BLACK"
                stateKey == "OBRIGADO" -> "OBRIGADO"
                stateKey.startsWith("QR_CODE") -> "QR_CODE"
                stateKey.startsWith("IMAGE") -> "IMAGE"
                stateKey == "WAITING" -> "WAITING"
                else -> "CONNECT"
            }
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
                        val qrBitmap = remember(currentText, displayMode, partialQrColorStyle) {
                            try {
                                val writer = com.google.zxing.qrcode.QRCodeWriter()
                                val hints = mapOf(com.google.zxing.EncodeHintType.MARGIN to 0)
                                val bitMatrix = writer.encode(currentText, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512, hints)
                                val w = bitMatrix.width
                                val h = bitMatrix.height
                                val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)

                                val isInverted = (displayMode == "PARTIAL" && partialQrColorStyle == "BLACK_BG_WHITE_QR")
                                val qrColor = if (isInverted) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                                val bgColor = if (isInverted) android.graphics.Color.BLACK else android.graphics.Color.WHITE

                                for (x in 0 until w) {
                                    for (y in 0 until h) {
                                        bmp.setPixel(x, y, if (bitMatrix.get(x, y)) qrColor else bgColor)
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
                                    
                                    val boxBgColor = if (partialQrColorStyle == "BLACK_BG_WHITE_QR") Color.Black else Color.White
                                    val containerBorderColor = if (partialQrColorStyle == "BLACK_BG_WHITE_QR") Color.Transparent else MaterialTheme.colorScheme.primary

                                    Column(
                                        modifier = Modifier
                                            .offset { IntOffset(absX.roundToInt(), absY.roundToInt()) }
                                            .border(2.dp, containerBorderColor, RoundedCornerShape(12.dp))
                                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                                            .padding(4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size((absW / density).dp, (absH / density).dp)
                                                .background(boxBgColor, RoundedCornerShape(8.dp)),
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
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
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
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = (absW / density).dp)
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
                                                    .fillMaxHeight(0.95f)
                                                    .aspectRatio(1f), // QUADRADO PERFEITO
                                                shape = RoundedCornerShape(20.dp),
                                                colors = androidx.compose.material3.CardDefaults.elevatedCardColors(containerColor = Color.White)
                                            ) {
                                                Box(
                                                    modifier = Modifier.fillMaxSize().padding(0.dp), // ZERADO
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (qrBitmap != null) {
                                                        Image(
                                                            bitmap = qrBitmap.asImageBitmap(),
                                                            contentDescription = "QR Code Pix",
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = ContentScale.Fit
                                                        )
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
                                                    .size((280 * qrCodeScale).dp)
                                                    .aspectRatio(1f)
                                                    .align(Alignment.CenterHorizontally),
                                                shape = RoundedCornerShape(20.dp),
                                                colors = androidx.compose.material3.CardDefaults.elevatedCardColors(containerColor = Color.White)
                                            ) {
                                                Box(
                                                    modifier = Modifier.fillMaxSize().padding(0.dp), // ZERADO
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (qrBitmap != null) {
                                                        Image(
                                                            bitmap = qrBitmap.asImageBitmap(),
                                                            contentDescription = "QR Code Pix",
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = ContentScale.Fit
                                                        )
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
                                            modifier = Modifier
                                                .size((320 * qrCodeScale).dp)
                                                .aspectRatio(1f), // QUADRADO PERFEITO
                                            shape = RoundedCornerShape(24.dp),
                                            colors = androidx.compose.material3.CardDefaults.elevatedCardColors(containerColor = Color.White)
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize().padding(0.dp), // ZERADO
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (qrBitmap != null) {
                                                    Image(
                                                        bitmap = qrBitmap.asImageBitmap(),
                                                        contentDescription = "QR Code Genérico",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Fit
                                                    )
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
                                    
                                    val boxBgColor = if (partialQrColorStyle == "BLACK_BG_WHITE_QR") Color.Black else Color.White
                                    val containerBorderColor = if (partialQrColorStyle == "BLACK_BG_WHITE_QR") Color.Transparent else MaterialTheme.colorScheme.primary

                                    Column(
                                        modifier = Modifier
                                            .offset { IntOffset(absX.roundToInt(), absY.roundToInt()) }
                                            .border(2.dp, containerBorderColor, RoundedCornerShape(12.dp))
                                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                                            .padding(4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size((absW / density).dp, (absH / density).dp)
                                                .background(boxBgColor, RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "QR Code Pix",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "QR Code extraído da tela",
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = (absW / density).dp)
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
                        Spacer(modifier = Modifier.height(12.dp))
                        androidx.compose.foundation.layout.Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.IconButton(onClick = onSettings) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Configurações",
                                    tint = Color.Gray
                                )
                            }
                            androidx.compose.material3.IconButton(
                                onClick = {
                                    autoReconnect = false
                                    prefs.edit().putBoolean("AUTO_RECONNECT", false).apply()
                                    stopPassengerConnection(context)
                                }
                            ) {
                                Icon(
                                    Icons.Default.WifiTetheringOff,
                                    contentDescription = "Desconectar",
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                }
                "CONNECT" -> {
                    // Estados de suporte ao Bluetooth
                    var isBluetoothConnecting by remember { mutableStateOf(false) }
                    var bluetoothError by remember { mutableStateOf<String?>(null) }
                    var hasBluetoothPermission by remember { mutableStateOf(com.alexlopes.pixdrive.utils.hasBluetoothPermissions(context)) }
                    var showDevicePickerDialog by remember { mutableStateOf(false) }
                    var showIpKeypad by remember { mutableStateOf(false) }
                    var pairedDevices by remember { mutableStateOf<List<android.bluetooth.BluetoothDevice>>(emptyList()) }

                    val bluetoothPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
                    ) { results ->
                        hasBluetoothPermission = results.values.all { it }
                        if (hasBluetoothPermission) {
                            pairedDevices = com.alexlopes.pixdrive.network.BluetoothClientHelper.getPairedDevices(context)
                            showDevicePickerDialog = true
                        }
                    }

                    if (showDevicePickerDialog) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showDevicePickerDialog = false },
                            title = { Text("Selecionar Motorista", color = Color.White) },
                            text = {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (pairedDevices.isEmpty()) {
                                        Text("Nenhum dispositivo pareado encontrado. Pareie com o celular do motorista nas configurações de Bluetooth do Android.", color = Color.Gray)
                                    } else {
                                        Text("Selecione o celular do motorista:", color = Color.LightGray)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            pairedDevices.forEach { device ->
                                                @SuppressLint("MissingPermission")
                                                val deviceName = device.name ?: device.address
                                                androidx.compose.material3.Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            showDevicePickerDialog = false
                                                            isBluetoothConnecting = true
                                                            bluetoothError = null
                                                            scope.launch {
                                                                com.alexlopes.pixdrive.network.BluetoothClientHelper.connectToDriver(
                                                                    context = context,
                                                                    device = device,
                                                                    onConfigReceived = { ssid, pass, ip ->
                                                                        bluetoothError = "Conectando ao Wi-Fi: $ssid..."
                                                                        com.alexlopes.pixdrive.utils.WifiConnector.connectToWifi(
                                                                            context = context,
                                                                            ssid = ssid,
                                                                            pass = pass,
                                                                            onConnected = {
                                                                                isBluetoothConnecting = false
                                                                                bluetoothError = null
                                                                                serverIp = ip
                                                                                prefs.edit().putString("LAST_IP", ip).apply()
                                                                                autoReconnect = false
                                                                                prefs.edit()
                                                                                    .putBoolean("AUTO_RECONNECT", false)
                                                                                    .apply()
                                                                                val currentPort = prefs
                                                                                    .getString("PORT", "8080")
                                                                                    ?.toIntOrNull() ?: 8080
                                                                                startPassengerConnection(
                                                                                    context,
                                                                                    ip.trim(),
                                                                                    currentPort,
                                                                                    autoReconnect = false
                                                                                )
                                                                            },
                                                                            onError = { err ->
                                                                                isBluetoothConnecting = false
                                                                                bluetoothError = "Falha ao conectar Wi-Fi: $err"
                                                                            }
                                                                        )
                                                                    },
                                                                    onError = { err ->
                                                                        isBluetoothConnecting = false
                                                                        bluetoothError = "Erro Bluetooth: $err"
                                                                    }
                                                                )
                                                            }
                                                        },
                                                    colors = androidx.compose.material3.CardDefaults.cardColors(
                                                        containerColor = Color(0xFF2C2C2C)
                                                    )
                                                ) {
                                                    Text(
                                                        text = deviceName,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(16.dp),
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                androidx.compose.material3.TextButton(onClick = { showDevicePickerDialog = false }) {
                                    Text("Cancelar", color = MaterialTheme.colorScheme.primary)
                                }
                            },
                            containerColor = Color(0xFF1E1E1E)
                        )
                    }

                    if (showIpKeypad) {
                        IpKeypadDialog(
                            initialValue = serverIp,
                            onDismiss = { showIpKeypad = false },
                            onConfirm = { newIp ->
                                serverIp = newIp
                                showIpKeypad = false
                                if (autoReconnect) {
                                    autoReconnect = false
                                    prefs.edit().putBoolean("AUTO_RECONNECT", false).apply()
                                    stopPassengerConnection(context)
                                }
                            }
                        )
                    }

                    val settingsContent = @Composable {
                        Text(
                            "Conectar ao motorista",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            if (autoReconnect) {
                                "Procurando o celular do motorista…"
                            } else {
                                "Conexão automática desativada"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = serverIp,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("IP do Motorista", color = Color.Gray) },
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = Color.Gray
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showIpKeypad = true }
                            )
                        }
                    }

                    val actionsContent = @Composable {
                        if (isDiscovering) {
                            androidx.compose.foundation.layout.Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    "Buscando motorista...",
                                    color = Color.LightGray,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val manualIp = serverIp.trim()
                                    prefs.edit().putString("LAST_IP", manualIp).apply()
                                    autoReconnect = false
                                    prefs.edit().putBoolean("AUTO_RECONNECT", false).apply()
                                    com.alexlopes.pixdrive.network.UdpDiscovery.stopClientDiscovery()
                                    val currentPort = prefs
                                        .getString("PORT", "8080")
                                        ?.toIntOrNull() ?: 8080
                                    startPassengerConnection(
                                        context,
                                        manualIp,
                                        currentPort,
                                        autoReconnect = false
                                    )
                                },
                                enabled = serverIp.trim().let { it.isNotEmpty() && it != "192.168." },
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Text("Conectar", style = MaterialTheme.typography.bodyMedium)
                            }
                            
                            androidx.compose.material3.OutlinedButton(
                                onClick = {
                                    if (autoReconnect) {
                                        autoReconnect = false
                                        prefs.edit()
                                            .putBoolean("AUTO_RECONNECT", false)
                                            .apply()
                                        stopPassengerConnection(context)
                                    } else {
                                        prefs.edit()
                                            .putBoolean("AUTO_RECONNECT", true)
                                            .apply()
                                        autoReconnect = true
                                    }
                                },
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Text(
                                    if (autoReconnect) "Desativar Auto" else "Auto Conectar",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                            }
                        }

                        Button(
                                onClick = {
                                    if (hasBluetoothPermission) {
                                        pairedDevices = com.alexlopes.pixdrive.network.BluetoothClientHelper.getPairedDevices(context)
                                        showDevicePickerDialog = true
                                    } else {
                                        bluetoothPermissionLauncher.launch(com.alexlopes.pixdrive.utils.bluetoothPermissionsList())
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Conectar via Bluetooth", style = MaterialTheme.typography.bodyMedium)
                            }

                        if (isBluetoothConnecting || bluetoothError != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                androidx.compose.material3.Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = androidx.compose.material3.CardDefaults.cardColors(
                                        containerColor = Color(0xFF2C2C2C)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (isBluetoothConnecting) {
                                            androidx.compose.material3.CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                        Text(
                                            text = bluetoothError ?: "Conectando ao motorista via Bluetooth...",
                                            color = Color.LightGray,
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                        }
                    }

                    androidx.compose.material3.Surface(
                        modifier = Modifier
                            .padding(16.dp)
                            .widthIn(max = 400.dp)
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF1E1E1E),
                        tonalElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 18.dp, vertical = 16.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            settingsContent()
                            actionsContent()
                            androidx.compose.material3.IconButton(onClick = onSettings) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Configurações",
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                }
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


