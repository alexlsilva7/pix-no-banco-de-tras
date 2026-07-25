package com.example.ui.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun AccessibilityDisclosureDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Uso do Serviço de Acessibilidade", color = Color.White) },
        text = {
            Text(
                text = "O aplicativo 'Pix no Banco de Trás' requer a permissão de Acessibilidade para:\n\n" +
                       "• Capturar a tela anonimamente quando você acionar a busca de QR Code.\n" +
                       "• Ler e extrair dados da chave Pix da corrida (Uber/99).\n\n" +
                       "🔒 Privacidade Garantida: Nenhuma informação pessoal ou tela do seu dispositivo é gravada, salva ou enviada para a internet. Todos os dados permanecem estritamente dentro da rede Wi-Fi local do seu veículo.",
                color = Color.LightGray,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Entendi e Concordo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF1E1E1E)
    )
}
