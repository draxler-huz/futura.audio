package com.futura.audio.outside

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.UserManager
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Diagnostic only. Runs at boot, before the first unlock, and records in device-protected storage:
 * whether the user is unlocked, whether a network is available, and the result of one HEAD request.
 * It plays nothing and stores no personal data. The log is shown by [OutsideTestActivity].
 */
class BootTestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pending = goAsync()
        EXECUTOR.execute {
            val line = try {
                val cm = context.getSystemService(ConnectivityManager::class.java)
                val caps = cm.getNetworkCapabilities(cm.activeNetwork)
                val hasNet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                val unlocked = context.getSystemService(UserManager::class.java).isUserUnlocked
                val http = if (hasNet) probe() else "no network"
                "${now()} action=$action unlocked=$unlocked net=$hasNet http=$http"
            } catch (e: Exception) {
                "${now()} action=$action error=${e.javaClass.simpleName}: ${e.message}"
            }
            try {
                val prefs = context.createDeviceProtectedStorageContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val old = prefs.getString(KEY_LOG, "") ?: ""
                prefs.edit().putString(KEY_LOG, (line + "\n" + old).take(4000)).apply()
            } finally {
                pending.finish()
            }
        }
    }

    private fun probe(): String {
        return try {
            val conn = URL("https://www.youtube.com/").openConnection() as HttpURLConnection
            conn.requestMethod = "HEAD"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            val code = conn.responseCode
            conn.disconnect()
            "HTTP $code"
        } catch (e: Exception) {
            "failed ${e.javaClass.simpleName}"
        }
    }

    private fun now(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

    companion object {
        const val PREFS = "outside_test"
        const val KEY_LOG = "log"
        const val KEY_LINK = "link"
        private val EXECUTOR = Executors.newSingleThreadExecutor()
    }
}
