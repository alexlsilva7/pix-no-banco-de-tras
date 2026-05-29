package com.example.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket

object TcpServer {
    private var serverSocket: ServerSocket? = null
    private val clientSockets = mutableListOf<Socket>()
    private val outputStreams = mutableListOf<DataOutputStream>()

    var isRunning = false
    
    private val _connectedClients = MutableStateFlow<List<String>>(emptyList())
    val connectedClients: StateFlow<List<String>> = _connectedClients

    private val _isServerRunningState = MutableStateFlow(false)
    val isServerRunningState: StateFlow<Boolean> = _isServerRunningState

    // 1. Escopo Dedicado: Sobrevive ao longo da vida do Singleton, 
    // mas permite cancelar as tarefas filhas a qualquer momento.
    private val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun startServer(port: Int = 8080) = withContext(Dispatchers.IO) {
        if (isRunning) return@withContext
        try {
            serverSocket = ServerSocket(port)
            isRunning = true
            _isServerRunningState.value = true
            Log.d("TcpServer", "Servidor iniciado na porta $port")

            // 2. Heartbeat (PING): Inicia apenas quando o servidor liga
            serverScope.launch {
                while (isActive && isRunning) {
                    delay(5000)
                    sendCommand("CMD_PING")
                }
            }

            while (isRunning) {
                val newClient = serverSocket?.accept()
                if (newClient != null) {
                    synchronized(clientSockets) {
                        clientSockets.add(newClient)
                        outputStreams.add(DataOutputStream(newClient.getOutputStream()))
                        _connectedClients.value = clientSockets.mapNotNull { it.inetAddress?.hostAddress }
                    }
                    Log.d("TcpServer", "Cliente conectado: ${newClient.inetAddress.hostAddress}")
                    
                    // 3. Listener de Desconexão rodando no escopo controlado
                    serverScope.launch {
                        try {
                            // Simple read to detect disconnect
                            newClient.getInputStream().read()
                        } catch (e: Exception) {
                            // Ignored (Client disconnected abruptly)
                        } finally {
                            synchronized(clientSockets) {
                                val index = clientSockets.indexOf(newClient)
                                if (index != -1) {
                                    clientSockets.removeAt(index)
                                    outputStreams.removeAt(index)
                                    _connectedClients.value = clientSockets.mapNotNull { it.inetAddress?.hostAddress }
                                }
                            }
                            try { newClient.close() } catch (ignored: Exception) {}
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TcpServer", "Erro no servidor: ${e.message}")
        } finally {
            // Se o loop principal (accept) for quebrado por alguma exceção na porta,
            // garantimos que tudo seja desligado corretamente.
            stopServer()
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
        if (!isRunning) return
        
        isRunning = false
        _isServerRunningState.value = false
        
        // 4. Mata imediatamente as coroutines filhas (o PING e os Listeners)
        // Isso evita que elas continuem consumindo CPU/Bateria em background
        serverScope.coroutineContext.cancelChildren()

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
            serverSocket = null
        } catch (e: Exception) {
             Log.e("TcpServer", "Erro ao parar servidor: ${e.message}")
        }
    }
}
