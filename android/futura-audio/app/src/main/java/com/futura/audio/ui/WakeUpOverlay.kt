package com.futura.audio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WakeUpOverlay(
    deviceSeconds: Int,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(FuturaColors.OledBlack),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = (maxHeight * 0.38f - 144.dp).coerceAtLeast(0.dp))
                .size(288.dp)
                .blur(48.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            FuturaColors.Neon.copy(alpha = 0.10f),
                            FuturaColors.Neon.copy(alpha = 0f),
                        ),
                    ),
                    CircleShape,
                ),
        )

        Column(modifier = Modifier.fillMaxSize()) {
            AlarmStatusBar()
            TimeSurface(
                deviceSeconds = deviceSeconds,
                modifier = Modifier.weight(1f),
            )
            ActionDock(
                onSnooze = onSnooze,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun AlarmStatusBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusPill(
            modifier = Modifier.weight(1f),
            icon = {
                Icon(
                    Icons.Outlined.Alarm,
                    contentDescription = null,
                    tint = FuturaColors.Neon,
                    modifier = Modifier.size(16.dp),
                )
            },
            title = "ALARM ARMED //",
            value = NEXT_ALARM_LABEL,
            accent = FuturaColors.Neon,
        )
        Spacer(Modifier.size(8.dp))
        StatusPill(
            modifier = Modifier.weight(1f),
            icon = {
                Icon(
                    Icons.Filled.Bluetooth,
                    contentDescription = null,
                    tint = FuturaColors.Cyan,
                    modifier = Modifier.size(16.dp),
                )
            },
            title = "BT: $AUDIO_OUTPUT_DEVICE",
            value = "• ${AUDIO_OUTPUT_MODE.uppercase()}",
            accent = FuturaColors.Cyan,
            alignEnd = true,
        )
    }
}

@Composable
private fun StatusPill(
    modifier: Modifier,
    icon: @Composable () -> Unit,
    title: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    alignEnd: Boolean = false,
) {
    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.30f),
                shape = RoundedCornerShape(16.dp),
            ),
        shape = RoundedCornerShape(16.dp),
        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.03f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!alignEnd) {
                icon()
                Spacer(Modifier.size(8.dp))
            }
            Column(
                modifier = Modifier.weight(1f, fill = false),
                horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
            ) {
                Text(
                    text = title,
                    color = accent.copy(alpha = 0.82f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = if (alignEnd) 10.sp else 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.7.sp,
                    maxLines = 1,
                )
                Text(
                    text = value,
                    color = accent,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = if (alignEnd) 0.4.sp else 1.sp,
                    maxLines = 1,
                )
            }
            if (alignEnd) {
                Spacer(Modifier.size(8.dp))
                icon()
            }
        }
    }
}

@Composable
private fun TimeSurface(deviceSeconds: Int, modifier: Modifier = Modifier) {
    val clock = splitClock(deviceSeconds)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "WAKE CYCLE 01",
            color = FuturaColors.Muted,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            letterSpacing = 4.4.sp,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = clock.hhmm,
            color = FuturaColors.Neon,
            fontFamily = FontFamily.Monospace,
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-3).sp,
            lineHeight = 76.sp,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displayLarge.copy(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = FuturaColors.Neon.copy(alpha = 0.45f),
                    blurRadius = 24f,
                ),
            ),
        )
        Row(
            modifier = Modifier.padding(top = 12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = clock.meridiem,
                color = FuturaColors.Neon.copy(alpha = 0.8f),
                fontFamily = FontFamily.Monospace,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp,
            )
            Text(
                text = ":${clock.seconds}",
                color = FuturaColors.Muted,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
            )
        }
        Row(
            modifier = Modifier.padding(top = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .background(FuturaColors.Cyan, CircleShape),
            )
            Text(
                text = "ROULETTE QUEUE READY",
                color = FuturaColors.Cyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 3.6.sp,
                style = MaterialTheme.typography.labelLarge.copy(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = FuturaColors.Cyan.copy(alpha = 0.45f),
                        blurRadius = 16f,
                    ),
                ),
            )
        }
    }
}

@Composable
private fun ActionDock(
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(
            onClick = onSnooze,
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .border(1.dp, FuturaColors.Neon.copy(alpha = 0.55f), CircleShape),
            shape = CircleShape,
            color = FuturaColors.Neon.copy(alpha = 0.10f),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "SNOOZE",
                    color = FuturaColors.Neon,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.8.sp,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = FuturaColors.Neon.copy(alpha = 0.45f),
                            blurRadius = 18f,
                        ),
                    ),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = null,
                        tint = FuturaColors.Neon.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "START ROULETTE QUEUE",
                        color = FuturaColors.Neon.copy(alpha = 0.8f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        letterSpacing = 2.2.sp,
                    )
                }
            }
        }
        SwipeToDismiss(
            label = "DISMISS ALARM & LOCK DECK",
            onComplete = onDismiss,
        )
    }
}
