package com.alexlopes.pixdrive.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.alexlopes.pixdrive.MainActivity
import com.alexlopes.pixdrive.PixActivity
import kotlinx.coroutines.*

class PassengerService : Service() {

    companion object {
        private const val CHANNEL_ID = "passenger_service_channel"
        private const val NOTIFICATION_ID = 2002

        const val ACTION_START = "com.alexlopes.pixdrive.action.START_PASSENGER"
        const val ACTION_STOP = "com.alexlopes.pixdrive.action.STOP_PASSENGER"
        
        const val EXTRA_IP = "extra_ip"
        const val EXTRA_PORT = "extra_port"
        const val EXTRA_AUTO_RECONNECT = "extra_auto_reconnect"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private lateinit var prefs: SharedPreferences

    private var connectionJob: Job? = null
    private var telemetryJob: Job? = null
    private var wakeUpJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("PixPrefs", Context.MODE_PRIVATE)
        acquireLocks()
        createNotificationChannel()
        startWakeUpObserver()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        
        if (action == ACTION_STOP) {
            stopForegroundService(startId)
            return START_NOT_STICKY
        }

        val ip = intent?.getStringExtra(EXTRA_IP) ?: prefs.getString("LAST_IP", "") ?: ""
        val port = intent?.getIntExtra(EXTRA_PORT, 8080) ?: 8080
        val autoReconnect = intent?.getBooleanExtra(EXTRA_AUTO_RECONNECT, false) ?: false

        // Inicialização obrigatória do serviço em primeiro plano para conformidade com Android 14+
        val notification = getNotification("Conectando ao painel do motorista...")
        startForeground(NOTIFICATION_ID, notification)
        acquireLocks()
        startWakeUpObserver()
        
        startConnectionLoop(ip, port, autoReconnect)
        
        return START_STICKY
    }

    private fun startConnectionLoop(targetIp: String, port: Int, autoReconnect: Boolean) {
        connectionJob?.cancel()
        telemetryJob?.cancel()
        UdpDiscovery.stopClientDiscovery()
        TcpClient.disconnect()
        connectionJob = serviceScope.launch {
            // Observer reativo: atualiza a notificação assim que o estado de conexão mudar,
            // independente do connect() bloqueante.
            launch {
                TcpClient.isConnected.collect { connected ->
                    if (connected) {
                        updateNotification("Conectado ao painel do motorista")
                        startTelemetryLoop()
                    }
                }
            }

            val lastKnownIp = targetIp.takeIf {
                it.isNotEmpty() && it != "192.168."
            }
            var currentIp = targetIp
            while (isActive) {
                if (!TcpClient.isConnected.value) {
                    updateNotification("Reconectando...")
                    
                    // 1. Tenta conexões diretas no IP conhecido
                    if (currentIp.isNotEmpty() && currentIp != "192.168.") {
                        Log.d("PassengerService", "Tentando conectar ao IP: $currentIp na porta: $port")
                        TcpClient.connect(currentIp, port)
                    }

                    // 2. Se falhou e o auto-reconnect estiver ligado, faz busca via UDP Discovery
                    if (!TcpClient.isConnected.value && autoReconnect) {
                        updateNotification("Buscando painel do motorista na rede...")
                        delay(2000) // Backoff para poupar processamento
                        
                        val discoveredIp = UdpDiscovery.discoverServerIp()
                        if (discoveredIp != null) {
                            currentIp = discoveredIp
                            prefs.edit().putString("LAST_IP", discoveredIp).apply()
                            Log.d("PassengerService", "Motorista descoberto via UDP: $discoveredIp")
                            TcpClient.connect(discoveredIp, port)
                        }

                        // 3. Ao terminar a busca sem conexão, tenta novamente o
                        // último endereço conhecido como fallback do ciclo.
                        if (
                            !TcpClient.isConnected.value &&
                            discoveredIp == null &&
                            lastKnownIp != null
                        ) {
                            currentIp = lastKnownIp
                            updateNotification("Tentando último IP conhecido...")
                            Log.d(
                                "PassengerService",
                                "Busca concluída; tentando novamente o último IP: $lastKnownIp"
                            )
                            TcpClient.connect(lastKnownIp, port)
                        }

                        if (!TcpClient.isConnected.value) {
                            delay(2000) // Backoff extra antes do próximo ciclo
                        }
                    } else if (!TcpClient.isConnected.value && !autoReconnect) {
                        break
                    }
                } else {
                    // Mantém o laço ativo enquanto a conexão estiver ativa
                    while (TcpClient.isConnected.value && isActive) {
                        delay(1000)
                    }
                    if (!autoReconnect) {
                        break
                    }
                }
            }
            
            if (!TcpClient.isConnected.value) {
                stopForegroundService()
            }
        }
    }

