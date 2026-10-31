package com.futura.audio.accessibility

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.futura.audio.alarm.AlarmCoordinator
import com.futura.audio.alarm.DiscreteAlarmState
import com.futura.audio.service.FuturaAlarmService

/**
 * On-demand alarm trigger, with two entry points:
 *  - the accessibility button in the navigation bar, from any screen
 *  - three quick Volume Up presses within [WINDOW_MS]
 *
 * Both call [ringNow], which uses the same path as a scheduled alarm. Volume keys are never
 * consumed. Window content retrieval is off, so the service cannot read what is on screen.
 */
class FuturaAccessibilityService : AccessibilityService() {

    private val recentPresses = ArrayDeque<Long>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        accessibilityButtonController.registerAccessibilityButtonCallback(
            object : AccessibilityButtonController.AccessibilityButtonCallback() {
                override fun onClicked(controller: AccessibilityButtonController) {
                    ringNow()
                }
            }
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used. Both triggers arrive as button callbacks or key events.
    }

    override fun onInterrupt() {
        recentPresses.clear()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_UP) return false
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount > 0) return false

        val now = event.eventTime
        recentPresses.addLast(now)
        while (recentPresses.isNotEmpty() && now - recentPresses.first() > WINDOW_MS) {
            recentPresses.removeFirst()
        }
        if (recentPresses.size >= REQUIRED_PRESSES) {
            recentPresses.clear()
            ringNow()
        }
        return false
    }

    private fun ringNow() {
        if (AlarmCoordinator.getInstance().alarmState.value is DiscreteAlarmState.Firing) return
        AlarmCoordinator.getInstance().onAlarmTriggered(System.currentTimeMillis())
        FuturaAlarmService.start(this)
    }

    companion object {
        const val REQUIRED_PRESSES = 3
        const val WINDOW_MS = 1_500L
    }
}