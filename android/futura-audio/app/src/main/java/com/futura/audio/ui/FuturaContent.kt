package com.futura.audio.ui

const val DEVICE_START_SECONDS = 6 * 60 * 60 + 30 * 60
const val SNOOZE_DURATION_SECONDS = 9 * 60
const val NEXT_ALARM_LABEL = "07:00 AM"
const val AUDIO_OUTPUT_DEVICE = "CyberBuds Pro"
const val AUDIO_OUTPUT_MODE = "Lossless Active"

data class FuturaTrack(
    val id: String,
    val title: String,
    val artist: String,
    val durationSeconds: Int,
    val format: String,
)

val ROULETTE_QUEUE = listOf(
    FuturaTrack("t1", "Neon Sunrise Protocol", "Halcyon Drift", 248, "FLAC 24/96"),
    FuturaTrack("t2", "Low Orbit Lullaby", "Kessler Field", 212, "FLAC 24/48"),
    FuturaTrack("t3", "Chrome Meridian", "Vanta Circuit", 301, "ALAC 24/96"),
    FuturaTrack("t4", "Quiet Signal", "Mira Okafor", 187, "FLAC 16/44"),
    FuturaTrack("t5", "Daybreak Handshake", "Sublevel 9", 264, "FLAC 24/192"),
)

data class SplitClock(
    val hhmm: String,
    val seconds: String,
    val meridiem: String,
)

fun splitClock(totalSeconds: Int): SplitClock {
    val secondsOfDay = ((totalSeconds % 86_400) + 86_400) % 86_400
    val hour24 = secondsOfDay / 3_600
    val minutes = (secondsOfDay % 3_600) / 60
    val seconds = secondsOfDay % 60
    val hour12 = when (val hour = hour24 % 12) {
        0 -> 12
        else -> hour
    }
    return SplitClock(
        hhmm = "%02d:%02d".format(hour12, minutes),
        seconds = "%02d".format(seconds),
        meridiem = if (hour24 < 12) "AM" else "PM",
    )
}

fun formatDuration(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    return "${safeSeconds / 60}:${(safeSeconds % 60).toString().padStart(2, '0')}"
}
