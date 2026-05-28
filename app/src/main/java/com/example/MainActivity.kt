package com.example

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

// Navigation Routes (trigger rebuild 3)
const val ModeSelectionRoute = "mode_selection"
const val DriverRoute = "driver"
const val PassengerRoute = "passenger"
const val SettingsRoute = "settings"
const val MyPixQrCodeRoute = "my_pix_qr_code"
const val MyWifiQrCodeRoute = "my_wifi_qr_code"

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Register callback to wake up screen when Pix is received
    com.example.network.TcpClient.onExibirPixCallback = { cmd ->
      try {
        val powerManager = getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        @Suppress("DEPRECATION")
        val wakeLock = powerManager.newWakeLock(
            android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or 
            android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or 
            android.os.PowerManager.ON_AFTER_RELEASE,
            "PixNoBancoDeTras::WakeLock"
        )
        wakeLock.acquire(5000)
        
        if (cmd == "CMD_EXIBIR_PIX" || cmd == "CMD_EXIBIR_MEU_PIX" || cmd == "CMD_EXIBIR_WIFI" || cmd == "CMD_EXIBIR_BEM_VINDO" || cmd == "CMD_EXIBIR_OBRIGADO") {
            val intent = android.content.Intent(this, PixActivity::class.java).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            
            val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val channelId = "pix_alerts"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    "Alertas de Pix",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }

            val pendingIntent = android.app.PendingIntent.getActivity(
                this,
                0,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val notification = androidx.core.app.NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Novo Pix Recebido")
                .setContentText("Você tem um novo QR Code de Pix na tela.")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(1001, notification)
        }
      } catch (e: Exception) {
          e.printStackTrace()
      }
    }

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val navController = rememberNavController()
        
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          NavHost(
            navController = navController,
            startDestination = ModeSelectionRoute,
            modifier = Modifier.padding(innerPadding)
          ) {
            composable(ModeSelectionRoute) {
              ModeSelectionScreen(
                onDriverSelected = { navController.navigate(DriverRoute) },
                onPassengerSelected = { navController.navigate(PassengerRoute) },
                onSettingsSelected = { navController.navigate(SettingsRoute) },
                onMyPixQrCodeSelected = { navController.navigate(MyPixQrCodeRoute) },
                onMyWifiQrCodeSelected = { navController.navigate(MyWifiQrCodeRoute) }
              )
            }
            composable(DriverRoute) {
              DriverScreen()
            }
            composable(PassengerRoute) {
              PassengerScreen()
            }
            composable(SettingsRoute) {
              SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(MyPixQrCodeRoute) {
              MyPixQrCodeScreen(onBack = { navController.popBackStack() })
            }
            composable(MyWifiQrCodeRoute) {
              MyWifiQrCodeScreen(onBack = { navController.popBackStack() })
            }
          }
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    if (com.example.network.TcpClient.onExibirPixCallback != null) {
      com.example.network.TcpClient.onExibirPixCallback = null
    }
  }
}

