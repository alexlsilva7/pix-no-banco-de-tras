package com.alexlopes.pixdrive

import android.content.Context

enum class DeviceMode(val preferenceValue: String) {
    DRIVER("DRIVER"),
    PASSENGER_DISPLAY("PASSENGER_DISPLAY");

    companion object {
        fun fromPreference(value: String?): DeviceMode? =
            entries.firstOrNull { it.preferenceValue == value }
    }
}

object DeviceModePreferences {
    const val PREFERENCES_NAME = "PixPrefs"
    const val DEVICE_MODE_KEY = "DEVICE_MODE"

    fun get(context: Context): DeviceMode? =
        DeviceMode.fromPreference(
            context
                .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getString(DEVICE_MODE_KEY, null)
        )

    fun set(context: Context, mode: DeviceMode) {
        context
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(DEVICE_MODE_KEY, mode.preferenceValue)
            .apply()
    }

    fun clear(context: Context) {
        context
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(DEVICE_MODE_KEY)
            .apply()
    }
}
