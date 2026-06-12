package com.example.network

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

object BluetoothClientHelper {
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    fun getPairedDevices(context: Context): List<BluetoothDevice> {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter ?: return emptyList()
        if (!bluetoothAdapter.isEnabled) return emptyList()
        return bluetoothAdapter.bondedDevices.toList()
    }

    @SuppressLint("MissingPermission")
    suspend fun connectToDriver(
        context: Context,
        device: BluetoothDevice,
        onConfigReceived: (ssid: String, pass: String, ip: String) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        var socket: BluetoothSocket? = null
        try {
            socket = device.createRfcommSocketToServiceRecord(MY_UUID)
            socket.connect()
            Log.d("BluetoothClient", "Conectado ao dispositivo Bluetooth: ${device.name}")

            val reader = BufferedReader(InputStreamReader(socket.inputStream))
            val line = reader.readLine()
            if (line != null && line.startsWith("WIFI_CONFIG:")) {
                val data = line.substringAfter("WIFI_CONFIG:").split(",")
                if (data.size >= 3) {
                    val ssid = data[0]
                    val pass = data[1]
                    val ip = data[2]
                    Log.d("BluetoothClient", "Configuração recebida: SSID=$ssid, Pass=$pass, IP=$ip")
                    onConfigReceived(ssid, pass, ip)
                } else {
                    onError("Dados inválidos recebidos via Bluetooth.")
                }
            } else {
                onError("Nenhum dado recebido do motorista.")
            }
        } catch (e: Exception) {
            Log.e("BluetoothClient", "Erro de conexão Bluetooth: ${e.message}")
            onError(e.message ?: "Falha ao conectar via Bluetooth.")
        } finally {
            try { socket?.close() } catch (ignored: Exception) {}
        }
    }
}
