package com.futura.audio.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import com.futura.audio.alarm.AlarmCoordinator
import com.futura.audio.alarm.DiscreteAlarmState
import com.futura.audio.ui.alarm.AlarmRingingActivity

/**
 * "Futura Deck" quick settings tile. Tapping it starts the alarm if it is not already ringing,
 * then opens [AlarmRingingActivity]. The activity shows over the lock screen.
 */
class FuturaQuickSettingsService : TileService() {

    @Suppress("DEPRECATION")
    override fun onClick() {
        super.onClick()
        val coordinator = AlarmCoordinator.getInstance()
        if (coordinator.alarmState.value !is DiscreteAlarmState.Firing) {
            coordinator.onAlarmTriggered(System.currentTimeMillis())
            FuturaAlarmService.start(this)
        }

        com.futura.audio.data.device.DeviceStateStore(applicationContext)
            .putString("tile_last_source", "QS_TILE")

        val intent = Intent(this, AlarmRingingActivity::class.java).apply {
            putExtra(AlarmRingingActivity.EXTRA_ALARM_TIME, "QS PANEL // ON-DEMAND")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending = PendingIntent.getActivity(
                this,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            startActivityAndCollapse(pending)
        } else {
            startActivityAndCollapse(intent)
        }
    }

    companion object {
        private const val REQUEST_CODE = 7010
    }
}
