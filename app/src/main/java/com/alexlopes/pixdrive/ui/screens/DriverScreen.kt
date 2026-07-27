package com.alexlopes.pixdrive.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PhonelinkErase
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alexlopes.pixdrive.ImageRepository
import com.alexlopes.pixdrive.OverlayService
import com.alexlopes.pixdrive.network.TcpServer
import com.alexlopes.pixdrive.utils.isAccessibilityServiceEnabled
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class ServerActionState {
    Idle,
    Starting,
    Error,
}

private enum class QuickAction {
    CapturePix,
    ClearPassengerScreen,
}

internal fun connectedDevicesLabel(count: Int): String = when (count) {
    0 -> "Nenhum dispositivo conectado"
    1 -> "1 dispositivo conectado"
    else -> "$count dispositivos conectados"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverScreen(
    onSettings: () -> Unit = {},
    onMyPix: () -> Unit = {},
    onMyWifi: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val connectedClients by TcpServer.connectedClients.collectAsState()
    val isServerRunning by TcpServer.isServerRunningState.collectAsState()
    val serverAddress by TcpServer.serverAddress.collectAsState()
    val lastImageBytes by ImageRepository.lastCapturedImage.collectAsState()
    val prefs = remember {
        context.getSharedPreferences("PixPrefs", Context.MODE_PRIVATE)
    }
    val port = remember {
        prefs.getString("PORT", "8080")?.toIntOrNull() ?: 8080
    }

    var serverActionState by remember { mutableStateOf(ServerActionState.Idle) }
    var activeQuickAction by remember { mutableStateOf<QuickAction?>(null) }
    var showStopConfirmation by remember { mutableStateOf(false) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var captureTimeoutJob by remember { mutableStateOf<Job?>(null) }
    var capturedImageAtRequest by remember { mutableStateOf<Int?>(null) }
    var hasOverlayPermission by remember {
        mutableStateOf(android.provider.Settings.canDrawOverlays(context))
    }
    var hasAccessibilityPermission by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context, OverlayService::class.java))
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        hasOverlayPermission = android.provider.Settings.canDrawOverlays(context)
        hasAccessibilityPermission =
            isAccessibilityServiceEnabled(context, OverlayService::class.java)
    }

    LaunchedEffect(isServerRunning) {
        if (isServerRunning) {
            serverActionState = ServerActionState.Idle
        }
    }

    LaunchedEffect(lastImageBytes) {
        val requestHash = capturedImageAtRequest ?: return@LaunchedEffect
        val newImageHash = lastImageBytes?.contentHashCode() ?: return@LaunchedEffect
        if (activeQuickAction == QuickAction.CapturePix && newImageHash != requestHash) {
            captureTimeoutJob?.cancel()
            activeQuickAction = null
            capturedImageAtRequest = null
            snackbarHostState.showSnackbar("Pix capturado e enviado.")
        }
    }

    fun startServer() {
        if (serverActionState == ServerActionState.Starting) return
        serverActionState = ServerActionState.Starting
        scope.launch {
            TcpServer.startServer(context, port)
            if (!TcpServer.isRunning) {
                serverActionState = ServerActionState.Error
            }
        }
    }

    fun copyIp() {
        val ip = serverAddress ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Endereço IP do servidor", ip))
        scope.launch { snackbarHostState.showSnackbar("IP copiado") }
    }

    fun showBriefConfirmation(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun capturePix() {
        if (activeQuickAction != null) return
        if (!hasAccessibilityPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            showAccessibilityDialog = true
            return
        }
        if (!hasOverlayPermission) {
            val intent = Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${context.packageName}"),
            )
            settingsLauncher.launch(intent)
            return
        }

        activeQuickAction = QuickAction.CapturePix
        capturedImageAtRequest = lastImageBytes?.contentHashCode() ?: 0
        context.startService(
            Intent(context, OverlayService::class.java).putExtra("action", "CAPTURE_NOW"),
        )
        captureTimeoutJob?.cancel()
        captureTimeoutJob = scope.launch {
            delay(10_000)
            if (activeQuickAction == QuickAction.CapturePix) {
                activeQuickAction = null
                capturedImageAtRequest = null
                snackbarHostState.showSnackbar(
                    message = "Não foi possível enviar. Verifique a conexão.",
                    actionLabel = "Tentar novamente",
                )
            }
        }
    }

    if (showAccessibilityDialog) {
        AccessibilityDisclosureDialog(
            onConfirm = {
                showAccessibilityDialog = false
                settingsLauncher.launch(
                    Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS),
                )
            },
            onDismiss = { showAccessibilityDialog = false },
        )
    }

    if (showStopConfirmation) {
        AlertDialog(
            onDismissRequest = { showStopConfirmation = false },
            title = { Text("Encerrar servidor?") },
            text = { Text("O dispositivo do passageiro será desconectado.") },
            dismissButton = {
                TextButton(onClick = { showStopConfirmation = false }) {
                    Text("Cancelar")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStopConfirmation = false
                        serverActionState = ServerActionState.Idle
                        TcpServer.stopServer()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Encerrar", fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Painel do Motorista",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "Modo motorista",
                        modifier = Modifier
                            .padding(start = 20.dp, end = 12.dp)
                            .size(28.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                actions = {
                    IconButton(
                        onClick = onSettings,
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurações",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets.safeDrawing,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            ServerStatusCard(
                isRunning = isServerRunning,
                actionState = serverActionState,
                connectedDevices = connectedClients.size,
                serverAddress = serverAddress,
                onCopyIp = ::copyIp,
                onStartServer = ::startServer,
            )

            ConnectedDeviceSection(connectedClients = connectedClients)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Ações rápidas")
                QuickActionsGrid(
                    actionsEnabled = isServerRunning && connectedClients.isNotEmpty(),
                    activeAction = activeQuickAction,
                    onShowPix = {
                        showBriefConfirmation("QR Pix exibido no dispositivo.")
                        onMyPix()
                    },
                    onShareWifi = {
                        showBriefConfirmation("Wi-Fi compartilhado.")
                        onMyWifi()
                    },
                    onCapturePix = ::capturePix,
                    onClearScreen = {
                        if (activeQuickAction != null) return@QuickActionsGrid
                        activeQuickAction = QuickAction.ClearPassengerScreen
                        scope.launch {
                            TcpServer.sendCommand("CMD_APAGAR_TELA")
                            activeQuickAction = null
                            snackbarHostState.showSnackbar(
                                "Tela do passageiro apagada.",
                            )
                        }
                    },
                )
                if (!isServerRunning || connectedClients.isEmpty()) {
                    Text(
                        text = "As ações estarão disponíveis quando um dispositivo estiver conectado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (isServerRunning) {
                OutlinedButton(
                    onClick = { showStopConfirmation = true },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.error.copy(alpha = 0.55f),
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Encerrar servidor", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ServerStatusCard(
    isRunning: Boolean,
    actionState: ServerActionState,
    connectedDevices: Int,
    serverAddress: String?,
    onCopyIp: () -> Unit,
    onStartServer: () -> Unit,
) {
    val isStarting = actionState == ServerActionState.Starting
    val hasError = actionState == ServerActionState.Error
    val statusText = when {
        isRunning -> "Servidor ativo"
        isStarting -> "Iniciando servidor..."
        hasError -> "Não foi possível iniciar o servidor"
        else -> "Servidor desligado"
    }
    val statusColor = when {
        isRunning -> Color(0xFF2E7D32)
        hasError -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when {
                    isStarting -> CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                    )
                    hasError -> Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Erro no servidor",
                        tint = statusColor,
                        modifier = Modifier.size(22.dp),
                    )
                    else -> Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(statusColor, CircleShape)
                            .semantics {
                                contentDescription =
                                    if (isRunning) "Servidor ativo" else "Servidor desligado"
                            },
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasError) statusColor else MaterialTheme.colorScheme.onSurface,
                    )
                    if (isRunning) {
                        Text(
                            text = connectedDevicesLabel(connectedDevices),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (isRunning) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Endereço IP",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = onCopyIp,
                            enabled = serverAddress != null,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Text(
                                text = serverAddress ?: "Obtendo endereço...",
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        IconButton(
                            onClick = onCopyIp,
                            enabled = serverAddress != null,
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar endereço IP",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            } else {
                Button(
                    onClick = onStartServer,
                    enabled = !isStarting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    if (isStarting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                    } else if (hasError) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = if (hasError) "Tentar novamente" else "Iniciar servidor",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectedDeviceSection(connectedClients: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Dispositivo conectado")
        if (connectedClients.isEmpty()) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Nenhum dispositivo conectado",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Verifique se o dispositivo do passageiro está na mesma rede.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = "Dispositivo passageiro",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dispositivo passageiro",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF2E7D32), CircleShape),
                            )
                            Text(
                                text = "Conectado",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Text(
                            text = buildString {
                                append("Wi-Fi • ")
                                append(connectedClients.first())
                                if (connectedClients.size > 1) {
                                    append(" • +${connectedClients.size - 1}")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionsGrid(
    actionsEnabled: Boolean,
    activeAction: QuickAction?,
    onShowPix: () -> Unit,
    onShareWifi: () -> Unit,
    onCapturePix: () -> Unit,
    onClearScreen: () -> Unit,
) {
    BoxWithConstraints {
        val singleColumn = maxWidth < 300.dp
        if (singleColumn) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    label = "Exibir QR Pix",
                    icon = Icons.Default.QrCode,
                    primary = true,
                    enabled = actionsEnabled,
                    loading = false,
                    onClick = onShowPix,
                )
                QuickActionCard(
                    label = "Compartilhar Wi-Fi",
                    icon = Icons.Default.Wifi,
                    enabled = actionsEnabled,
                    loading = false,
                    onClick = onShareWifi,
                )
                QuickActionCard(
                    label = "Capturar Pix",
                    icon = Icons.Default.CameraAlt,
                    enabled = actionsEnabled && activeAction == null,
                    loading = activeAction == QuickAction.CapturePix,
                    onClick = onCapturePix,
                )
                QuickActionCard(
                    label = "Apagar tela",
                    icon = Icons.Default.PhonelinkErase,
                    enabled = actionsEnabled && activeAction == null,
                    loading = activeAction == QuickAction.ClearPassengerScreen,
                    onClick = onClearScreen,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickActionCard(
                        label = "Exibir QR Pix",
                        icon = Icons.Default.QrCode,
                        primary = true,
                        enabled = actionsEnabled,
                        loading = false,
                        modifier = Modifier.weight(1f),
                        onClick = onShowPix,
                    )
                    QuickActionCard(
                        label = "Compartilhar Wi-Fi",
                        icon = Icons.Default.Wifi,
                        enabled = actionsEnabled,
                        loading = false,
                        modifier = Modifier.weight(1f),
                        onClick = onShareWifi,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickActionCard(
                        label = "Capturar Pix",
                        icon = Icons.Default.CameraAlt,
                        enabled = actionsEnabled && activeAction == null,
                        loading = activeAction == QuickAction.CapturePix,
                        modifier = Modifier.weight(1f),
                        onClick = onCapturePix,
                    )
                    QuickActionCard(
                        label = "Apagar tela",
                        icon = Icons.Default.PhonelinkErase,
                        enabled = actionsEnabled && activeAction == null,
                        loading = activeAction == QuickAction.ClearPassengerScreen,
                        modifier = Modifier.weight(1f),
                        onClick = onClearScreen,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
) {
    val containerColor = if (primary) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (primary) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.45f),
            disabledContentColor = contentColor.copy(alpha = 0.45f),
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (primary) 2.dp else 0.dp,
            pressedElevation = 1.dp,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp,
                    color = contentColor,
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(24.dp),
                    tint = if (primary) contentColor else MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}
