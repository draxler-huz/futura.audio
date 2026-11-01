package com.futura.audio.data.device

import android.content.Context
import android.content.SharedPreferences

/**
 * Small key-value store in device-protected storage, readable before first unlock.
 * Never put credentials, the PIN, or library data here.
 */
class DeviceStateStore(context: Context) {
    private val prefs: SharedPreferences? = try {
        context.createDeviceProtectedStorageContext()
            .getSharedPreferences("futura_device_state", Context.MODE_PRIVATE)
    } catch (e: Exception) {
        null
    }

    fun getString(key: String, default: String?): String? = try {
        prefs?.getString(key, default) ?: default
    } catch (e: Exception) {
        default
    }

    fun putString(key: String, value: String) {
        try {
            prefs?.edit()?.putString(key, value)?.apply()
        } catch (e: Exception) {
            // best effort only
        }
    }
}
