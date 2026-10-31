package com.futura.audio.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Typography

object FuturaColors {
    val OledBlack = Color(0xFF000000)
    val Neon = Color(0xFF00FF66)
    val Cyan = Color(0xFF00E5FF)
    val Foreground = Color(0xFFF0F3F1)
    val Muted = Color(0xFF84918A)
}

@Composable
fun FuturaTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = FuturaColors.Neon,
        secondary = FuturaColors.Cyan,
        background = FuturaColors.OledBlack,
        surface = FuturaColors.OledBlack,
        onPrimary = FuturaColors.OledBlack,
        onSecondary = FuturaColors.OledBlack,
        onBackground = FuturaColors.Foreground,
        onSurface = FuturaColors.Foreground,
    )

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content,
    )
}
