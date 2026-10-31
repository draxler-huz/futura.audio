package com.futura.audio.ui

enum class FuturaScreen {
    ALARM,
    DASHBOARD,
    LOCKED,
}

data class FuturaUiState(
    val screen: FuturaScreen = FuturaScreen.ALARM,
    val elapsedSeconds: Int = 0,
    val snoozeEndsAt: Int? = null,
    val trackIndex: Int = 0,
    val trackElapsedSeconds: Int = 0,
    val playing: Boolean = false,
    val sliding: Boolean = false,
) {
    val deviceSeconds: Int
        get() = DEVICE_START_SECONDS + elapsedSeconds

    val snoozeRemainingSeconds: Int
        get() = (snoozeEndsAt?.minus(elapsedSeconds) ?: 0).coerceAtLeast(0)
}
