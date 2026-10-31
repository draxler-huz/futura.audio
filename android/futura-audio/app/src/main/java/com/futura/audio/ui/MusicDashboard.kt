package com.futura.audio.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val EQ_BARS = listOf(0.9f, 0.5f, 0.75f, 0.35f, 1f, 0.6f, 0.8f, 0.45f, 0.95f, 0.55f, 0.7f, 0.4f)

@Composable
fun MusicDashboard(
    state: FuturaUiState,
    onTogglePlay: () -> Unit,
    onSkip: (Int) -> Unit,
    onWakeNow: () -> Unit,
) {
    val trackIndex = state.trackIndex.coerceIn(ROULETTE_QUEUE.indices)
    val track = ROULETTE_QUEUE[trackIndex]
    val upNext = (1..3).map { offset ->
        ROULETTE_QUEUE[(trackIndex + offset) % ROULETTE_QUEUE.size]
    }
    val clock = splitClock(state.deviceSeconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FuturaColors.OledBlack)
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "FUTURA",
                    color = FuturaColors.Foreground,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.6.sp,
                )
                Text(
                    text = "//",
                    color = FuturaColors.Neon,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                )
                Text(
                    text = "DECK",
                    color = FuturaColors.Foreground,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.6.sp,
                )
            }
            Text(
                text = "${clock.hhmm} ${clock.meridiem}",
                color = FuturaColors.Muted,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                letterSpacing = 0.3.sp,
            )
        }

        SessionBanner(
            snoozeRemaining = state.snoozeRemainingSeconds,
            onWakeNow = onWakeNow,
        )

        NowPlayingCard(
            track = track,
            elapsedSeconds = state.trackElapsedSeconds,
            playing = state.playing,
        )

        Transport(
            playing = state.playing,
            onTogglePlay = onTogglePlay,
            onSkip = onSkip,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Filled.Shuffle,
                    contentDescription = null,
                    tint = FuturaColors.Muted,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = "ROULETTE QUEUE",
                    color = FuturaColors.Muted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 3.sp,
                )
            }
            upNext.forEachIndexed { index, nextTrack ->
                QueueTrackCard(
                    track = nextTrack,
                    order = index + 1,
                )
            }
        }
    }
}

@Composable
private fun SessionBanner(
    snoozeRemaining: Int,
    onWakeNow: () -> Unit,
) {
    val pulseTransition = rememberInfiniteTransition(label = "alarm pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alarm pulse intensity",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                FuturaColors.Cyan.copy(alpha = 0.55f),
                RoundedCornerShape(16.dp),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(FuturaColors.Cyan.copy(alpha = 0.05f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Filled.Alarm,
            contentDescription = null,
            tint = FuturaColors.Cyan.copy(alpha = pulse),
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "ALARM SESSION LIVE",
                color = FuturaColors.Cyan.copy(alpha = 0.75f),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 2.5.sp,
            )
            Text(
                text = "Re-ring in ${formatDuration(snoozeRemaining)}",
                color = FuturaColors.Cyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                letterSpacing = 0.25.sp,
            )
        }
        Surface(
            onClick = onWakeNow,
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier.border(
                1.dp,
                FuturaColors.Cyan.copy(alpha = 0.4f),
                CircleShape,
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.WbSunny,
                    contentDescription = null,
                    tint = FuturaColors.Cyan,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "WAKE NOW",
                    color = FuturaColors.Cyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}

@Composable
private fun NowPlayingCard(
    track: FuturaTrack,
    elapsedSeconds: Int,
    playing: Boolean,
) {
    val elapsed = elapsedSeconds.coerceIn(0, track.durationSeconds)
    val progress = elapsed.toFloat() / track.durationSeconds.toFloat()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                FuturaColors.Neon.copy(alpha = 0.2f),
                RoundedCornerShape(24.dp),
            )
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.02f))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        EqualizerBars(playing = playing)
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = track.format,
                color = FuturaColors.Neon.copy(alpha = 0.75f),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 2.5.sp,
            )
            Text(
                text = track.title,
                color = FuturaColors.Foreground,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                color = FuturaColors.Muted,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = FuturaColors.Neon,
                trackColor = Color.White.copy(alpha = 0.10f),
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatDuration(elapsed),
                    color = FuturaColors.Muted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                )
                Text(
                    text = "-${formatDuration(track.durationSeconds - elapsed)}",
                    color = FuturaColors.Muted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun EqualizerBars(playing: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        EQ_BARS.forEachIndexed { index, baseHeight ->
            val transition = rememberInfiniteTransition(label = "equalizer bar $index")
            val pulse by transition.animateFloat(
                initialValue = 0.45f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 450 + index * 43),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "equalizer amplitude $index",
            )
            val scale = if (playing) pulse else 0.3f
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(baseHeight * scale)
                    .clip(CircleShape)
                    .background(FuturaColors.Neon.copy(alpha = if (playing) 0.8f else 0.3f)),
            )
        }
    }
}

@Composable
private fun Transport(
    playing: Boolean,
    onTogglePlay: () -> Unit,
    onSkip: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TransportButton(
            size = 56.dp,
            icon = Icons.Filled.SkipPrevious,
            label = "Previous track",
            onClick = { onSkip(-1) },
            primary = false,
        )
        Spacer(Modifier.size(24.dp))
        TransportButton(
            size = 80.dp,
            icon = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            label = if (playing) "Pause" else "Play",
            onClick = onTogglePlay,
            primary = true,
        )
        Spacer(Modifier.size(24.dp))
        TransportButton(
            size = 56.dp,
            icon = Icons.Filled.SkipNext,
            label = "Next track (roulette)",
            onClick = { onSkip(1) },
            primary = false,
        )
    }
}

@Composable
private fun TransportButton(
    size: androidx.compose.ui.unit.Dp,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    primary: Boolean,
) {
    val color = if (primary) FuturaColors.OledBlack else FuturaColors.Foreground
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(size)
            .border(
                width = 1.dp,
                color = if (primary) FuturaColors.Neon else Color.White.copy(alpha = 0.10f),
                shape = CircleShape,
            ),
        shape = CircleShape,
        color = if (primary) FuturaColors.Neon else Color.White.copy(alpha = 0.03f),
        contentColor = color,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(if (primary) 32.dp else 20.dp),
            )
        }
    }
}

@Composable
private fun QueueTrackCard(track: FuturaTrack, order: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.02f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "0$order",
            color = FuturaColors.Muted,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = FuturaColors.Foreground,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                color = FuturaColors.Muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = formatDuration(track.durationSeconds),
            color = FuturaColors.Muted,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
        )
    }
}
