package com.futura.audio.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.futura.audio.alarm.AlarmCoordinator
import com.futura.audio.alarm.DiscreteAlarmState
import com.futura.audio.data.security.PinState
import com.futura.audio.data.security.SecurityPreferences
import com.futura.audio.ui.FuturaApp
import com.futura.audio.ui.FuturaViewModel
import com.futura.audio.ui.NEXT_ALARM_LABEL
import com.futura.audio.ui.alarm.AlarmCamouflageScreen
import com.futura.audio.ui.alarm.PinSetupScreen
import kotlinx.coroutines.launch

object Routes {
    const val PIN_SETUP = "pin_setup"
    const val CAMOUFLAGE = "camouflage"
    const val MAIN = "main"
}

/**
 * Root navigation. Start route is chosen only after the stored PIN has been read,
 * so a configured user never lands on PIN setup.
 *
 * Flow:
 *  - no PIN saved  -> PIN setup -> camouflage
 *  - PIN saved     -> camouflage -> (correct PIN) -> main (existing [FuturaApp])
 *  - alarm fires   -> jump to main, where the existing alarm screen is shown
 */
@Composable
fun FuturaNavGraph(
    viewModel: FuturaViewModel,
    securityPreferences: SecurityPreferences,
) {
    val pinState by securityPreferences.pinState.collectAsStateWithLifecycle(initialValue = PinState.Loading)
    val alarmState by AlarmCoordinator.getInstance().alarmState.collectAsStateWithLifecycle()

    when (val state = pinState) {
        PinState.Loading -> Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
        )

        else -> {
            val startRoute = if (state is PinState.Set) Routes.CAMOUFLAGE else Routes.PIN_SETUP
            val navController = rememberNavController()
            val scope = rememberCoroutineScope()
            val storedHash = (state as? PinState.Set)?.hash.orEmpty()

            // Phase 1 alarm firing overrides the camouflage so the existing alarm screen shows.
            LaunchedEffect(alarmState) {
                if (alarmState is DiscreteAlarmState.Firing) {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            NavHost(navController = navController, startDestination = startRoute) {
                composable(Routes.PIN_SETUP) {
                    PinSetupScreen(
                        onPinCreated = { hash ->
                            scope.launch {
                                securityPreferences.savePinHash(hash)
                            }
                            navController.navigate(Routes.CAMOUFLAGE) {
                                popUpTo(Routes.PIN_SETUP) { inclusive = true }
                            }
                        },
                    )
                }

                composable(Routes.CAMOUFLAGE) {
                    AlarmCamouflageScreen(
                        correctPinHash = storedHash,
                        // Placeholder until the real next alarm time is wired from AlarmSchedulerImpl.
                        nextAlarmLabel = NEXT_ALARM_LABEL,
                        onUnlocked = {
                            navController.navigate(Routes.MAIN) {
                                popUpTo(Routes.CAMOUFLAGE) { inclusive = true }
                            }
                        },
                    )
                }

                composable(Routes.MAIN) {
                    FuturaApp(viewModel = viewModel)
                }
            }
        }
    }
}
