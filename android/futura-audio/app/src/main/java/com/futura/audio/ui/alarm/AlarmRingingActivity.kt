package com.futura.audio.ui.alarm

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.futura.audio.MainActivity
import com.futura.audio.alarm.AlarmCoordinator
import com.futura.audio.alarm.DiscreteAlarmState
import com.futura.audio.service.FuturaAlarmService
import com.futura.audio.ui.FuturaTheme

/**
 * Ringing screen opened by the Futura Deck quick settings tile. The tile starts the alarm before
 * launching this, so the screen always has an active alarm to show. It is shown over the lock
 * screen only through the window flags below, and it closes itself once the alarm stops.
 */
class AlarmRingingActivity : ComponentActivity() {

    private var sawFiring = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val source = intent?.getStringExtra(EXTRA_ALARM_TIME)
        val coordinator = AlarmCoordinator.getInstance()

        setContent {
            FuturaTheme {
                val state by coordinator.alarmState.collectAsStateWithLifecycle()
                LaunchedEffect(state) {
                    if (state is DiscreteAlarmState.Firing) {
                        sawFiring = true
                    } else if (sawFiring) {
                        finish()
                    }
                }
                RingingScreen(
                    ringing = state is DiscreteAlarmState.Firing,
                    source = source,
                    onDismiss = {
                        startService(
                            Intent(this, FuturaAlarmService::class.java)
                                .setAction(FuturaAlarmService.ACTION_DISMISS_ALARM),
                        )
                    },
                    onOpenApp = {
                        startActivity(
                            Intent(this, MainActivity::class.java).addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP,
                            ),
                        )
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        const val EXTRA_ALARM_TIME = "com.futura.audio.EXTRA_ALARM_TIME"
    }
}

@Composable
private fun RingingScreen(
    ringing: Boolean,
    source: String?,
    onDismiss: () -> Unit,
    onOpenApp: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(if (ringing) "Alarm is ringing" else "No alarm is ringing")
        if (source != null) {
            Text(source)
        }
        Spacer(Modifier.height(24.dp))
        if (ringing) {
            Button(onClick = onDismiss) { Text("Dismiss") }
            Spacer(Modifier.height(12.dp))
        }
        Button(onClick = onOpenApp) { Text("Open Futura Audio") }
    }
}
