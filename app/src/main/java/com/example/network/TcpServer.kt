package com.example.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket

object TcpServer {
    private var serverSocket: ServerSocket? = null
    private val clientSockets = mutableListOf<Socket>()
    private val outputStreams = mutableListOf<DataOutputStream>()

    var isRunning = false
    
    private val _connectedClients = kotlinx.coroutines.flow.MutableStateFlow<List<String>>(emptyList())
    val connectedClients: kotlinx.coroutines.flow.StateFlow<List<String>> = _connectedClients

    private val _isServerRunningState = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isServerRunningState: kotlinx.coroutines.flow.StateFlow<Boolean> = _isServerRunningState

    suspend fun startServer(port: Int = 8080) = withContext(Dispatchers.IO) {
        if (isRunning) return@withContext
        try {
            serverSocket = ServerSocket(port)
            isRunning = true
            _isServerRunningState.value = true
            Log.d("TcpServer", "Servidor iniciado na porta $port")
            while (isRunning) {
                val newClient = serverSocket?.accept()
                if (newClient != null) {
                    synchronized(clientSockets) {
                        clientSockets.add(newClient)
                        outputStreams.add(DataOutputStream(newClient.getOutputStream()))
                        _connectedClients.value = clientSockets.mapNotNull { it.inetAddress?.hostAddress }
                    }
                    Log.d("TcpServer", "Cliente conectado: ${newClient.inetAddress.hostAddress}")
                    
                    // Listen for disconnections
                    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                        try {
                            // Simple read to detect disconnect
                            newClient.getInputStream().read()
                        } catch (e: Exception) {
                            // Ignored
                        } finally {
                            synchronized(clientSockets) {
                                val index = clientSockets.indexOf(newClient)
                                if (index != -1) {
                                    clientSockets.removeAt(index)
                                    outputStreams.removeAt(index)
                                    _connectedClients.value = clientSockets.mapNotNull { it.inetAddress?.hostAddress }
                                }
                            }
                            newClient.close()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TcpServer", "Erro no servidor: ${e.message}")
        }
    }

    init {
        // Heartbeat to clear ghost connections
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                kotlinx.coroutines.delay(5000)
                if (isRunning) {
                    sendCommand("CMD_PING")
                }
            }
        }
    }

    suspend fun sendCommandAndImage(command: String, imageBytes: ByteArray) = withContext(Dispatchers.IO) {
        synchronized(clientSockets) {
            val iterOut = outputStreams.iterator()
            val iterSock = clientSockets.iterator()
            while (iterOut.hasNext() && iterSock.hasNext()) {
                val out = iterOut.next()
                val sock = iterSock.next()
                try {
                    out.writeUTF(command)
                    out.writeInt(imageBytes.size)
                    out.write(imageBytes)
                    out.flush()
                } catch (e: Exception) {
                    Log.e("TcpServer", "Erro ao enviar imagem: ${e.message}")
                    try { sock.close() } catch (ignored: Exception) {}
                    iterOut.remove()
                    iterSock.remove()
                }
            }
            _connectedClients.value = clientSockets.mapNotNull { it.inetAddress?.hostAddress }
        }
    }
    
    suspend fun sendCommandAndText(command: String, text: String) = withContext(Dispatchers.IO) {
        synchronized(clientSockets) {
            val iterOut = outputStreams.iterator()
            val iterSock = clientSockets.iterator()
            while (iterOut.hasNext() && iterSock.hasNext()) {
                val out = iterOut.next()
                val sock = iterSock.next()
                try {
                    out.writeUTF(command)
                    out.writeUTF(text)
                    out.flush()
                } catch (e: Exception) {
                    Log.e("TcpServer", "Erro ao enviar texto: ${e.message}")
                    try { sock.close() } catch (ignored: Exception) {}
                    iterOut.remove()
                    iterSock.remove()
                }
            }
            _connectedClients.value = clientSockets.mapNotNull { it.inetAddress?.hostAddress }
        }
    }

    suspend fun sendCommand(command: String) = withContext(Dispatchers.IO) {
        synchronized(clientSockets) {
            val iterOut = outputStreams.iterator()
            val iterSock = clientSockets.iterator()
            while (iterOut.hasNext() && iterSock.hasNext()) {
                val out = iterOut.next()
                val sock = iterSock.next()
                try {
                    out.writeUTF(command)
                    out.flush()
                } catch (e: Exception) {
                    Log.e("TcpServer", "Erro ao enviar comando: ${e.message}")
                    try { sock.close() } catch (ignored: Exception) {}
                    iterOut.remove()
                    iterSock.remove()
                }
            }
            _connectedClients.value = clientSockets.mapNotNull { it.inetAddress?.hostAddress }
        }
    }

    fun stopServer() {
        isRunning = false
        _isServerRunningState.value = false
        try {
            synchronized(clientSockets) {
                for (sock in clientSockets) {
                    try { sock.close() } catch (ignored: Exception) {}
                }
                clientSockets.clear()
                outputStreams.clear()
                _connectedClients.value = emptyList()
            }
            serverSocket?.close()
        } catch (e: Exception) {
             Log.e("TcpServer", "Erro ao parar servidor: ${e.message}")
        }
    }
}
