package com.example.network

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

object BluetoothServerHelper {
    private const val NAME = "PixNoBancoDeTras"
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    
    private var serverSocket: BluetoothServerSocket? = null
    
    private val _isBluetoothServerRunning = MutableStateFlow(false)
    val isBluetoothServerRunning: StateFlow<Boolean> = _isBluetoothServerRunning

    val isRunning: Boolean get() = _isBluetoothServerRunning.value

    @SuppressLint("MissingPermission")
    suspend fun startBluetoothServer(context: Context, ssid: String, pass: String, ip: String) = withContext(Dispatchers.IO) {
        if (isRunning) return@withContext
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter ?: return@withContext
        if (!bluetoothAdapter.isEnabled) return@withContext

        try {
            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID)
            _isBluetoothServerRunning.value = true
            Log.d("BluetoothServer", "Servidor Bluetooth RFCOMM iniciado.")

            while (isRunning) {
                var socket: BluetoothSocket? = null
                try {
                    socket = serverSocket?.accept()
                } catch (e: IOException) {
                    Log.e("BluetoothServer", "Socket accept() fechado ou servidor parado.")
                    break
                }

                if (socket != null) {
                    try {
                        val outputStream = socket.outputStream
                        val payload = "WIFI_CONFIG:$ssid,$pass,$ip\n"
                        outputStream.write(payload.toByteArray())
                        outputStream.flush()
                        Log.d("BluetoothServer", "Payload enviado via Bluetooth: $payload")
                    } catch (e: Exception) {
                        Log.e("BluetoothServer", "Erro ao enviar dados Bluetooth: ${e.message}")
                    } finally {
                        try { socket.close() } catch (ignored: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BluetoothServer", "Erro no servidor Bluetooth: ${e.message}")
        } finally {
            stopBluetoothServer()
        }
    }

    fun stopBluetoothServer() {
        if (!_isBluetoothServerRunning.value) return
        _isBluetoothServerRunning.value = false
        try {
            serverSocket?.close()
            serverSocket = null
            Log.d("BluetoothServer", "Servidor Bluetooth RFCOMM parado manualmente.")
        } catch (e: Exception) {
            Log.e("BluetoothServer", "Erro ao fechar BluetoothServerSocket: ${e.message}")
        }
    }
}
