package com.example.ui.screens

import android.content.Context
import android.content.pm.ActivityInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.utils.getActivity
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartialDisplaySetupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("PixPrefs", Context.MODE_PRIVATE) }
    val passengerOrientation = prefs.getString("PASSENGER_ORIENTATION", "LANDSCAPE") ?: "LANDSCAPE"
    val colorStyle = remember { prefs.getString("PARTIAL_QR_COLOR_STYLE", "WHITE_BG_BLACK_QR") ?: "WHITE_BG_BLACK_QR" }

    DisposableEffect(passengerOrientation) {
        val activity = context.getActivity()
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = when (passengerOrientation) {
            "PORTRAIT" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "REVERSE_PORTRAIT" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            "REVERSE_LANDSCAPE" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            "AUTO" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            else -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

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

    // Gerador do QR Code com cores dinâmicas
    val samplePayload = "00020101021126360014br.gov.bcb.pix0114+5587981504902520400005303986540515.005802BR5919Alex Lopes da Silva6011GaranhunsPE62070503***6304539E"
    val qrBitmap = remember(samplePayload, colorStyle) {
        try {
            val writer = com.google.zxing.qrcode.QRCodeWriter()
            val hints = mapOf(com.google.zxing.EncodeHintType.MARGIN to 0)
            val bitMatrix = writer.encode(samplePayload, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512, hints)
            val w = bitMatrix.width
            val h = bitMatrix.height
            val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)

            val qrColor = if (colorStyle == "BLACK_BG_WHITE_QR") android.graphics.Color.WHITE else android.graphics.Color.BLACK
            val bgColor = if (colorStyle == "BLACK_BG_WHITE_QR") android.graphics.Color.BLACK else android.graphics.Color.WHITE

            for (x in 0 until w) {
                for (y in 0 until h) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) qrColor else bgColor)
                }
            }
            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    val density = LocalDensity.current.density

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()

        var relX by remember { mutableStateOf(prefs.getFloat("PARTIAL_QR_X", 0.1f)) }
        var relY by remember { mutableStateOf(prefs.getFloat("PARTIAL_QR_Y", 0.1f)) }
        var relW by remember { mutableStateOf(prefs.getFloat("PARTIAL_QR_WIDTH", 0.35f)) }
        var relH by remember { mutableStateOf(prefs.getFloat("PARTIAL_QR_HEIGHT", 0.5f)) }

        var absX by remember { mutableStateOf(relX * screenW) }
        var absY by remember { mutableStateOf(relY * screenH) }
        var absW by remember { mutableStateOf(relW * screenW) }
        var absH by remember { mutableStateOf(relH * screenH) }

        val containerBgColor = if (colorStyle == "BLACK_BG_WHITE_QR") Color.Black else Color.White
        val containerBorderColor = if (colorStyle == "BLACK_BG_WHITE_QR") Color.Transparent else MaterialTheme.colorScheme.primary

        // Elemento Movel e Redimensionável (Pré-visualização do QR Code com Estilo e Quadrado/Bordas)
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .offset { IntOffset(absX.roundToInt(), absY.roundToInt()) }
                    .border(2.dp, containerBorderColor, RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size((absW / density).dp, (absH / density).dp)
                        .background(containerBgColor, RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                absX = (absX + dragAmount.x).coerceIn(0f, screenW - absW)
                                absY = (absY + dragAmount.y).coerceIn(0f, screenH - absH)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Amostra QR Code",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // Alça de Redimensionamento no Canto Inferior Direito
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 12.dp))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    absW = (absW + dragAmount.x).coerceIn(80f * density, screenW - absX)
                                    absH = (absH + dragAmount.y).coerceIn(80f * density, screenH - absY)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "Redimensionar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pix: Alex Lopes da Silva | R$ 15,00",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    modifier = Modifier.widthIn(max = (absW / density).dp)
                )
            }
        }

        // Botões flutuantes (Voltar e Salvar) sem App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }

            IconButton(
                onClick = {
                    prefs.edit()
                        .putFloat("PARTIAL_QR_X", absX / screenW)
                        .putFloat("PARTIAL_QR_Y", absY / screenH)
                        .putFloat("PARTIAL_QR_WIDTH", absW / screenW)
                        .putFloat("PARTIAL_QR_HEIGHT", absH / screenH)
                        .apply()
                    onBack()
                },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.Check, contentDescription = "Salvar", tint = Color.Green)
            }
        }
    }
}