@Composable
fun ModeSelectionScreen(
  onDriverSelected: () -> Unit,
  onPassengerSelected: () -> Unit,
  onSettingsSelected: () -> Unit,
  onMyPixQrCodeSelected: () -> Unit = {},
  onMyWifiQrCodeSelected: () -> Unit = {}
) {
  Box(modifier = Modifier.fillMaxSize()) {
    androidx.compose.material3.IconButton(
      onClick = onSettingsSelected,
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(16.dp)
        .size(48.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Settings,
        contentDescription = "Configurações",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(28.dp)
      )
    }

    Column(
      modifier = Modifier.fillMaxSize().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = Icons.Default.QrCode,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(72.dp)
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = "Pix no Banco de Trás",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = "Escolha seu modo de operação",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(48.dp))
      
      androidx.compose.material3.ElevatedCard(
        onClick = onDriverSelected,
        modifier = Modifier.fillMaxWidth().height(120.dp).testTag("driver_button"),
        shape = RoundedCornerShape(24.dp),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
      ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
          androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.DirectionsCar,
              contentDescription = "Motorista",
              modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(24.dp))
            Column {
               Text("Motorista", style = MaterialTheme.typography.titleLarge)
               Text("Compartilhe a tela (Servidor)", style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      }
      
      Spacer(modifier = Modifier.height(24.dp))
      
      androidx.compose.material3.ElevatedCard(
        onClick = onMyPixQrCodeSelected,
        modifier = Modifier.fillMaxWidth().height(120.dp).testTag("my_pix_qr_button"),
        shape = RoundedCornerShape(24.dp),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
          containerColor = MaterialTheme.colorScheme.tertiaryContainer,
          contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
      ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
          androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.QrCode,
              contentDescription = "Meu QR Code",
              modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(24.dp))
            Column {
               Text("Meu QR Code Pix", style = MaterialTheme.typography.titleLarge)
               Text("Exibir meu código de pagamento", style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      }
      
      Spacer(modifier = Modifier.height(24.dp))
      
      androidx.compose.material3.ElevatedCard(
        onClick = onMyWifiQrCodeSelected,
        modifier = Modifier.fillMaxWidth().height(120.dp).testTag("my_wifi_qr_button"),
        shape = RoundedCornerShape(24.dp),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
          containerColor = Color(0xFF4A148C),
          contentColor = Color.White
        )
      ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
          androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = androidx.compose.material.icons.Icons.Default.Wifi,
              contentDescription = "Meu Wi-Fi",
              modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(24.dp))
            Column {
               Text("Wi-Fi do Carro", style = MaterialTheme.typography.titleLarge)
               Text("Compartilhar internet com passageiros", style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
      
      androidx.compose.material3.ElevatedCard(
        onClick = onPassengerSelected,
        modifier = Modifier.fillMaxWidth().height(120.dp).testTag("passenger_button"),
        shape = RoundedCornerShape(24.dp),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
      ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
          androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Person,
              contentDescription = "Passageiro",
              modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(24.dp))
            Column {
               Text("Passageiro", style = MaterialTheme.typography.titleLarge)
               Text("Visualizar imagem do Pix (Cliente)", style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      }
    }
  }
}

fun isAccessibilityServiceEnabled(context: Context, accessibilityService: Class<*>): Boolean {
    val expectedComponentName = android.content.ComponentName(context, accessibilityService)
    val enabledServicesSetting = android.provider.Settings.Secure.getString(
        context.contentResolver,
        android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServicesSetting)
    while (colonSplitter.hasNext()) {
        val componentNameString = colonSplitter.next()
        val enabledService = android.content.ComponentName.unflattenFromString(componentNameString)
        if (enabledService != null && enabledService == expectedComponentName) {
            return true
        }
    }
    return false
}

fun getLocalIpAddress(): String {
    try {
        val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            val addresses = networkInterface.inetAddresses
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                if (!address.isLoopbackAddress && address.hostAddress?.indexOf(':') == -1) {
                    return address.hostAddress ?: "0.0.0.0"
                }
            }
        }
    } catch (ex: java.net.SocketException) {
        ex.printStackTrace()
    }
    return "Desconhecido"
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
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
    val coroutineScope = rememberCoroutineScope()
    
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
                androidx.compose.material3.IconButton(onClick = { /* Handle back if needed or rely on system back */ }) {
                    Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = null)
                }
            },
            colors = androidx.compose.material3.TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent
            )
        )
        
        // Status do Servidor
        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
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
                                TcpServer.startServer()
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

tailrec fun Context.getActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}