    private fun startTelemetryLoop() {
        telemetryJob?.cancel()
        telemetryJob = serviceScope.launch {
            while (isActive && TcpClient.isConnected.value) {
                val pct = getBatteryPercentage()
                if (pct >= 0) {
                    TcpClient.sendTelemetry("TELEMETRY_BATTERY:$pct")
                }
                delay(60000L) // Telemetria de bateria a cada 60 segundos
            }
        }
    }

    private fun getBatteryPercentage(): Int {
        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100 / scale) else -1
    }

    private fun getNotification(content: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PixDrive - Visor Traseiro")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, getNotification(content))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Canal do Passageiro",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantém a escuta de QR Codes do motorista em segundo plano."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    @Suppress("DEPRECATION")
    private fun acquireLocks() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PixNoBancoDeTras::PassengerWakeLock").apply {
                acquire(8 * 60 * 60 * 1000L) // Timeout de 8 horas como rede de segurança
            }

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiLock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "PixNoBancoDeTras::PassengerWifiLock")
            } else {
                wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "PixNoBancoDeTras::PassengerWifiLock")
            }.apply {
                acquire()
            }
            Log.d("PassengerService", "Locks de CPU e Wi-Fi adquiridos.")
        } catch (e: Exception) {
            Log.e("PassengerService", "Erro ao obter locks de hardware: ${e.message}")
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            if (wifiLock?.isHeld == true) wifiLock?.release()
            Log.d("PassengerService", "Locks de hardware liberados.")
        } catch (e: Exception) {
            Log.e("PassengerService", "Erro ao liberar locks: ${e.message}")
        }
    }

    private fun stopForegroundService(startId: Int? = null) {
        connectionJob?.cancel()
        telemetryJob?.cancel()
        wakeUpJob?.cancel()
        UdpDiscovery.stopClientDiscovery()
        TcpClient.disconnect()
        releaseLocks()
        stopForeground(true)
        if (startId != null) {
            stopSelf(startId)
        } else {
            stopSelf()
        }
    }

    override fun onDestroy() {
        connectionJob?.cancel()
        telemetryJob?.cancel()
        wakeUpJob?.cancel()
        UdpDiscovery.stopClientDiscovery()
        TcpClient.disconnect()
        releaseLocks()
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * Observa os comandos recebidos pelo TcpClient para ligar a tela do tablet
     * quando o motorista envia um Pix/Wi-Fi/Obrigado. Esta lógica fica no Service
     * (sempre ativo) em vez da Activity (pode ser destruída pelo sistema).
     */
    private fun startWakeUpObserver() {
        wakeUpJob?.cancel()
        wakeUpJob = serviceScope.launch {
            TcpClient.command.collect { cmd ->
                if (cmd != null && (cmd.startsWith("CMD_EXIBIR"))) {
                    wakeUpScreen(cmd)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun wakeUpScreen(cmd: String) {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val screenLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
                "PixNoBancoDeTras::ScreenWakeLock"
            )
            screenLock.acquire(5000)

            // Lança a PixActivity via Full-Screen Intent para exibir sobre a tela bloqueada
            val intent = Intent(this, PixActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "pix_alerts"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Alertas de Pix",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }

            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Novo QR Code")
                .setContentText("Você tem um novo QR Code na tela.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(1001, notification)
            Log.d("PassengerService", "Tela acordada para comando: $cmd")
        } catch (e: Exception) {
            Log.e("PassengerService", "Erro ao acordar tela: ${e.message}")
        }
    }
}
