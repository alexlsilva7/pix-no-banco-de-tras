package com.example.utils

import android.content.Context
import android.content.ContextWrapper
import android.app.Activity
fun isAccessibilityServiceEnabled(context: Context, accessibilityService: Class<*>): Boolean {
    val expectedComponentName = android.content.ComponentName(context, accessibilityService)
    val enabledServicesSetting = android.provider.Settings.Secure.getString(
        context.contentResolver,
        android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServicesSetting)
    while (colonSplitter.hasNext()) {
        val componentNameString = colonSplitter.next()
        val enabledService = android.content.ComponentName.unflattenFromString(componentNameString)
        if (enabledService != null && enabledService == expectedComponentName) {
            return true
        }
    }
    return false
}

fun getLocalIpAddress(): String {
    try {
        val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            val addresses = networkInterface.inetAddresses
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                if (!address.isLoopbackAddress && address.hostAddress?.indexOf(':') == -1) {
                    return address.hostAddress ?: "0.0.0.0"
                }
            }
        }
    } catch (ex: java.net.SocketException) {
        ex.printStackTrace()
    }
    return "Desconhecido"
}


tailrec fun Context.getActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}


data class PixData(
    val name: String,
    val key: String,
    val city: String,
    val amount: String
)

fun parsePixPayload(payload: String): PixData {
    var name = "Desconhecido"
    var key = "Desconhecida"
    var city = "Desconhecida"
    var amount = ""
    
    try {
        var index = 0
        while (index < payload.length - 4) {
            val id = payload.substring(index, index + 2)
            val lengthStr = payload.substring(index + 2, index + 4)
            val length = lengthStr.toIntOrNull() ?: break
            if (index + 4 + length > payload.length) break
            val value = payload.substring(index + 4, index + 4 + length)
            
            when (id) {
                "26" -> {
                    var subIndex = 0
                    while (subIndex < value.length - 4) {
                        val subId = value.substring(subIndex, subIndex + 2)
                        val subLenStr = value.substring(subIndex + 2, subIndex + 4)
                        val subLen = subLenStr.toIntOrNull() ?: break
                        if (subIndex + 4 + subLen > value.length) break
                        val subValue = value.substring(subIndex + 4, subIndex + 4 + subLen)
                        if (subId == "01" || subId == "02" || subId == "03") {
                            if (!subValue.startsWith("br.gov")) {
                                key = subValue
                            }
                        }
                        subIndex += 4 + subLen
                    }
                }
                "59" -> name = value
                "60" -> city = value
                "54" -> amount = value
            }
            index += 4 + length
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    return PixData(name, key, city, amount)
}
