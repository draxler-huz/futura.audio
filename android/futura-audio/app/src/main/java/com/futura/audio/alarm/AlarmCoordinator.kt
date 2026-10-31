package com.futura.audio.alarm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface DiscreteAlarmState {
    data object Armed : DiscreteAlarmState
    data class Firing(val triggerTime: Long, val snoozeCount: Int) : DiscreteAlarmState
    data class QueueActive(val activeTrackId: String, val isLossless: Boolean) : DiscreteAlarmState
    data object Disarmed : DiscreteAlarmState
}

class AlarmCoordinator private constructor() {
    private val lock = Any()
    private val _alarmState = MutableStateFlow<DiscreteAlarmState>(DiscreteAlarmState.Armed)
    val alarmState: StateFlow<DiscreteAlarmState> = _alarmState.asStateFlow()
    private var snoozeCount = 0

    fun onAlarmTriggered(triggerTime: Long) = synchronized(lock) {
        snoozeCount = 0
        _alarmState.value = DiscreteAlarmState.Firing(triggerTime, snoozeCount)
    }

    fun onSnoozeTriggered(trackId: String, isLossless: Boolean): DiscreteAlarmState.QueueActive =
        synchronized(lock) {
            snoozeCount += 1
            DiscreteAlarmState.QueueActive(trackId, isLossless).also {
                _alarmState.value = it
            }
        }

    fun onDismissTriggered() = synchronized(lock) {
        snoozeCount = 0
        _alarmState.value = DiscreteAlarmState.Disarmed
    }

    fun resetToArmed() = synchronized(lock) {
        snoozeCount = 0
        _alarmState.value = DiscreteAlarmState.Armed
    }

    companion object {
        @Volatile
        private var instance: AlarmCoordinator? = null

        fun getInstance(): AlarmCoordinator =
            instance ?: synchronized(this) {
                instance ?: AlarmCoordinator().also { instance = it }
            }
    }
}
