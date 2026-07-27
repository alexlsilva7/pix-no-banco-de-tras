package com.alexlopes.pixdrive

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
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
import com.alexlopes.pixdrive.ui.theme.MyApplicationTheme


import com.alexlopes.pixdrive.ui.screens.*
import com.alexlopes.pixdrive.utils.*

const val ModeSelectionRoute = "mode_selection"
const val DriverRoute = "driver"
const val PassengerRoute = "passenger"
const val SettingsRoute = "settings"
const val ConfirmDriverRoute = "confirm_driver"
const val ConfirmPassengerRoute = "confirm_passenger"
const val ConfirmDriverChangeRoute = "confirm_driver_change"
const val ConfirmPassengerChangeRoute = "confirm_passenger_change"
const val DriverPermissionsRoute = "driver_permissions"
const val PassengerPermissionsRoute = "passenger_permissions"
const val DriverConnectionTestRoute = "driver_connection_test"
const val PassengerConnectionTestRoute = "passenger_connection_test"
const val MyPixQrCodeRoute = "my_pix_qr_code"
const val MyWifiQrCodeRoute = "my_wifi_qr_code"
const val PartialSetupRoute = "partial_setup"
const val QrCodeSizeRoute = "qr_code_size"

class MainActivity : ComponentActivity() {
  private fun stopRuntimeFor(mode: DeviceMode?) {
    when (mode) {
      DeviceMode.DRIVER -> {
        com.alexlopes.pixdrive.network.TcpServer.stopServer()
        com.alexlopes.pixdrive.network.BluetoothServerHelper.stopBluetoothServer()
        startService(
          android.content.Intent(this, OverlayService::class.java).apply {
            putExtra("action", "HIDE_BUBBLE")
          }
        )
      }
      DeviceMode.PASSENGER_DISPLAY -> {
        com.alexlopes.pixdrive.network.TcpClient.disconnect()
        startService(
          android.content.Intent(
            this,
            com.alexlopes.pixdrive.network.PassengerService::class.java
          ).apply {
            action =
              com.alexlopes.pixdrive.network.PassengerService.ACTION_STOP
          }
        )
      }
      null -> Unit
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Register callback to wake up screen when Pix is received
    com.alexlopes.pixdrive.network.TcpClient.onExibirPixCallback = { cmd ->
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
        val initialMode = remember { DeviceModePreferences.get(this@MainActivity) }
        val startRoute = when (initialMode) {
          DeviceMode.DRIVER -> DriverRoute
          DeviceMode.PASSENGER_DISPLAY -> PassengerRoute
          null -> ModeSelectionRoute
        }

        fun openMain(mode: DeviceMode) {
          val destination =
            if (mode == DeviceMode.DRIVER) DriverRoute else PassengerRoute
          navController.navigate(destination) {
            popUpTo(ModeSelectionRoute) { inclusive = true }
            launchSingleTop = true
          }
        }
        
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(innerPadding)
          ) {
            composable(ModeSelectionRoute) {
              ModeSelectionScreen(
                onDriverSelected = {
                  val isChange = DeviceModePreferences.get(this@MainActivity) != null
                  navController.navigate(
                    if (isChange) ConfirmDriverChangeRoute else ConfirmDriverRoute
                  )
                },
                onPassengerSelected = {
                  val isChange = DeviceModePreferences.get(this@MainActivity) != null
                  navController.navigate(
                    if (isChange) ConfirmPassengerChangeRoute
                    else ConfirmPassengerRoute
                  )
                }
              )
            }
            composable(DriverRoute) {
              DriverScreen(
                onSettings = { navController.navigate(SettingsRoute) },
                onMyPix = { navController.navigate(MyPixQrCodeRoute) },
                onMyWifi = { navController.navigate(MyWifiQrCodeRoute) }
              )
            }
            composable(PassengerRoute) {
              PassengerScreen(
                onSettings = { navController.navigate(SettingsRoute) }
              )
            }
            composable(ConfirmDriverRoute) {
              ModeConfirmationScreen(
                mode = DeviceMode.DRIVER,
                isModeChange = false,
                onContinue = {
                  stopRuntimeFor(DeviceModePreferences.get(this@MainActivity))
                  DeviceModePreferences.set(this@MainActivity, DeviceMode.DRIVER)
                  navController.navigate(DriverPermissionsRoute)
                }
              )
            }
            composable(ConfirmDriverChangeRoute) {
              ModeConfirmationScreen(
                mode = DeviceMode.DRIVER,
                isModeChange = true,
                onContinue = {
                  stopRuntimeFor(DeviceModePreferences.get(this@MainActivity))
                  DeviceModePreferences.set(this@MainActivity, DeviceMode.DRIVER)
                  navController.navigate(DriverPermissionsRoute)
                },
                onCancel = { navController.popBackStack() }
              )
            }
            composable(ConfirmPassengerRoute) {
              ModeConfirmationScreen(
                mode = DeviceMode.PASSENGER_DISPLAY,
                isModeChange = false,
                onContinue = {
                  stopRuntimeFor(DeviceModePreferences.get(this@MainActivity))
                  DeviceModePreferences.set(
                    this@MainActivity,
                    DeviceMode.PASSENGER_DISPLAY
                  )
                  navController.navigate(PassengerPermissionsRoute)
                }
              )
            }
            composable(ConfirmPassengerChangeRoute) {
              ModeConfirmationScreen(
                mode = DeviceMode.PASSENGER_DISPLAY,
                isModeChange = true,
                onContinue = {
                  stopRuntimeFor(DeviceModePreferences.get(this@MainActivity))
                  DeviceModePreferences.set(
                    this@MainActivity,
                    DeviceMode.PASSENGER_DISPLAY
                  )
                  navController.navigate(PassengerPermissionsRoute)
                },
                onCancel = { navController.popBackStack() }
              )
            }
            composable(DriverPermissionsRoute) {
              ModePermissionsScreen(
                mode = DeviceMode.DRIVER,
                onContinue = { navController.navigate(DriverConnectionTestRoute) }
              )
            }
            composable(PassengerPermissionsRoute) {
              ModePermissionsScreen(
                mode = DeviceMode.PASSENGER_DISPLAY,
                onContinue = { navController.navigate(PassengerConnectionTestRoute) }
              )
            }
            composable(DriverConnectionTestRoute) {
              ConnectionTestScreen(
                mode = DeviceMode.DRIVER,
                onFinish = { openMain(DeviceMode.DRIVER) }
              )
            }
            composable(PassengerConnectionTestRoute) {
              ConnectionTestScreen(
                mode = DeviceMode.PASSENGER_DISPLAY,
                onFinish = { openMain(DeviceMode.PASSENGER_DISPLAY) }
              )
            }
            composable(SettingsRoute) {
              SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPartialSetup = { navController.navigate(PartialSetupRoute) },
                currentMode = DeviceModePreferences.get(this@MainActivity),
                onChangeMode = {
                  navController.navigate(ModeSelectionRoute) {
                    popUpTo(navController.graph.startDestinationId) {
                      inclusive = true
                    }
                    launchSingleTop = true
                  }
                },
                onResetApp = {
                  stopRuntimeFor(DeviceModePreferences.get(this@MainActivity))
                  getSharedPreferences(
                    DeviceModePreferences.PREFERENCES_NAME,
                    android.content.Context.MODE_PRIVATE
                  ).edit().clear().apply()
                  navController.navigate(ModeSelectionRoute) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                  }
                }
              )
            }
            composable(PartialSetupRoute) {
              PartialDisplaySetupScreen(onBack = { navController.popBackStack() })
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
    if (com.alexlopes.pixdrive.network.TcpClient.onExibirPixCallback != null) {
      com.alexlopes.pixdrive.network.TcpClient.onExibirPixCallback = null
    }
  }
}


