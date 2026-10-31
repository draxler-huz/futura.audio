package com.futura.audio.receiver

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.futura.audio.alarm.AlarmCoordinator
import com.futura.audio.alarm.AlarmSchedulerImpl
import com.futura.audio.service.FuturaAlarmService

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AlarmSchedulerImpl.ACTION_TRIGGER_ALARM -> {
                AlarmCoordinator.getInstance().onAlarmTriggered(System.currentTimeMillis())
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, FuturaAlarmService::class.java).setAction(
                        FuturaAlarmService.ACTION_START_ALARM,
                    ),
                )
            }

            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                AlarmSchedulerImpl(context).restoreSavedAlarm()
            }
        }
    }
}
