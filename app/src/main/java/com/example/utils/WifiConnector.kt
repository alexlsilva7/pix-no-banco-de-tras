package com.example.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.util.Log

object WifiConnector {
    fun connectToWifi(context: Context, ssid: String, pass: String, onConnected: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (!wifiManager.isWifiEnabled) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = true
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(pass)
                .build()

            val networkRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(wifiNetworkSpecifier)
                .build()

            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    connectivityManager.bindProcessToNetwork(network)
                    Log.d("WifiConnector", "Conectado com sucesso ao Wi-Fi: $ssid")
                    onConnected()
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    Log.e("WifiConnector", "Falha ao conectar no Wi-Fi: $ssid")
                    onError("Dispositivo recusou ou não encontrou a rede.")
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    connectivityManager.bindProcessToNetwork(null)
                }
            }

            try {
                connectivityManager.requestNetwork(networkRequest, networkCallback)
            } catch (e: Exception) {
                Log.e("WifiConnector", "Erro ao requisitar rede: ${e.message}")
                onError(e.message ?: "Erro desconhecido")
            }
        } else {
            @Suppress("DEPRECATION")
            val wifiConfig = WifiConfiguration().apply {
                SSID = "\"$ssid\""
                preSharedKey = "\"$pass\""
            }
            @Suppress("DEPRECATION")
            val netId = wifiManager.addNetwork(wifiConfig)
            if (netId != -1) {
                @Suppress("DEPRECATION")
                wifiManager.disconnect()
                @Suppress("DEPRECATION")
                wifiManager.enableNetwork(netId, true)
                @Suppress("DEPRECATION")
                wifiManager.reconnect()
                Log.d("WifiConnector", "Conectado ao Wi-Fi legado: $ssid")
                onConnected()
            } else {
                Log.e("WifiConnector", "Erro ao adicionar rede legado: $ssid")
                onError("Falha ao adicionar rede Wi-Fi.")
            }
        }
    }
}
