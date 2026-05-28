package com.example.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.net.Socket

object TcpClient {
    private var socket: Socket? = null
    private var inputStream: DataInputStream? = null
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    var isRunning = false

    private val _receivedImage = MutableStateFlow<ByteArray?>(null)
    val receivedImage: StateFlow<ByteArray?> = _receivedImage.asStateFlow()

    private val _qrCodeText = MutableStateFlow<String?>(null)
    val qrCodeText: StateFlow<String?> = _qrCodeText.asStateFlow()

    private val _command = MutableStateFlow<String?>(null)
    val command: StateFlow<String?> = _command.asStateFlow()

    var onExibirPixCallback: ((String) -> Unit)? = null
    var onFecharPixCallback: (() -> Unit)? = null

    suspend fun connect(ip: String, port: Int = 8080) = withContext(Dispatchers.IO) {
        if (isRunning) return@withContext
        try {
            socket = Socket()
            socket?.connect(java.net.InetSocketAddress(ip, port), 1000)
            inputStream = DataInputStream(socket?.getInputStream())
            isRunning = true
            _isConnected.value = true
            Log.d("TcpClient", "Conectado ao servidor $ip:$port")

            while (isRunning) {
                val cmd = inputStream?.readUTF() ?: break
                Log.d("TcpClient", "Comando recebido: $cmd")
                _command.value = cmd

                if (cmd == "CMD_EXIBIR_PIX" || cmd == "CMD_EXIBIR_MEU_PIX" || cmd == "CMD_EXIBIR_WIFI") {
                    onExibirPixCallback?.invoke(cmd)
                    val qrText = inputStream?.readUTF() ?: ""
                    if (qrText.isNotEmpty()) {
                        _qrCodeText.value = qrText
                        Log.d("TcpClient", "QR Code texto recebido: ${qrText.take(50)}...")
                    }
                } else if (cmd == "CMD_APAGAR_TELA" || cmd == "CMD_LIMPAR_TELA") {
                    onFecharPixCallback?.invoke()
                    _receivedImage.value = null
                    _qrCodeText.value = null
                }
            }
        } catch (e: Exception) {
            Log.e("TcpClient", "Erro no cliente: ${e.message}")
        } finally {
            disconnect()
        }
    }

    fun clearImage() {
        _receivedImage.value = null
        _qrCodeText.value = null
    }

    fun disconnect() {
        isRunning = false
        _isConnected.value = false
        try {
            inputStream?.close()
            socket?.close()
            socket = null
        } catch (e: Exception) {
             Log.e("TcpClient", "Erro ao desconectar: ${e.message}")
        }
    }
}

