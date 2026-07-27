package com.alexlopes.pixdrive.ui.screens

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.alexlopes.pixdrive.DeviceMode
import com.alexlopes.pixdrive.MyDeviceAdminReceiver
import com.alexlopes.pixdrive.OverlayService
import com.alexlopes.pixdrive.network.PassengerService
import com.alexlopes.pixdrive.network.TcpClient
import com.alexlopes.pixdrive.network.TcpServer
import com.alexlopes.pixdrive.utils.bluetoothPermissionsList
import com.alexlopes.pixdrive.utils.hasBluetoothPermissions
import com.alexlopes.pixdrive.utils.isAccessibilityServiceEnabled
import kotlinx.coroutines.launch

@Composable
fun ModeConfirmationScreen(
    mode: DeviceMode,
    isModeChange: Boolean,
    onContinue: () -> Unit,
    onCancel: () -> Unit = {}
) {
    val isDriver = mode == DeviceMode.DRIVER
    val modeName = if (isDriver) "motorista" else "Visor"
    val title = when {
        isModeChange -> "Alterar para modo $modeName?"
        isDriver -> "Modo motorista ativado"
        else -> "Modo Visor ativado"
    }
    val description = when {
        isModeChange && isDriver ->
            "As configurações atuais serão mantidas, mas este aparelho passará a controlar o visor traseiro."
        isModeChange ->
            "As configurações atuais serão mantidas, mas este aparelho passará a receber conteúdos."
        isDriver ->
            "Este aparelho será usado para controlar o visor traseiro."
        else ->
            "Este aparelho será usado como visor no banco traseiro."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isDriver) Icons.Default.DirectionsCar else Icons.Default.QrCode2,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(76.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Continuar configuração")
        }
        if (isModeChange) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar")
            }
        }
    }
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun ModePermissionsScreen(
    mode: DeviceMode,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshKey by remember { mutableIntStateOf(0) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }

    val refreshLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshKey++
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshKey++
    }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        refreshKey++
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val hasBluetooth = remember(refreshKey) { hasBluetoothPermissions(context) }
    val hasNotifications = remember(refreshKey) {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
    val hasOverlay = remember(refreshKey) { Settings.canDrawOverlays(context) }
    val hasAccessibility = remember(refreshKey) {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
            isAccessibilityServiceEnabled(context, OverlayService::class.java)
    }
    val dpm = remember {
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }
    val adminComponent = remember {
        ComponentName(context, MyDeviceAdminReceiver::class.java)
    }
    val hasDeviceAdmin = remember(refreshKey) { dpm.isAdminActive(adminComponent) }
    val ready = if (mode == DeviceMode.DRIVER) {
        hasOverlay && hasAccessibility && hasBluetooth && hasNotifications
    } else {
        hasBluetooth && hasNotifications && hasDeviceAdmin
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (mode == DeviceMode.DRIVER) {
                            "Configurar motorista"
                        } else {
                            "Configurar visor"
                        }
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Permissões necessárias",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (mode == DeviceMode.DRIVER) {
                    "Ative somente os recursos usados para capturar cobranças e controlar o visor."
                } else {
                    "Ative somente os recursos usados para conectar, manter o visor disponível e bloquear a tela."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (mode == DeviceMode.DRIVER) {
                PermissionCard(
                    title = "Sobreposição",
                    description = "Exibe a bolha de captura sobre o aplicativo de Pix.",
                    icon = Icons.Default.Visibility,
                    granted = hasOverlay,
                    onRequest = {
                        refreshLauncher.launch(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                )
                PermissionCard(
                    title = "Acessibilidade",
                    description = "Permite capturar o QR Code quando você solicitar pela bolha.",
                    icon = Icons.Default.ScreenLockPortrait,
                    granted = hasAccessibility,
                    onRequest = { showAccessibilityDialog = true }
                )
            } else {
                PermissionCard(
                    title = "Bloqueio de tela",
                    description = "Permite apagar e reativar o visor traseiro com segurança.",
                    icon = Icons.Default.Lock,
                    granted = hasDeviceAdmin,
                    onRequest = {
                        refreshLauncher.launch(
                            Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                putExtra(
                                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                    "Permite que o PixDrive apague a tela quando o visor estiver ocioso."
                                )
                            }
                        )
                    }
                )
            }

            PermissionCard(
                title = "Bluetooth",
                description = "Ajuda os dois aparelhos a trocar os dados de conexão.",
                icon = Icons.Default.Bluetooth,
                granted = hasBluetooth,
                onRequest = { permissionLauncher.launch(bluetoothPermissionsList()) }
            )
            PermissionCard(
                title = "Notificações",
                description = "Mantém a conexão ativa em segundo plano.",
                icon = Icons.Default.Notifications,
                granted = hasNotifications,
                onRequest = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onContinue,
                enabled = ready,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(if (ready) "Testar conexão" else "Conclua as permissões")
            }
        }
    }

    if (showAccessibilityDialog) {
        AccessibilityDisclosureDialog(
            onConfirm = {
                showAccessibilityDialog = false
                refreshLauncher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
            onDismiss = { showAccessibilityDialog = false }
        )
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    icon: ImageVector,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (granted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else icon,
                contentDescription = null,
                tint = if (granted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(30.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!granted) {
                OutlinedButton(onClick = onRequest) {
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(" Ativar")
                }
            }
        }
    }
}

@Composable
fun ConnectionTestScreen(
    mode: DeviceMode,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val driverClients by TcpServer.connectedClients.collectAsState()
    val passengerConnected by TcpClient.isConnected.collectAsState()
    var passengerTestStarted by remember { mutableStateOf(false) }
    val connected =
        if (mode == DeviceMode.DRIVER) driverClients.isNotEmpty() else passengerConnected
    val testRunning = mode == DeviceMode.DRIVER || passengerTestStarted
    val finishConnectionTest = {
        if (mode == DeviceMode.PASSENGER_DISPLAY &&
            passengerTestStarted &&
            !connected
        ) {
            context.startService(
                Intent(context, PassengerService::class.java).apply {
                    action = PassengerService.ACTION_STOP
                }
            )
        }
        onFinish()
    }

    LaunchedEffect(mode, passengerTestStarted) {
        val prefs = context.getSharedPreferences("PixPrefs", Context.MODE_PRIVATE)
        val port = prefs.getString("PORT", "8080")?.toIntOrNull() ?: 8080
        if (mode == DeviceMode.DRIVER) {
            if (!TcpServer.isRunning) {
                scope.launch { TcpServer.startServer(context, port) }
            }
        } else if (passengerTestStarted) {
            val intent = Intent(context, PassengerService::class.java).apply {
                action = PassengerService.ACTION_START
                putExtra(PassengerService.EXTRA_IP, prefs.getString("LAST_IP", "") ?: "")
                putExtra(PassengerService.EXTRA_PORT, port)
                putExtra(PassengerService.EXTRA_AUTO_RECONNECT, true)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!testRunning) {
            Icon(
                Icons.Default.WifiFind,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(78.dp)
            )
        } else if (connected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(78.dp)
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.size(64.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = when {
                connected -> "Conexão concluída"
                !testRunning -> "Teste de conexão"
                else -> "Testando conexão"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = when {
                connected -> "Os dois aparelhos estão prontos para uso."
                !testRunning ->
                    "O visor só procurará o celular do motorista quando você iniciar o teste."
                mode == DeviceMode.DRIVER ->
                    "Mantenha o visor do passageiro ligado e conectado à mesma rede."
                else -> "Procurando o celular do motorista…"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        if (!testRunning) {
            Button(
                onClick = { passengerTestStarted = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Iniciar teste")
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = finishConnectionTest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Configurar depois")
            }
        } else {
            Button(
                onClick = finishConnectionTest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(if (connected) "Ir para a tela principal" else "Configurar depois")
            }
        }
        if (!connected && testRunning) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.WifiFind,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "O teste continuará disponível na tela principal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
