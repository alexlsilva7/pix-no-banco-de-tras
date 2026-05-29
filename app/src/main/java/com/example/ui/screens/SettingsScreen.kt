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
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("PixPrefs", android.content.Context.MODE_PRIVATE) }
    
    var port by remember { mutableStateOf(prefs.getString("PORT", "8080") ?: "8080") }
    var maxBrightness by remember { mutableStateOf(prefs.getBoolean("MAX_BRIGHTNESS", true)) }
    var offScreenBehavior by remember { mutableStateOf(prefs.getString("OFF_SCREEN_BEHAVIOR", "LOCK") ?: "LOCK") }
    var safetyTimeout by remember { mutableStateOf(prefs.getString("SAFETY_TIMEOUT", "2") ?: "2") }
    
    val dpm = remember { context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    val adminComponent = remember { ComponentName(context, MyDeviceAdminReceiver::class.java) }
    var isAdminActive by remember { mutableStateOf(dpm.isAdminActive(adminComponent)) }
    
    var hasOverlayPermission by remember { mutableStateOf(android.provider.Settings.canDrawOverlays(context)) }
    var hasAccessibilityPermission by remember { mutableStateOf(isAccessibilityServiceEnabled(context, OverlayService::class.java)) }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
        hasOverlayPermission = android.provider.Settings.canDrawOverlays(context)
        hasAccessibilityPermission = isAccessibilityServiceEnabled(context, OverlayService::class.java)
    }

    val adminLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
        isAdminActive = dpm.isAdminActive(adminComponent)
    }

    // Force landscape mode for settings screen (it fits perfectly with the tablet flow)
    DisposableEffect(Unit) {
        val activity = context.getActivity()
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        onDispose {
            if (originalOrientation != null) {
                activity?.requestedOrientation = originalOrientation
            }
        }
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

            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Network & Passenger
                Column(
                    modifier = Modifier
                        .weight(1.1f)
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
                }

                // Right Column: Screen Behavior & Permissions
                Column(
                    modifier = Modifier
                        .weight(0.9f)
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
                }
            }
        }
    }
}


