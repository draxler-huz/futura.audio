package com.futura.audio

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.futura.audio.alarm.AlarmCoordinator
import com.futura.audio.alarm.DiscreteAlarmState
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.futura.audio.data.security.SecurityPreferences
import com.futura.audio.ui.NotificationPermissionGate
import com.futura.audio.ui.FuturaTheme
import com.futura.audio.ui.FuturaViewModel
import com.futura.audio.ui.navigation.FuturaNavGraph

class MainActivity : ComponentActivity() {
    private val viewModel: FuturaViewModel by viewModels()
    private val securityPreferences by lazy { SecurityPreferences(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        setContent {
            FuturaTheme {
                Box {
                    AlarmWindowEffect(activity = this@MainActivity)
                    FuturaNavGraph(
                        viewModel = viewModel,
                        securityPreferences = securityPreferences,
                    )
                    NotificationPermissionGate(
                        isAlarmLaunch = intent?.action ==
                            com.futura.audio.alarm.AlarmSchedulerImpl.ACTION_OPEN_ALARM_SCREEN,
                    )
                }
            }
        }

        handleNotificationIntent(intent)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: android.content.Intent?) {
        if (intent?.getBooleanExtra(EXTRA_SNOOZE_FROM_NOTIFICATION, false) == true) {
            intent.removeExtra(EXTRA_SNOOZE_FROM_NOTIFICATION)
            viewModel.snooze()
        }
    }

    /**
     * Shows this activity over the lock screen and wakes the display only while an alarm is
     * firing. Outside an alarm the window flags are cleared, so the app never appears over the
     * lock screen by itself. The full-screen alarm notification already launches this activity.
     */
    @Composable
    private fun AlarmWindowEffect(activity: ComponentActivity) {
        val alarmState by AlarmCoordinator.getInstance().alarmState.collectAsStateWithLifecycle()
        val ringing = alarmState is DiscreteAlarmState.Firing
        LaunchedEffect(ringing) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                activity.setShowWhenLocked(ringing)
                activity.setTurnScreenOn(ringing)
            } else {
                val flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                if (ringing) activity.window.addFlags(flags) else activity.window.clearFlags(flags)
            }
        }
    }

    companion object {
        const val EXTRA_SNOOZE_FROM_NOTIFICATION =
            "com.futura.audio.extra.SNOOZE_FROM_NOTIFICATION"
    }
}
