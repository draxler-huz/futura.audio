package com.futura.audio.ui

import com.futura.audio.ui.library.LibraryCatalog

const val DEVICE_START_SECONDS = 6 * 60 * 60 + 30 * 60
const val SNOOZE_DURATION_SECONDS = 9 * 60
const val NEXT_ALARM_LABEL = "07:00 AM"
const val AUDIO_OUTPUT_DEVICE = "CyberBuds Pro"
const val AUDIO_OUTPUT_MODE = "Lossless Active"

/** Playback order for the view model. The index is the track index used by selectTrack and loadTrackPcm. */
val ROULETTE_QUEUE: List<FuturaTrack> = LibraryCatalog.tracks.map {
    FuturaTrack(it.id, it.title, it.artist, it.durationSeconds, "FLAC 16/44.1")
}

data class FuturaTrack(
    val id: String,
    val title: String,
    val artist: String,
    val durationSeconds: Int,
    val format: String,
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
