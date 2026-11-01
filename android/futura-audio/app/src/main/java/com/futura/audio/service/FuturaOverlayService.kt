package com.futura.audio.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.TextView
import com.futura.audio.MainActivity
import com.futura.audio.R

/**
 * Foreground service that shows one small draggable pill over other apps.
 * Tap opens MainActivity (does not start alarm playback). Drag moves the pill.
 * No timers, no polling.
 */
class FuturaOverlayService : Service() {
    private var wm: WindowManager? = null
    private var pill: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIF_ID, buildNotification())
        if (pill == null) addPill()
        return START_NOT_STICKY
    }

    private fun addPill() {
        val manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val d = resources.displayMetrics.density

        val view = TextView(this).apply {
            text = "Futura"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 13f
            val h = (14 * d).toInt()
            val v = (8 * d).toInt()
            setPadding(h, v, h, v)
            background = GradientDrawable().apply {
                cornerRadius = 100 * d
                setColor(0x66202030)                        // translucent fill
                setStroke((1 * d).toInt(), 0x55FFFFFF)      // light border
            }
        }

        val type = if (Build.VERSION.SDK_INT >= 26)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = (200 * d).toInt()
        }

        val slop = ViewConfiguration.get(this).scaledTouchSlop
        var downX = 0f; var downY = 0f
        var startX = 0; var startY = 0
        var dragged = false

        view.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY
                    startX = lp.x; startY = lp.y
                    dragged = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (!dragged && dx * dx + dy * dy > slop * slop) dragged = true
                    if (dragged) {
                        lp.x = startX + dx.toInt()
                        lp.y = startY + dy.toInt()
                        manager.updateViewLayout(view, lp)
                    }
                }
                MotionEvent.ACTION_UP -> if (!dragged) openApp()
            }
            true
        }

        manager.addView(view, lp)
        wm = manager
        pill = view
    }

    private fun openApp() {
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        )
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Floating button", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val stop = PendingIntent.getService(
            this, 0,
            Intent(this, FuturaOverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL_ID)
        else @Suppress("DEPRECATION") Notification.Builder(this)
        return builder
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Futura floating button")
            .addAction(Notification.Action.Builder(android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_alarm), "Stop", stop).build())
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        try { pill?.let { wm?.removeView(it) } } catch (_: Exception) {}
        pill = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.futura.audio.OVERLAY_STOP"
        private const val CHANNEL_ID = "futura_overlay"
        private const val NOTIF_ID = 4201
    }
}