@Composable
fun PassengerScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("PixPrefs", android.content.Context.MODE_PRIVATE) }
    var serverIp by remember { mutableStateOf(prefs.getString("LAST_IP", "192.168.") ?: "192.168.") }
    var isDiscovering by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    val receivedImage by TcpClient.receivedImage.collectAsState()
    val qrCodeText by TcpClient.qrCodeText.collectAsState()
    val command by TcpClient.command.collectAsState()
    val isConnected by TcpClient.isConnected.collectAsState()
    
    val dpm = remember { context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    val adminComponent = remember { ComponentName(context, MyDeviceAdminReceiver::class.java) }
    var isAdminActive by remember { mutableStateOf(dpm.isAdminActive(adminComponent)) }

    val adminLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
        isAdminActive = dpm.isAdminActive(adminComponent)
    }

    DisposableEffect(Unit) {
        val activity = context.getActivity()
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
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

    LaunchedEffect(Unit) {
        if (!isConnected) {
            val lastIp = prefs.getString("LAST_IP", "")
            val currentPort = prefs.getString("PORT", "8080")?.toIntOrNull() ?: 8080
            if (!lastIp.isNullOrEmpty() && lastIp != "192.168.") {
                TcpClient.connect(lastIp, currentPort)
            }
            if (!TcpClient.isConnected.value) {
                isDiscovering = true
                val discoveredIp = com.example.network.UdpDiscovery.discoverServerIp()
                isDiscovering = false
                if (discoveredIp != null) {
                    serverIp = discoveredIp
                    prefs.edit().putString("LAST_IP", discoveredIp).apply()
                    TcpClient.connect(discoveredIp, currentPort)
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
                // Remove flags de tela caso o motorista esqueça
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
    }

    LaunchedEffect(command) {
        if (command == "CMD_APAGAR_TELA") {
            val offScreenBehavior = prefs.getString("OFF_SCREEN_BEHAVIOR", "LOCK") ?: "LOCK"
            if (offScreenBehavior == "LOCK") {
                if (dpm.isAdminActive(adminComponent)) {
                    try {
                        dpm.lockNow()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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
                "QR_CODE" -> {
                    val currentText = qrCodeText
                    if (currentText != null) {
                        val qrBitmap = remember(currentText) {
                            try {
                                val writer = com.google.zxing.qrcode.QRCodeWriter()
                                val bitMatrix = writer.encode(currentText, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512)
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
                        
                        if (command == "CMD_EXIBIR_OBRIGADO") {
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
                        } else if (command == "CMD_EXIBIR_PIX" || command == "CMD_EXIBIR_MEU_PIX" || command == "CMD_EXIBIR_WIFI" || command == "CMD_EXIBIR_BEM_VINDO") {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.layout.Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // QR Code
                                    androidx.compose.material3.ElevatedCard(
                                        modifier = Modifier
                                            .weight(1f)
                                            .wrapContentHeight(),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                                            containerColor = Color.White
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (qrBitmap != null) {
                                                Image(
                                                    bitmap = qrBitmap.asImageBitmap(),
                                                    contentDescription = "QR Code Pix",
                                                    modifier = Modifier.size(280.dp),
                                                    contentScale = ContentScale.Fit
                                                )
                                            } else {
                                                Text("Erro ao gerar QR Code", color = Color.Red)
                                            }
                                        }
                                    }

                                    // Info
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
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
                                                                "Bem-Vindo ao Carro!",
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
                                                } else {
                                                    // Pix
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
                                                }
                                            }
                                        }

                                        // Instruction text
                                        Text(
                                            if (command == "CMD_EXIBIR_WIFI" || command == "CMD_EXIBIR_BEM_VINDO") "Escaneie o QR Code ao lado para se conectar à rede Wi-Fi." else "Escaneie o QR Code ao lado para realizar o pagamento via Pix.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Generic QR Code (e.g., from screenshot)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.layout.Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    androidx.compose.material3.ElevatedCard(
                                        shape = RoundedCornerShape(24.dp),
                                        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                                            containerColor = Color.White
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (qrBitmap != null) {
                                                Image(
                                                    bitmap = qrBitmap.asImageBitmap(),
                                                    contentDescription = "QR Code Genérico",
                                                    modifier = Modifier.size(360.dp),
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
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
                "IMAGE" -> {
                    val currentImage = receivedImage
                    if (currentImage != null) {
                        val bitmap = BitmapFactory.decodeByteArray(currentImage, 0, currentImage.size)
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR Code Pix",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                "WAITING" -> {
                    val infiniteTransition = rememberInfiniteTransition()
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1.0f,
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
                                    Text(
                                        "Modo Passageiro",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = Color.White
                                    )
                                    OutlinedTextField(
                                        value = serverIp,
                                        onValueChange = { serverIp = it },
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
                                
                                // Right Column - Actions
                                Column(
                                    modifier = Modifier.weight(0.9f),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
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
                                                scope.launch {
                                                    val currentPort = prefs.getString("PORT", "8080")?.toIntOrNull() ?: 8080
                                                    TcpClient.connect(serverIp, currentPort)
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().height(50.dp)
                                        ) {
                                            Text("Conectar Manual", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        
                                        androidx.compose.material3.OutlinedButton(
                                            onClick = {
                                                scope.launch {
                                                    isDiscovering = true
                                                    val discoveredIp = com.example.network.UdpDiscovery.discoverServerIp()
                                                    isDiscovering = false
                                                    if (discoveredIp != null) {
                                                        serverIp = discoveredIp
                                                        prefs.edit().putString("LAST_IP", discoveredIp).apply()
                                                        val currentPort = prefs.getString("PORT", "8080")?.toIntOrNull() ?: 8080
                                                        TcpClient.connect(discoveredIp, currentPort)
                                                    }
                                                }
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
                            }
                        }
                    }
                }
            }
        }
        
        if (isConnected && receivedImage == null && qrCodeText == null) {
            androidx.compose.material3.IconButton(
                onClick = { TcpClient.disconnect() },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.WifiTetheringOff, contentDescription = "Desconectar", tint = Color.Gray)
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MyPixQrCodeScreen(onBack: () -> Unit) {
    val pixPayload = "00020101021126360014br.gov.bcb.pix0114+55879815049025204000053039865802BR5919Alex Lopes da Silva6011GaranhunsPE62070503***6304539E"

    val qrBitmap = remember(pixPayload) {
        try {
            val writer = com.google.zxing.qrcode.QRCodeWriter()
            val bitMatrix = writer.encode(pixPayload, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            androidx.compose.material3.TopAppBar(
                title = { Text("Meu QR Code Pix", color = Color.White) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E1E)
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // QR Code
                    androidx.compose.material3.ElevatedCard(
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentHeight(),
                        shape = RoundedCornerShape(24.dp),
                        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "QR Code Pix",
                                    modifier = Modifier.size(280.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Text("Erro ao gerar QR Code", color = Color.Red)
                            }
                        }
                    }

                    // Info
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
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
                                // Name
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

                                // Institution
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

                                // Chave PIX
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "Chave Pix",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    SelectionContainer {
                                        Text(
                                            "(87) 98150-4902",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Instruction text
                        Text(
                            "Escaneie o QR Code ao lado para realizar o pagamento via Pix.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MyWifiQrCodeScreen(onBack: () -> Unit) {
    val wifiPayload = "WIFI:S:AL€X;T:WPA;P:qwertyuiop;H:false;;"
    val qrBitmap = remember {
        try {
            val writer = com.google.zxing.qrcode.QRCodeWriter()
            val bitMatrix = writer.encode(wifiPayload, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Back button
        androidx.compose.material3.IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Voltar",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .padding(top = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // QR Code
                androidx.compose.material3.ElevatedCard(
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(24.dp),
                    colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                        containerColor = Color.White
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "QR Code Wi-Fi",
                                modifier = Modifier.size(280.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text("Erro ao gerar QR Code", color = Color.Red)
                        }
                    }
                }

                // Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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
                        }
                    }

                    // Instruction text
                    Text(
                        "Escaneie o QR Code ao lado para se conectar à rede Wi-Fi do carro.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}
