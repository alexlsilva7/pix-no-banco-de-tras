package com.example.ui.screens

import android.content.Context
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
        onDispose {
            if (originalOrientation != null) {
                activity?.requestedOrientation = originalOrientation
            }
        }
    }

    val density = LocalDensity.current.density

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
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

        // Área para arrastar
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(absX.roundToInt(), absY.roundToInt()) }
                    .size((absW / density).dp, (absH / density).dp)
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(2.dp, MaterialTheme.colorScheme.primary)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            absX = (absX + dragAmount.x).coerceIn(0f, screenW - absW)
                            absY = (absY + dragAmount.y).coerceIn(0f, screenH - absH)
                        }
                    }
            ) {
                Text(
                    "Arraste para mover",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
                // Ícone/Área de redimensionar
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                absW = (absW + dragAmount.x).coerceIn(100f * density, screenW - absX)
                                absH = (absH + dragAmount.y).coerceIn(100f * density, screenH - absY)
                            }
                        }
                )
            }
        }

        TopAppBar(
            title = { Text("Área do QR Code", color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
            },
            actions = {
                IconButton(onClick = {
                    prefs.edit()
                        .putFloat("PARTIAL_QR_X", absX / screenW)
                        .putFloat("PARTIAL_QR_Y", absY / screenH)
                        .putFloat("PARTIAL_QR_WIDTH", absW / screenW)
                        .putFloat("PARTIAL_QR_HEIGHT", absH / screenH)
                        .apply()
                    onBack()
                }) {
                    Icon(Icons.Default.Check, contentDescription = "Salvar", tint = Color.Green)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
    }
}
