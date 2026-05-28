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
    
    private val _connectedClientsCount = kotlinx.coroutines.flow.MutableStateFlow(0)
    val connectedClientsCount: kotlinx.coroutines.flow.StateFlow<Int> = _connectedClientsCount

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
                        _connectedClientsCount.value = clientSockets.size
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
                                    _connectedClientsCount.value = clientSockets.size
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
            _connectedClientsCount.value = clientSockets.size
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
            _connectedClientsCount.value = clientSockets.size
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
            _connectedClientsCount.value = clientSockets.size
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
                _connectedClientsCount.value = 0
            }
            serverSocket?.close()
        } catch (e: Exception) {
             Log.e("TcpServer", "Erro ao parar servidor: ${e.message}")
        }
    }
}
