package com.futura.audio.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.futura.audio.MainActivity
import com.futura.audio.receiver.AlarmReceiver

sealed interface AlarmScheduleResult {
    data object Scheduled : AlarmScheduleResult
    data object ExactAlarmPermissionRequired : AlarmScheduleResult
    data object InvalidTriggerTime : AlarmScheduleResult
}

class AlarmSchedulerImpl(context: Context) {
    private val appContext = context.applicationContext
    private val deviceProtectedContext = appContext.createDeviceProtectedStorageContext()
    private val alarmManager =
        appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val preferences = deviceProtectedContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun scheduleExactAlarm(triggerTimeMillis: Long): AlarmScheduleResult {
        if (triggerTimeMillis <= System.currentTimeMillis()) {
            return AlarmScheduleResult.InvalidTriggerTime
        }
        preferences.edit().putLong(KEY_TRIGGER_TIME, triggerTimeMillis).commit()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            return AlarmScheduleResult.ExactAlarmPermissionRequired
        }

        val triggerIntent = Intent(appContext, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
        }
        val triggerPendingIntent = PendingIntent.getBroadcast(
            appContext,
            ALARM_REQUEST_CODE,
            triggerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val showIntent = Intent(appContext, MainActivity::class.java).apply {
            action = ACTION_OPEN_ALARM_SCREEN
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val showPendingIntent = PendingIntent.getActivity(
            appContext,
            SHOW_REQUEST_CODE,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerTimeMillis, showPendingIntent),
            triggerPendingIntent,
        )
        return AlarmScheduleResult.Scheduled
    }

    fun cancelAlarm() {
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            ALARM_REQUEST_CODE,
            Intent(appContext, AlarmReceiver::class.java).setAction(ACTION_TRIGGER_ALARM),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        preferences.edit().remove(KEY_TRIGGER_TIME).apply()
    }

    fun restoreSavedAlarm(): AlarmScheduleResult? {
        val savedTriggerTime = preferences.getLong(KEY_TRIGGER_TIME, NO_TRIGGER)
        if (savedTriggerTime == NO_TRIGGER) return null
        if (savedTriggerTime <= System.currentTimeMillis()) {
            preferences.edit().remove(KEY_TRIGGER_TIME).apply()
            return null
        }
        return scheduleExactAlarm(savedTriggerTime)
    }

    fun hasExactAlarmPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    companion object {
        const val ACTION_TRIGGER_ALARM = "com.futura.audio.ACTION_TRIGGER_ALARM"
        const val ACTION_OPEN_ALARM_SCREEN = "com.futura.audio.ACTION_OPEN_ALARM_SCREEN"

        private const val ALARM_REQUEST_CODE = 1001
        private const val SHOW_REQUEST_CODE = 1002
        private const val PREFERENCES_NAME = "futura_alarm_schedule"
        private const val KEY_TRIGGER_TIME = "next_trigger_at"
        private const val NO_TRIGGER = -1L
    }
}
