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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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


import com.example.utils.*
import com.example.MyDeviceAdminReceiver
import com.example.OverlayService

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun SettingsScreen(onBack: () -> Unit, onNavigateToPartialSetup: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("PixPrefs", android.content.Context.MODE_PRIVATE) }
    
    var port by remember { mutableStateOf(prefs.getString("PORT", "8080") ?: "8080") }
    var hotspotSsid by remember { mutableStateOf(prefs.getString("HOTSPOT_SSID", "AL€X") ?: "AL€X") }
    var hotspotPassword by remember { mutableStateOf(prefs.getString("HOTSPOT_PASSWORD", "qwertyuiop") ?: "qwertyuiop") }
    var maxBrightness by remember { mutableStateOf(prefs.getBoolean("MAX_BRIGHTNESS", true)) }
    var offScreenBehavior by remember { mutableStateOf(prefs.getString("OFF_SCREEN_BEHAVIOR", "LOCK") ?: "LOCK") }
    var safetyTimeout by remember { mutableStateOf(prefs.getString("SAFETY_TIMEOUT", "2") ?: "2") }
    
    var targetPackage by remember { mutableStateOf(prefs.getString("TARGET_PACKAGE", "") ?: "") }
    var debugMonitorEnabled by remember { mutableStateOf(prefs.getBoolean("DEBUG_MONITOR_ENABLED", false)) }
    var autoScanInterval by remember { mutableStateOf(prefs.getString("AUTO_SCAN_INTERVAL", "10") ?: "10") }
    var qrScaleFactor by remember { mutableStateOf(prefs.getFloat("QR_SCALE_FACTOR", 0.5f)) }
    var qrEngine by remember { mutableStateOf(prefs.getString("QR_ENGINE", "MLKIT") ?: "MLKIT") }
    
    var passengerOrientation by remember { mutableStateOf(prefs.getString("PASSENGER_ORIENTATION", "LANDSCAPE") ?: "LANDSCAPE") }
    var displayMode by remember { mutableStateOf(prefs.getString("PASSENGER_DISPLAY_MODE", "FULLSCREEN") ?: "FULLSCREEN") }
    
    val dpm = remember { context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    val adminComponent = remember { ComponentName(context, MyDeviceAdminReceiver::class.java) }
    var isAdminActive by remember { mutableStateOf(dpm.isAdminActive(adminComponent)) }
    
    var hasOverlayPermission by remember { mutableStateOf(android.provider.Settings.canDrawOverlays(context)) }
    var hasAccessibilityPermission by remember { mutableStateOf(isAccessibilityServiceEnabled(context, OverlayService::class.java)) }
    var isBatteryUnrestricted by remember {
        val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        mutableStateOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) pm.isIgnoringBatteryOptimizations(context.packageName) else true)
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

    LaunchedEffect(resumeTrigger) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        isBatteryUnrestricted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) pm.isIgnoringBatteryOptimizations(context.packageName) else true
    }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
        hasOverlayPermission = android.provider.Settings.canDrawOverlays(context)
        hasAccessibilityPermission = isAccessibilityServiceEnabled(context, OverlayService::class.java)
        val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        isBatteryUnrestricted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) pm.isIgnoringBatteryOptimizations(context.packageName) else true
    }

    val adminLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
        isAdminActive = dpm.isAdminActive(adminComponent)
    }



    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            androidx.compose.material3.TopAppBar(
                title = { Text("Configurações", color = Color.White) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E1E)
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Network & Passenger
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Rede & Geral",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    OutlinedTextField(
                        value = port,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() } && newValue.length <= 5) {
                                port = newValue
                                prefs.edit().putString("PORT", newValue).apply()
                            }
                        },
                        label = { Text("Porta de Comunicação", color = Color.Gray) },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )

                    OutlinedTextField(
                        value = hotspotSsid,
                        onValueChange = { newValue ->
                            hotspotSsid = newValue
                            prefs.edit().putString("HOTSPOT_SSID", newValue).apply()
                        },
                        label = { Text("Nome do Wi-Fi (SSID)", color = Color.Gray) },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = hotspotPassword,
                        onValueChange = { newValue ->
                            hotspotPassword = newValue
                            prefs.edit().putString("HOTSPOT_PASSWORD", newValue).apply()
                        },
                        label = { Text("Senha do Wi-Fi", color = Color.Gray) },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Exibição da Tela (Passageiro)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    // Orientation
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Orientação da Tela", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val row1 = listOf("LANDSCAPE" to "Paisagem", "PORTRAIT" to "Retrato", "AUTO" to "Auto")
                            row1.forEach { (value, label) ->
                                androidx.compose.material3.Card(
                                    onClick = {
                                        passengerOrientation = value
                                        prefs.edit().putString("PASSENGER_ORIENTATION", value).apply()
                                    },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    colors = androidx.compose.material3.CardDefaults.cardColors(
                                        containerColor = if (passengerOrientation == value) MaterialTheme.colorScheme.primaryContainer else Color(0xFF2C2C2C)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = if (passengerOrientation == value) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(label, color = if (passengerOrientation == value) MaterialTheme.colorScheme.onPrimaryContainer else Color.White, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val row2 = listOf("REVERSE_LANDSCAPE" to "Paisagem Invertida", "REVERSE_PORTRAIT" to "Retrato Invertido")
                            row2.forEach { (value, label) ->
                                androidx.compose.material3.Card(
                                    onClick = {
                                        passengerOrientation = value
                                        prefs.edit().putString("PASSENGER_ORIENTATION", value).apply()
                                    },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    colors = androidx.compose.material3.CardDefaults.cardColors(
                                        containerColor = if (passengerOrientation == value) MaterialTheme.colorScheme.primaryContainer else Color(0xFF2C2C2C)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = if (passengerOrientation == value) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(label, color = if (passengerOrientation == value) MaterialTheme.colorScheme.onPrimaryContainer else Color.White, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Display Mode
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Abertura do QR Code", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val modes = listOf("FULLSCREEN" to "Tela Inteira", "PARTIAL" to "Parte da Tela")
                            modes.forEach { (value, label) ->
                                androidx.compose.material3.Card(
                                    onClick = {
                                        displayMode = value
                                        prefs.edit().putString("PASSENGER_DISPLAY_MODE", value).apply()
                                    },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    colors = androidx.compose.material3.CardDefaults.cardColors(
                                        containerColor = if (displayMode == value) MaterialTheme.colorScheme.primaryContainer else Color(0xFF2C2C2C)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = if (displayMode == value) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(label, color = if (displayMode == value) MaterialTheme.colorScheme.onPrimaryContainer else Color.White, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    if (displayMode == "PARTIAL") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onNavigateToPartialSetup,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Configurar Posição do QR Code")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Comportamento do Passageiro",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Max brightness toggle
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                maxBrightness = !maxBrightness
                                prefs.edit().putBoolean("MAX_BRIGHTNESS", maxBrightness).apply()
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Brilho Máximo Automático", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            Text("Força o brilho no máximo quando o Pix chega", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                        androidx.compose.material3.Switch(
                            checked = maxBrightness,
                            onCheckedChange = {
                                maxBrightness = it
                                prefs.edit().putBoolean("MAX_BRIGHTNESS", it).apply()
                            }
                        )
                    }

                    // Safety timeout selector
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tempo Limite de Exibição do Pix", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val timeouts = listOf("1" to "1 min", "2" to "2 min", "5" to "5 min", "0" to "Nunca")
                            timeouts.forEach { (value, label) ->
                                androidx.compose.material3.Card(
                                    onClick = {
                                        safetyTimeout = value
                                        prefs.edit().putString("SAFETY_TIMEOUT", value).apply()
                                    },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    colors = androidx.compose.material3.CardDefaults.cardColors(
                                        containerColor = if (safetyTimeout == value) MaterialTheme.colorScheme.primaryContainer else Color(0xFF2C2C2C)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = if (safetyTimeout == value) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            label,
                                            color = if (safetyTimeout == value) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Captura Automática (Auto-Scan)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = targetPackage,
                        onValueChange = {
                            targetPackage = it
                            prefs.edit().putString("TARGET_PACKAGE", it).apply()
                        },
                        label = { Text("Aplicativo Alvo (Package Name)", color = Color.Gray) },
                        placeholder = { Text("Deixe vazio para funcionar em todos", color = Color.Gray) },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // NOVO CÓDIGO: Sugestões rápidas de Pacotes
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Sugestões rápidas:", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val packageSuggestions = listOf(
                                "99" to "com.app99.drive",
                                "Uber" to "com.ubercab.driver",
                                "Todos" to "" // Vazio significa funcionar em todos
                            )
                            
                            packageSuggestions.forEach { (label, pkg) ->
                                androidx.compose.material3.Card(
                                    onClick = {
                                        targetPackage = pkg
                                        prefs.edit().putString("TARGET_PACKAGE", pkg).apply()
                                    },
                                    modifier = Modifier.height(32.dp),
                                    colors = androidx.compose.material3.CardDefaults.cardColors(
                                        containerColor = if (targetPackage == pkg) MaterialTheme.colorScheme.primaryContainer else Color(0xFF2C2C2C)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = if (targetPackage == pkg) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxHeight().padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (targetPackage == pkg) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Intervalo do Auto-Scan", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val intervals = listOf("1" to "1 min", "5" to "5 min", "10" to "10 min", "30" to "30 min")
                            intervals.forEach { (value, label) ->
                                androidx.compose.material3.Card(
                                    onClick = {
                                        autoScanInterval = value
                                        prefs.edit().putString("AUTO_SCAN_INTERVAL", value).apply()
                                    },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    colors = androidx.compose.material3.CardDefaults.cardColors(
                                        containerColor = if (autoScanInterval == value) MaterialTheme.colorScheme.primaryContainer else Color(0xFF2C2C2C)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = if (autoScanInterval == value) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            label,
                                            color = if (autoScanInterval == value) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Escala de Redução (Fallback)", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        Text("Usada caso a leitura em 100% falhe. Menor = Mais rápido.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val scales = listOf(0.25f to "25%", 0.50f to "50%", 0.75f to "75%")
                            scales.forEach { (value, label) ->
                                androidx.compose.material3.Card(
                                    onClick = {
                                        qrScaleFactor = value
                                        prefs.edit().putFloat("QR_SCALE_FACTOR", value).apply()
                                    },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    colors = androidx.compose.material3.CardDefaults.cardColors(
                                        containerColor = if (qrScaleFactor == value) MaterialTheme.colorScheme.primaryContainer else Color(0xFF2C2C2C)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = if (qrScaleFactor == value) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            label,
                                            color = if (qrScaleFactor == value) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Mecanismo de Leitura", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        Text("Google ML Kit é mais rápido. ZXing é a alternativa (fallback).", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val engines = listOf("MLKIT" to "Google ML Kit", "ZXING" to "ZXing")
                            engines.forEach { (value, label) ->
                                androidx.compose.material3.Card(
                                    onClick = {
                                        qrEngine = value
                                        prefs.edit().putString("QR_ENGINE", value).apply()
                                    },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    colors = androidx.compose.material3.CardDefaults.cardColors(
                                        containerColor = if (qrEngine == value) MaterialTheme.colorScheme.primaryContainer else Color(0xFF2C2C2C)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = if (qrEngine == value) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            label,
                                            color = if (qrEngine == value) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    androidx.compose.material3.Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = Color(0xFF2C2C2C)
                        )
                    ) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Monitor de Pacotes (Debug)", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                Text("Exibe a etiqueta verde no Overlay", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }
                            androidx.compose.material3.Switch(
                                checked = debugMonitorEnabled,
                                onCheckedChange = {
                                    debugMonitorEnabled = it
                                    prefs.edit().putBoolean("DEBUG_MONITOR_ENABLED", it).apply()
                                }
                            )
                        }
                    }
                }

                // Right Column: Screen Behavior & Permissions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Ação ao Apagar a Tela",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
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
                            modifier = Modifier.weight(1f).height(64.dp),
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
                            modifier = Modifier.weight(1f).height(64.dp),
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

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Permissões do Sistema",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Device Admin Status / Settings (Only visible if LOCK behavior is selected)
                    if (offScreenBehavior == "LOCK") {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Administrador do Dispositivo", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = if (isAdminActive) "Ativado (Necessário para bloqueio)" else "Desativado",
                                    color = if (isAdminActive) Color.Green else Color.LightGray,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (!isAdminActive) {
                                Button(
                                    onClick = {
                                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                            putExtra(
                                                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                                "Necessário para apagar e bloquear a tela remotamente."
                                            )
                                        }
                                        adminLauncher.launch(intent)
                                    }
                                ) {
                                    Text("Ativar")
                                }
                            }
                        }
                    }

                    // Overlay permission Status / Settings
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Desenhar Sobre Outros Apps", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = if (hasOverlayPermission) "Concedido (Necessário para bolha)" else "Não concedido",
                                color = if (hasOverlayPermission) Color.Green else Color.LightGray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (!hasOverlayPermission) {
                            Button(
                                onClick = {
                                    val intent = Intent(
                                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        android.net.Uri.parse("package:${context.packageName}")
                                    )
                                    launcher.launch(intent)
                                }
                            ) {
                                Text("Conceder")
                            }
                        }
                    }

                    // Accessibility permission Status / Settings
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Serviço de Acessibilidade", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = if (hasAccessibilityPermission) "Ativado (Captura de tela instantânea)" else "Desativado",
                                color = if (hasAccessibilityPermission) Color.Green else Color.LightGray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (!hasAccessibilityPermission) {
                            Button(
                                onClick = {
                                    val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    launcher.launch(intent)
                                }
                            ) {
                                Text("Habilitar")
                            }
                        }
                    }

                    // Battery Optimization Status / Settings
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Otimização de Bateria", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = if (isBatteryUnrestricted) "Irrestrito (Recomendado)" else "Otimizado (Pode congelar o app)",
                                color = if (isBatteryUnrestricted) Color.Green else Color.LightGray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (!isBatteryUnrestricted) {
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                        launcher.launch(intent)
                                    }
                                }
                            ) {
                                Text("Ajustar")
                            }
                        }
                    }
                }
            }
        }
    }
}


