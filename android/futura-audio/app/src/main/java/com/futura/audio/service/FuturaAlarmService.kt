package com.futura.audio.service

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.futura.audio.MainActivity
import com.futura.audio.R
import com.futura.audio.alarm.AlarmCoordinator
import com.futura.audio.alarm.AlarmScheduleResult
import com.futura.audio.alarm.AlarmSchedulerImpl
import com.futura.audio.receiver.AlarmReceiver

class FuturaAlarmService : Service() {
    private var ringtone: Ringtone? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_ALARM -> startAlarm()
            ACTION_SNOOZE_TO_PLAYER -> snoozeToPlayer()
            ACTION_DISMISS_ALARM -> dismissAlarm()
            else -> stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    private fun startAlarm() {
        promoteToForeground(buildNotification(alarmRinging = true))
        acquireWakeLock()
        requestAlarmAudioFocus()
        startAlarmRingtone()
    }

    private fun snoozeToPlayer() {
        stopAlarmRingtone()
        val snoozeAt = System.currentTimeMillis() + SNOOZE_MILLIS
        val result = AlarmSchedulerImpl(this).scheduleExactAlarm(snoozeAt)
        val requiresExactAlarmPermission =
            result == AlarmScheduleResult.ExactAlarmPermissionRequired
        val notification = buildNotification(
            alarmRinging = false,
            snoozeNeedsPermission = requiresExactAlarmPermission,
        )
        promoteToForeground(notification)
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (requiresExactAlarmPermission) {
            postNotificationIfAllowed(notification)
        }
        stopSelf()
    }

    private fun dismissAlarm() {
        stopAlarmRingtone()
        AlarmCoordinator.getInstance().onDismissTriggered()
        AlarmSchedulerImpl(this).cancelAlarm()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun promoteToForeground(notification: android.app.Notification) {
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } catch (_: SecurityException) {
            stopSelf()
        }
    }

    private fun buildNotification(
        alarmRinging: Boolean,
        snoozeNeedsPermission: Boolean = false,
    ): android.app.Notification {
        val openAlarmIntent = PendingIntent.getActivity(
            this,
            OPEN_ACTIVITY_REQUEST_CODE,
            Intent(this, MainActivity::class.java).apply {
                action = AlarmSchedulerImpl.ACTION_OPEN_ALARM_SCREEN
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val snoozeIntent = PendingIntent.getActivity(
            this,
            SNOOZE_REQUEST_CODE,
            Intent(this, MainActivity::class.java).apply {
                action = AlarmSchedulerImpl.ACTION_OPEN_ALARM_SCREEN
                putExtra(MainActivity.EXTRA_SNOOZE_FROM_NOTIFICATION, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val dismissIntent = PendingIntent.getService(
            this,
            DISMISS_REQUEST_CODE,
            Intent(this, FuturaAlarmService::class.java).setAction(ACTION_DISMISS_ALARM),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(
                when {
                    alarmRinging -> "Alarm is ringing"
                    snoozeNeedsPermission -> "Snooze set; exact-alarm access is required to re-ring"
                    else -> "Alarm snoozed"
                },
            )
            .setContentIntent(openAlarmIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(alarmRinging || !snoozeNeedsPermission)
            .setAutoCancel(false)

        if (alarmRinging) {
            builder
                .setFullScreenIntent(openAlarmIntent, true)
                .addAction(
                    android.R.drawable.ic_media_pause,
                    "Snooze",
                    snoozeIntent,
                )
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Dismiss",
                    dismissIntent,
                )
        } else if (snoozeNeedsPermission) {
            val settingsIntent = PendingIntent.getActivity(
                this,
                SETTINGS_REQUEST_CODE,
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = android.net.Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(
                android.R.drawable.ic_menu_manage,
                "Allow exact alarms",
                settingsIntent,
            )
        } else {
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Dismiss",
                dismissIntent,
            )
        }
        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.alarm_channel_description)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setSound(null, null)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun postNotificationIfAllowed(notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        try {
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Notification access is optional; an exact alarm can still be scheduled.
        }
    }

    private fun requestAlarmAudioFocus() {
        val manager = audioManager ?: return
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { }
            .build()
        audioFocusRequest = request
        manager.requestAudioFocus(request)
    }

    private fun startAlarmRingtone() {
        stopAlarmRingtone()
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return
        ringtone = RingtoneManager.getRingtone(this, uri)?.also {
            it.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                it.isLooping = true
            }
            it.play()
        }
    }

    private fun stopAlarmRingtone() {
        ringtone?.stop()
        ringtone = null
        val manager = audioManager
        val request = audioFocusRequest
        if (manager != null && request != null) {
            manager.abandonAudioFocusRequest(request)
        }
        audioFocusRequest = null
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "$packageName:FuturaAlarm",
        ).apply {
            setReferenceCounted(false)
            acquire(WAKE_LOCK_TIMEOUT_MILLIS)
        }
    }

    override fun onDestroy() {
        stopAlarmRingtone()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_ALARM = "com.futura.audio.service.START_ALARM"
        const val ACTION_SNOOZE_TO_PLAYER = "com.futura.audio.service.SNOOZE_TO_PLAYER"
        const val ACTION_DISMISS_ALARM = "com.futura.audio.service.DISMISS_ALARM"

        private const val CHANNEL_ID = "futura_alarm_channel"
        private const val NOTIFICATION_ID = 7001
        private const val OPEN_ACTIVITY_REQUEST_CODE = 7002
        private const val SNOOZE_REQUEST_CODE = 7003
        private const val DISMISS_REQUEST_CODE = 7004
        private const val SETTINGS_REQUEST_CODE = 7005
        private const val SNOOZE_MILLIS = 9 * 60 * 1000L
        private const val WAKE_LOCK_TIMEOUT_MILLIS = 15 * 60 * 1000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, FuturaAlarmService::class.java).setAction(ACTION_START_ALARM),
            )
        }
    }
}
