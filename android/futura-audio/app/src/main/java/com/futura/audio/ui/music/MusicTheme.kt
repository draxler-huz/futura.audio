package com.futura.audio.ui.music

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/** Design tokens from the design 1.1 prototype (globals.css :root). */
object Deck {
    val Background = Color(0xFF030507)
    val Card = Color(0xFF0A0F14)
    val Foreground = Color(0xFFE6EDF3)
    val Muted = Color(0xFF7D8A99)
    val Primary = Color(0xFF00E5FF)
    val PrimaryForeground = Color(0xFF001317)
    val Signal = Color(0xFF00FF66)
    val SignalForeground = Color(0xFF00170A)
    val Destructive = Color(0xFFFF4D6D)
    val Border = Color(0x14FFFFFF)
    val Accent = Color(0xFF142029)
    val Mono = FontFamily.Monospace
    val Sans = FontFamily.SansSerif
}

enum class LoopMode { OFF, ALL, ONE }

enum class DeckTab(val label: String) {
    QUEUE("UP NEXT QUEUE"),
    LYRICS("SYNCED LYRICS"),
    RELATED("RELATED & MOOD"),
}
