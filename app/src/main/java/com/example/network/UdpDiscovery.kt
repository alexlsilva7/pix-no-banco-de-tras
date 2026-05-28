package com.example.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object UdpDiscovery {
    private const val UDP_PORT = 8888
    private const val DISCOVERY_MESSAGE = "PIX_SERVER_DISCOVERY"
    private const val RESPONSE_MESSAGE = "PIX_SERVER_HERE"

    var isServerListening = false
    var isClientListening = false

    // Chamado pelo Motorista (Servidor)
    suspend fun startDiscoveryServer() = withContext(Dispatchers.IO) {
        if (isServerListening) return@withContext
        isServerListening = true
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket(UDP_PORT)
            socket.broadcast = true
            Log.d("UdpDiscovery", "Servidor UDP escutando na porta $UDP_PORT")

            val buffer = ByteArray(256)
            while (isServerListening) {
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)
                val message = String(packet.data, 0, packet.length)

                if (message == DISCOVERY_MESSAGE) {
                    val responseData = RESPONSE_MESSAGE.toByteArray()
                    val responsePacket = DatagramPacket(
                        responseData, responseData.size,
                        packet.address, packet.port
                    )
                    socket.send(responsePacket)
                    Log.d("UdpDiscovery", "Respondido para ${packet.address.hostAddress}")
                }
            }
        } catch (e: Exception) {
            Log.e("UdpDiscovery", "Erro no servidor UDP: ${e.message}")
        } finally {
            socket?.close()
            isServerListening = false
        }
    }

    fun stopDiscoveryServer() {
        isServerListening = false
    }

    // Chamado pelo Passageiro (Cliente) para encontrar o servidor
    suspend fun discoverServerIp(): String? = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.broadcast = true
            socket.soTimeout = 3000 // 3 segundos de timeout

            val requestData = DISCOVERY_MESSAGE.toByteArray()
            // Tenta enviar para o endereço de broadcast da sub-rede local, ou genérico como fallback
            val broadcastAddress = getBroadcastAddress() ?: InetAddress.getByName("255.255.255.255")
            val requestPacket = DatagramPacket(
                requestData, requestData.size,
                broadcastAddress, UDP_PORT
            )
            socket.send(requestPacket)
            Log.d("UdpDiscovery", "Broadcast UDP enviado.")

            val buffer = ByteArray(256)
            val responsePacket = DatagramPacket(buffer, buffer.size)
            
            // Aguarda resposta
            socket.receive(responsePacket)
            val message = String(responsePacket.data, 0, responsePacket.length)

            if (message == RESPONSE_MESSAGE) {
                val serverIp = responsePacket.address.hostAddress
                Log.d("UdpDiscovery", "Servidor encontrado: $serverIp")
                return@withContext serverIp
            }
        } catch (e: Exception) {
            Log.e("UdpDiscovery", "Erro ao buscar servidor UDP: ${e.message}")
        } finally {
            socket?.close()
        }
        return@withContext null
    }

    private fun getBroadcastAddress(): InetAddress? {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue

                for (interfaceAddress in networkInterface.interfaceAddresses) {
                    val broadcast = interfaceAddress.broadcast
                    if (broadcast != null) {
                        return broadcast
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("UdpDiscovery", "Erro ao obter endereço de broadcast: ${e.message}")
        }
        return null
    }
}
