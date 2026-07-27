package com.alexlopes.pixdrive.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ModeSelectionScreen(
    onDriverSelected: () -> Unit,
    onPassengerSelected: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val horizontalCards =
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
            configuration.screenWidthDp >= 720

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(
                    id = com.alexlopes.pixdrive.R.drawable.ic_launcher_foreground
                ),
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.Unspecified,
                modifier = Modifier.size(76.dp)
            )
            Text(
                text = "PixDrive",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Como este aparelho será usado?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Escolha o modo deste dispositivo. Você poderá alterar essa opção depois nas configurações.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))

            if (horizontalCards) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    DeviceModeCard(
                        title = "Motorista",
                        description = "Use este celular para enviar Pix, Wi-Fi e controlar o visor traseiro.",
                        highlight = "Controla o visor",
                        buttonLabel = "Usar como motorista",
                        icon = Icons.Default.DirectionsCar,
                        onClick = onDriverSelected,
                        testTag = "driver_button",
                        modifier = Modifier.weight(1f)
                    )
                    DeviceModeCard(
                        title = "Visor do passageiro",
                        description = "Use este aparelho no banco traseiro para exibir QR Codes e informações aos passageiros.",
                        highlight = "Recebe e exibe conteúdos",
                        buttonLabel = "Usar como visor",
                        icon = Icons.Default.QrCode2,
                        onClick = onPassengerSelected,
                        testTag = "passenger_button",
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DeviceModeCard(
                        title = "Motorista",
                        description = "Use este celular para enviar Pix, Wi-Fi e controlar o visor traseiro.",
                        highlight = "Controla o visor",
                        buttonLabel = "Usar como motorista",
                        icon = Icons.Default.DirectionsCar,
                        onClick = onDriverSelected,
                        testTag = "driver_button"
                    )
                    DeviceModeCard(
                        title = "Visor do passageiro",
                        description = "Use este aparelho no banco traseiro para exibir QR Codes e informações aos passageiros.",
                        highlight = "Recebe e exibe conteúdos",
                        buttonLabel = "Usar como visor",
                        icon = Icons.Default.QrCode2,
                        onClick = onPassengerSelected,
                        testTag = "passenger_button"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Você poderá alterar essa escolha nas configurações.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DeviceModeCard(
    title: String,
    description: String,
    highlight: String,
    buttonLabel: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = highlight,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag(testTag)
            ) {
                Text(buttonLabel)
            }
        }
    }
}
