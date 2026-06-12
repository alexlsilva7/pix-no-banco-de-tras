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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.MyApplicationTheme

import com.example.utils.*
import com.example.OverlayService
import com.example.ImageRepository

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun DriverScreen() {
    val context = LocalContext.current
    var hasOverlayPermission by remember {
        mutableStateOf(android.provider.Settings.canDrawOverlays(context))
    }
    var hasAccessibilityPermission by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context, OverlayService::class.java))
    }
    val ipAddress = remember { getLocalIpAddress() }
    
    val lastImageBytes by ImageRepository.lastCapturedImage.collectAsState()
    val scrollState = rememberScrollState()
    val connectedClients by TcpServer.connectedClients.collectAsState()
    val isServerRunning by TcpServer.isServerRunningState.collectAsState()
    val isBtServerRunning by com.example.network.BluetoothServerHelper.isBluetoothServerRunning.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val prefs = remember { context.getSharedPreferences("PixPrefs", android.content.Context.MODE_PRIVATE) }
    val hotspotSsid = remember { prefs.getString("HOTSPOT_SSID", "AL€X") ?: "AL€X" }
    val hotspotPassword = remember { prefs.getString("HOTSPOT_PASSWORD", "qwertyuiop") ?: "qwertyuiop" }
    val port = remember { prefs.getString("PORT", "8080")?.toIntOrNull() ?: 8080 }

    var hasBluetoothPermission by remember { mutableStateOf(com.example.utils.hasBluetoothPermissions(context)) }
    val bluetoothPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasBluetoothPermission = results.values.all { it }
    }
    
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
        hasOverlayPermission = android.provider.Settings.canDrawOverlays(context)
        hasAccessibilityPermission = isAccessibilityServiceEnabled(context, OverlayService::class.java)
    }

    LaunchedEffect(Unit) {
        hasAccessibilityPermission = isAccessibilityServiceEnabled(context, OverlayService::class.java)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        androidx.compose.material3.CenterAlignedTopAppBar(
            title = { Text("Painel do Motorista") },
            navigationIcon = {
                androidx.compose.material3.IconButton(onClick = { /* ... */ }) {
                    Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = null)
                }
            },
            colors = androidx.compose.material3.TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent
            )
        )
        
        // STATUS 1: Servidor de Transmissão (TCP)
        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = if (isServerRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (isServerRunning) androidx.compose.material.icons.Icons.Default.WifiTethering else androidx.compose.material.icons.Icons.Default.WifiTetheringOff,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = if (isServerRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isServerRunning) "Transmissão Ativa" else "Transmissão Parada",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isServerRunning) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isServerRunning) {
                    Text("${connectedClients.size} dispositivo(s) conectado(s)", style = MaterialTheme.typography.bodyMedium)
                    if (connectedClients.isNotEmpty()) {
                        Text("IPs: ${connectedClients.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (isServerRunning) {
                            TcpServer.stopServer()
                        } else {
                            coroutineScope.launch {
                                TcpServer.startServer(context, port)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isServerRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (isServerRunning) "Desligar Servidor" else "Iniciar Servidor")
                }
            }
        }

        // STATUS 2: Servidor Bluetooth Independente
        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = if (isBtServerRunning) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Bluetooth,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = if (isBtServerRunning) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isBtServerRunning) "Pareamento BT Ativo" else "Pareamento BT Inativo",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isBtServerRunning) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Auxilia os passageiros a se conectarem no seu Wi-Fi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (isBtServerRunning) {
                            com.example.network.BluetoothServerHelper.stopBluetoothServer()
                        } else {
                            if (com.example.utils.hasBluetoothPermissions(context)) {
                                coroutineScope.launch {
                                    com.example.network.BluetoothServerHelper.startBluetoothServer(
                                        context = context,
                                        ssid = hotspotSsid,
                                        pass = hotspotPassword,
                                        ip = ipAddress
                                    )
                                }
                            } else {
                                bluetoothPermissionLauncher.launch(com.example.utils.bluetoothPermissionsList())
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBtServerRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text(if (isBtServerRunning) "Desativar Bluetooth" else "Ativar Bluetooth")
                }
            }
        }
        
        androidx.compose.material3.Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = androidx.compose.foundation.shape.CircleShape,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            SelectionContainer {
                Text(
                    text = "IP: $ipAddress",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (!hasOverlayPermission) {
            androidx.compose.material3.OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Permissão Inicial Necessária", style = MaterialTheme.typography.titleMedium)
                    Text("Para criar a bolha flutuante, o aplicativo precisa da permissão de 'Sobreposição'.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:${context.packageName}")
                        )
                        launcher.launch(intent)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Conceder Permissão")
                    }
                }
            }
        } else if (!hasAccessibilityPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            androidx.compose.material3.OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Configuração Final", style = MaterialTheme.typography.titleMedium)
                    Text("Para capturar a tela anonimamente (sem pedir no Android), ative o serviço de Acessibilidade do 'Pix no Banco de Trás'.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        launcher.launch(intent)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Habilitar Acessibilidade")
                    }
                }
            }
        } else if (!hasBluetoothPermission) {
            androidx.compose.material3.OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Permissão de Bluetooth", style = MaterialTheme.typography.titleMedium)
                    Text("Necessária para transmitir os ajustes de Wi-Fi aos passageiros de forma transparente.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        bluetoothPermissionLauncher.launch(com.example.utils.bluetoothPermissionsList())
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Conceder Permissão")
                    }
                }
            }
        } else {
            // Ações do Overlay
            Text("Bolha Flutuante", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, bottom = 8.dp))
            androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = {
                        val intent = Intent(context, OverlayService::class.java).apply { putExtra("action", "SHOW_BUBBLE") }
                        context.startService(intent)
                    },
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Text("Ativar Overlay")
                }
                Button(
                    onClick = {
                        val intent = Intent(context, OverlayService::class.java).apply { putExtra("action", "HIDE_BUBBLE") }
                        context.startService(intent)
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Desativar Overlay")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Ações do Motorista
            Text("Controles Remotos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, bottom = 8.dp))
            androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                androidx.compose.material3.OutlinedCard(
                    onClick = { coroutineScope.launch { TcpServer.sendCommand("CMD_LIMPAR_TELA") } },
                    modifier = Modifier.weight(1f).height(100.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(androidx.compose.material.icons.Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Limpar Imagem", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                androidx.compose.material3.OutlinedCard(
                    onClick = { coroutineScope.launch { TcpServer.sendCommand("CMD_APAGAR_TELA") } },
                    modifier = Modifier.weight(1f).height(100.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(androidx.compose.material.icons.Icons.Default.PhonelinkErase, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Apagar Tela", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        val currentLastImageBytes = lastImageBytes
        if (currentLastImageBytes != null) {
            Spacer(modifier = Modifier.height(32.dp))
            Text("Última Imagem Enviada", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, bottom = 8.dp))
            androidx.compose.material3.ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val bitmap = BitmapFactory.decodeByteArray(currentLastImageBytes, 0, currentLastImageBytes.size)
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Última Captura",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
