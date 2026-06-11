package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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
  val screenWidthDp = configuration.screenWidthDp
  val useHorizontalLayout = isLandscape && screenWidthDp >= 640

  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 48.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(
        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_launcher_foreground),
        contentDescription = null,
        tint = Color.Unspecified,
        modifier = Modifier.size(if (isLandscape) 64.dp else 96.dp)
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
      Spacer(modifier = Modifier.height(24.dp))
      
      if (useHorizontalLayout) {
        androidx.compose.foundation.layout.Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // Coluna Esquerda
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DriverCard(onDriverSelected)
            MyPixCard(onMyPixQrCodeSelected)
          }
          // Coluna Direita
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PassengerCard(onPassengerSelected)
            MyWifiCard(onMyWifiQrCodeSelected)
          }
        }
      } else {
        // Disposição Vertical em Celulares em Pé ou Paisagens Estreitas
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          DriverCard(onDriverSelected)
          MyPixCard(onMyPixQrCodeSelected)
          MyWifiCard(onMyWifiQrCodeSelected)
          PassengerCard(onPassengerSelected)
        }
      }
    }

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
  }
}

@Composable
fun DriverCard(onClick: () -> Unit) {
  androidx.compose.material3.ElevatedCard(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().height(100.dp).testTag("driver_button"),
    shape = RoundedCornerShape(20.dp),
    colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer,
      contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
      androidx.compose.foundation.layout.Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = "Motorista", modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
           Text("Motorista", style = MaterialTheme.typography.titleMedium)
           Text("Compartilhe a tela (Servidor)", style = MaterialTheme.typography.bodySmall)
        }
      }
    }
  }
}

@Composable
fun MyPixCard(onClick: () -> Unit) {
  androidx.compose.material3.ElevatedCard(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().height(100.dp).testTag("my_pix_qr_button"),
    shape = RoundedCornerShape(20.dp),
    colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.tertiaryContainer,
      contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    )
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
      androidx.compose.foundation.layout.Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.QrCode, contentDescription = "Meu QR Code", modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
           Text("Meu QR Code Pix", style = MaterialTheme.typography.titleMedium)
           Text("Exibir código de pagamento", style = MaterialTheme.typography.bodySmall)
        }
      }
    }
  }
}

@Composable
fun MyWifiCard(onClick: () -> Unit) {
  androidx.compose.material3.ElevatedCard(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().height(100.dp).testTag("my_wifi_qr_button"),
    shape = RoundedCornerShape(20.dp),
    colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
      containerColor = Color(0xFF4A148C), contentColor = Color.White
    )
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
      androidx.compose.foundation.layout.Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.Wifi, contentDescription = "Meu Wi-Fi", modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
           Text("Wi-Fi do Carro", style = MaterialTheme.typography.titleMedium)
           Text("Compartilhar internet", style = MaterialTheme.typography.bodySmall)
        }
      }
    }
  }
}

@Composable
fun PassengerCard(onClick: () -> Unit) {
  androidx.compose.material3.ElevatedCard(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().height(100.dp).testTag("passenger_button"),
    shape = RoundedCornerShape(20.dp),
    colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.secondaryContainer,
      contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    )
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
      androidx.compose.foundation.layout.Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.Person, contentDescription = "Passageiro", modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
           Text("Passageiro (Tablet)", style = MaterialTheme.typography.titleMedium)
           Text("Visualizar Pix/Cliente", style = MaterialTheme.typography.bodySmall)
        }
      }
    }
  }
}
