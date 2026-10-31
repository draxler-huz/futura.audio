package com.futura.audio.ui.music

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun HeroArtwork(
    @DrawableRes art: Int,
    album: String,
    title: String,
    bpm: Int,
    playing: Boolean,
    accent: Color,
) {
    val transition = rememberInfiniteTransition(label = "bass")
    val beat by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (60_000 / bpm) / 2, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "beat",
    )
    val auraScale = if (playing) beat else 1f

    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .aspectRatio(1f)
                .graphicsLayer { scaleX = auraScale; scaleY = auraScale }
                .background(
                    Brush.radialGradient(listOf(accent.copy(alpha = 0.45f), Color.Transparent)),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(32.dp))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(32.dp)),
        ) {
            Image(
                painter = painterResource(art),
                contentDescription = "$album album artwork for $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun TrackInfo(track: Track, accent: Color, onOpenLyrics: () -> Unit) {
    val hasLyrics = track.lyrics != null
    val isDuet = track.voiceB != null
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "${track.album.uppercase()} • ${track.bpm} BPM • ${track.key}",
            color = Deck.Muted,
            fontFamily = Deck.Mono,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = track.title,
            color = Deck.Foreground,
            fontFamily = Deck.Sans,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(text = track.artist, color = accent, fontFamily = Deck.Sans, fontWeight = FontWeight.Medium, fontSize = 18.sp)
        if (hasLyrics) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, Deck.Signal.copy(alpha = 0.3f), RoundedCornerShape(50))
                    .background(Deck.Signal.copy(alpha = 0.05f))
                    .clickable(onClick = onOpenLyrics)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Mic, contentDescription = null, tint = Deck.Signal, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isDuet) "DUET / SYNCED LYRICS AVAILABLE" else "SYNCED LYRICS AVAILABLE",
                    color = Deck.Signal,
                    fontFamily = Deck.Mono,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}

@Composable
fun WaveformScrubber(
    trackId: String,
    duration: Float,
    time: Float,
    playing: Boolean,
    onSeek: (Float) -> Unit,
) {
    val peaks = remember(trackId) { generatePeaks(trackId, 96) }
    val progress = (time / duration).coerceIn(0f, 1f)
    val playedBars = (progress * 96).toInt()

    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .pointerInput(duration) {
                    detectTapGestures(
                        onTap = { offset -> onSeek((offset.x / size.width).coerceIn(0f, 1f) * duration) },
                    )
                }
                .pointerInput(duration) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset -> onSeek((offset.x / size.width).coerceIn(0f, 1f) * duration) },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            onSeek((change.position.x / size.width).coerceIn(0f, 1f) * duration)
                        },
                    )
                },
        ) {
            val barWidth = size.width / 96f
            val gap = 2f
            for (i in peaks.indices) {
                val h = peaks[i] * size.height
                val played = i < playedBars
                drawRoundRect(
                    color = if (played) Deck.Primary else Color.White.copy(alpha = 0.15f),
                    topLeft = Offset(i * barWidth + gap / 2, (size.height - h) / 2),
                    size = Size(barWidth - gap, h),
                    cornerRadius = CornerRadius(4f, 4f),
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(4.dp)) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(50)))
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(listOf(Deck.Primary, Deck.Signal)), RoundedCornerShape(50)),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = formatTime(time), color = Deck.Muted, fontFamily = Deck.Mono, fontSize = 12.sp)
            Text(text = "-${formatTime(duration - time)}", color = Deck.Muted, fontFamily = Deck.Mono, fontSize = 12.sp)
        }
    }
}

private val BANDS = listOf("31" to 4f, "62" to 3.5f, "125" to 2f, "250" to 0.5f, "500" to -1f, "1k" to -1.5f, "2k" to 0f, "4k" to 1.5f, "8k" to 3f, "16k" to 3.5f)

@Composable
fun EqStrip(open: Boolean, playing: Boolean) {
    AnimatedVisibility(visible = open, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        val transition = rememberInfiniteTransition(label = "eq")
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
            label = "eq-phase",
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Deck.Signal.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .background(Deck.Signal.copy(alpha = 0.04f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text("EQ ACTIVE", color = Deck.Signal, fontFamily = Deck.Mono, fontSize = 10.sp)
                Text("FUTURA CURVE", color = Deck.Muted, fontFamily = Deck.Mono, fontSize = 10.sp)
            }
            Row(
                modifier = Modifier.weight(1f).height(48.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BANDS.forEachIndexed { i, (_, gain) ->
                    val wobble = if (playing) 0.35f + 0.65f * abs(sin(phase * PI.toFloat() + i * 0.7f)) else 1f
                    val fraction = (0.30f + ((gain + 2f) / 6f) * 0.60f) * wobble
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(fraction.coerceIn(0.05f, 1f))
                            .clip(RoundedCornerShape(3.dp))
                            .background(Brush.verticalGradient(listOf(Deck.Signal, Deck.Signal.copy(alpha = 0.3f)))),
                    )
                }
            }
        }
    }
}
