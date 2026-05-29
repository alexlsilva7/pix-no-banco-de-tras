package com.example

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


import com.example.ui.screens.*
import com.example.utils.*

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


