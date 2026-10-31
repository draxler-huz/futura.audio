package com.futura.audio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.futura.audio.ui.FuturaApp
import com.futura.audio.ui.NotificationPermissionGate
import com.futura.audio.ui.FuturaTheme
import com.futura.audio.ui.FuturaViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: FuturaViewModel by viewModels()

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
                    FuturaApp(viewModel)
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

    companion object {
        const val EXTRA_SNOOZE_FROM_NOTIFICATION =
            "com.futura.audio.extra.SNOOZE_FROM_NOTIFICATION"
    }
}
