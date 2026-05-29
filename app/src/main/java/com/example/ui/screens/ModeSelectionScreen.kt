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


@Composable
fun ModeSelectionScreen(
  onDriverSelected: () -> Unit,
  onPassengerSelected: () -> Unit,
  onSettingsSelected: () -> Unit,
  onMyPixQrCodeSelected: () -> Unit = {},
  onMyWifiQrCodeSelected: () -> Unit = {}
) {
  val configuration = LocalConfiguration.current
  val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

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
        modifier = Modifier.size(isLandscape.let { if (it) 48.dp else 72.dp }) // Ícone menor em landscape
      )
      Spacer(modifier = Modifier.height(8.dp))
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
      Spacer(modifier = Modifier.height(32.dp))
      
      // Layout condicional: Colunas duplas para landscape (tablets), lista única para retrato
      if (isLandscape) {
        androidx.compose.foundation.layout.Row(
          modifier = Modifier.fillMaxWidth().weight(1f),
          horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
          // Coluna Esquerda
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            DriverCard(onDriverSelected)
            MyPixCard(onMyPixQrCodeSelected)
          }
          // Coluna Direita
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            PassengerCard(onPassengerSelected)
            MyWifiCard(onMyWifiQrCodeSelected)
          }
        }
      } else {
        // Layout original
        DriverCard(onDriverSelected)
        Spacer(modifier = Modifier.height(24.dp))
        MyPixCard(onMyPixQrCodeSelected)
        Spacer(modifier = Modifier.height(24.dp))
        MyWifiCard(onMyWifiQrCodeSelected)
        Spacer(modifier = Modifier.height(24.dp))
        PassengerCard(onPassengerSelected)
      }
    }
  }
}

// Sub-componentes para limpar o código da tela inicial
@Composable
fun DriverCard(onClick: () -> Unit) {
  androidx.compose.material3.ElevatedCard(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().height(120.dp).testTag("driver_button"),
    shape = RoundedCornerShape(24.dp),
    colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer,
      contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
      androidx.compose.foundation.layout.Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = "Motorista", modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.width(24.dp))
        Column {
           Text("Motorista", style = MaterialTheme.typography.titleLarge)
           Text("Compartilhe a tela (Servidor)", style = MaterialTheme.typography.bodyMedium)
        }
      }
    }
  }
}

@Composable
fun MyPixCard(onClick: () -> Unit) {
  androidx.compose.material3.ElevatedCard(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().height(120.dp).testTag("my_pix_qr_button"),
    shape = RoundedCornerShape(24.dp),
    colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.tertiaryContainer,
      contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    )
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
      androidx.compose.foundation.layout.Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.QrCode, contentDescription = "Meu QR Code", modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.width(24.dp))
        Column {
           Text("Meu QR Code Pix", style = MaterialTheme.typography.titleLarge)
           Text("Exibir código de pagamento", style = MaterialTheme.typography.bodyMedium)
        }
      }
    }
  }
}

@Composable
fun MyWifiCard(onClick: () -> Unit) {
  androidx.compose.material3.ElevatedCard(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().height(120.dp).testTag("my_wifi_qr_button"),
    shape = RoundedCornerShape(24.dp),
    colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
      containerColor = Color(0xFF4A148C), contentColor = Color.White
    )
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
      androidx.compose.foundation.layout.Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.Wifi, contentDescription = "Meu Wi-Fi", modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.width(24.dp))
        Column {
           Text("Wi-Fi do Carro", style = MaterialTheme.typography.titleLarge)
           Text("Compartilhar internet", style = MaterialTheme.typography.bodyMedium)
        }
      }
    }
  }
}

@Composable
fun PassengerCard(onClick: () -> Unit) {
  androidx.compose.material3.ElevatedCard(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().height(120.dp).testTag("passenger_button"),
    shape = RoundedCornerShape(24.dp),
    colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.secondaryContainer,
      contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    )
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
      androidx.compose.foundation.layout.Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.Person, contentDescription = "Passageiro", modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.width(24.dp))
        Column {
           Text("Passageiro (Tablet)", style = MaterialTheme.typography.titleLarge)
           Text("Visualizar Pix/Cliente", style = MaterialTheme.typography.bodyMedium)
        }
      }
    }
  }
}


