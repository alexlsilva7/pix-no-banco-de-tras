package com.alexlopes.pixdrive.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.net.Socket

object TcpClient {
    private val connectionLock = Any()
    private var socket: Socket? = null
    private var inputStream: DataInputStream? = null
    private var outputStream: java.io.DataOutputStream? = null
    private var connectionGeneration = 0L
    
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
        val clientSocket: Socket
        val generation: Long
        synchronized(connectionLock) {
            if (isRunning || socket != null) return@withContext
            clientSocket = Socket()
            socket = clientSocket
            connectionGeneration += 1
            generation = connectionGeneration
        }

        var clientInput: DataInputStream? = null
        var clientOutput: java.io.DataOutputStream? = null
        try {
            clientSocket.connect(java.net.InetSocketAddress(ip, port), 1000)
            clientInput = DataInputStream(clientSocket.getInputStream())
            clientOutput = java.io.DataOutputStream(clientSocket.getOutputStream())

            synchronized(connectionLock) {
                if (connectionGeneration != generation || socket !== clientSocket) {
                    return@withContext
                }
                inputStream = clientInput
                outputStream = clientOutput
                isRunning = true
                _isConnected.value = true
            }
            Log.d("TcpClient", "Conectado ao servidor $ip:$port")

            while (isCurrentConnection(generation, clientSocket)) {
                val cmd = clientInput?.readUTF() ?: break
                Log.d("TcpClient", "Comando recebido: $cmd")
                
                // REVERTIDO: IGNORAR o PING para não sobrescrever o estado da UI atual
                if (cmd != "CMD_PING") {
                    _command.value = cmd
                }

                if (cmd == "CMD_EXIBIR_PIX" || cmd == "CMD_EXIBIR_MEU_PIX" || cmd == "CMD_EXIBIR_WIFI" || cmd == "CMD_EXIBIR_BEM_VINDO" || cmd == "CMD_EXIBIR_OBRIGADO") {
                    onExibirPixCallback?.invoke(cmd)
                    val qrText = clientInput?.readUTF() ?: ""
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
            closeConnection(generation, clientSocket, clientInput, clientOutput)
        }
    }

    private fun isCurrentConnection(generation: Long, clientSocket: Socket): Boolean =
        synchronized(connectionLock) {
            isRunning &&
                connectionGeneration == generation &&
                socket === clientSocket
        }

    private fun closeConnection(
        generation: Long,
        clientSocket: Socket,
        clientInput: DataInputStream?,
        clientOutput: java.io.DataOutputStream?
    ) {
        synchronized(connectionLock) {
            if (connectionGeneration == generation && socket === clientSocket) {
                isRunning = false
                _isConnected.value = false
                socket = null
                inputStream = null
                outputStream = null
            }
        }
        try {
            clientInput?.close()
            clientOutput?.close()
            clientSocket.close()
        } catch (e: Exception) {
            Log.e("TcpClient", "Erro ao fechar conexão: ${e.message}")
        }
    }

    fun clearImage() {
        _receivedImage.value = null
        _qrCodeText.value = null
    }

    fun disconnect() {
        val currentSocket: Socket?
        val currentInput: DataInputStream?
        val currentOutput: java.io.DataOutputStream?
        synchronized(connectionLock) {
            connectionGeneration += 1
            isRunning = false
            _isConnected.value = false
            currentSocket = socket
            currentInput = inputStream
            currentOutput = outputStream
            socket = null
            inputStream = null
            outputStream = null
        }
        try {
            currentInput?.close()
            currentOutput?.close()
            currentSocket?.close()
        } catch (e: Exception) {
             Log.e("TcpClient", "Erro ao desconectar: ${e.message}")
        }
    }

    suspend fun sendTelemetry(msg: String) = withContext(Dispatchers.IO) {
        try {
            outputStream?.writeUTF(msg)
            outputStream?.flush()
        } catch (e: Exception) {
            Log.e("TcpClient", "Erro ao enviar telemetria: ${e.message}")
        }
    }
}
