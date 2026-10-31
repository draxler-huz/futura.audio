package com.futura.audio.ui.music

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt

private const val ROW_HEIGHT_DP = 64

@Composable
fun PanelDeck(
    tab: DeckTab,
    onTabChange: (DeckTab) -> Unit,
    accent: Color,
    track: Track,
    time: Float,
    onSeek: (Float) -> Unit,
    queue: List<Track>,
    suggestions: List<Track>,
    queuedIds: Set<String>,
    related: List<Track>,
    currentId: String,
    onPlay: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, Deck.Border, RoundedCornerShape(24.dp))
            .background(Deck.Card.copy(alpha = 0.5f))
            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Deck.Background.copy(alpha = 0.6f))
                .padding(4.dp),
        ) {
            DeckTab.values().forEach { option ->
                val selected = option == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) Deck.Primary.copy(alpha = 0.10f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (selected) Deck.Primary.copy(alpha = 0.3f) else Color.Transparent,
                            RoundedCornerShape(12.dp),
                        )
                        .clickable { onTabChange(option) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = option.label,
                        color = if (selected) Deck.Primary else Deck.Muted,
                        fontFamily = Deck.Mono,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                    )
                }
            }
        }

        Crossfade(targetState = tab, label = "deck-panel") { current ->
            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 4.dp, end = 4.dp)) {
                when (current) {
                    DeckTab.QUEUE -> QueuePanel(queue, suggestions, currentId, onPlay)
                    DeckTab.LYRICS -> LyricsPanel(track, time, onSeek, accent)
                    DeckTab.RELATED -> RelatedPanel(track, related, queuedIds, accent)
                }
            }
        }
    }
}

@Composable
fun QueuePanel(
    queue: List<Track>,
    suggestions: List<Track>,
    currentId: String,
    onPlay: (String) -> Unit,
) {
    val totalSeconds = queue.sumOf { it.duration.toDouble() }.toFloat()

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "QUEUE • ${queue.size} TRACKS", color = Deck.Muted, fontFamily = Deck.Mono, fontSize = 10.sp, letterSpacing = 2.sp)
                Text(text = formatTime(totalSeconds), color = Deck.Muted, fontFamily = Deck.Mono, fontSize = 10.sp)
            }
            queue.forEach { track ->
                val active = track.id == currentId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ROW_HEIGHT_DP.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) Deck.Accent else Color.Transparent)
                        .clickable(enabled = !active) { onPlay(track.id) }
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(track.art),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            color = if (active) Deck.Primary else Deck.Foreground,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                        )
                        Text(text = track.artist, color = Deck.Muted, fontSize = 12.sp, maxLines = 1)
                    }
                    Text(text = formatTime(track.duration), color = Deck.Muted, fontFamily = Deck.Mono, fontSize = 11.sp)
                }
            }
        }

        if (suggestions.isNotEmpty()) {
            Column {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "NOT IN THIS QUEUE", color = Deck.Signal, fontFamily = Deck.Mono, fontSize = 10.sp, letterSpacing = 2.sp)
                }
                suggestions.forEach { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Deck.Border, RoundedCornerShape(12.dp))
                            .background(Deck.Background.copy(alpha = 0.4f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(track.art),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = track.title, color = Deck.Foreground, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1)
                            Text(text = track.artist, color = Deck.Muted, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

private val VOICE_A = Deck.Primary
private val VOICE_B = Deck.Signal
private val VOICE_BOTH = Color.White

private fun voiceColor(voice: String): Color = when (voice) {
    "A" -> VOICE_A
    "B" -> VOICE_B
    else -> VOICE_BOTH
}

@Composable
fun LyricsPanel(track: Track, time: Float, onSeek: (Float) -> Unit, accent: Color) {
    val lyrics = track.lyrics
    if (lyrics == null) {
        Text(
            text = "No synced lyrics for this track. Instrumental signal detected.",
            color = Deck.Muted,
            fontSize = 14.sp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Deck.Border, RoundedCornerShape(12.dp))
                .padding(32.dp),
        )
        return
    }

    var showTranslation by remember { mutableStateOf(true) }
    val lines = remember(track.id) { buildTimedLyrics(lyrics, track.duration) }
    val isDuet = track.voiceB != null
    var activeIndex = -1
    for (i in lines.indices) {
        if (lines[i].line.start <= time) activeIndex = i else break
    }

    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val lineHeightPx = with(density) { 84.dp.toPx() }
    val viewportPx = with(density) { 416.dp.toPx() }
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            val target = (activeIndex * lineHeightPx - viewportPx / 2 + lineHeightPx / 2).toInt().coerceAtLeast(0)
            scroll.animateScrollTo(target)
        }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = (track.voiceA ?: "").uppercase(), color = VOICE_A, fontFamily = Deck.Mono, fontSize = 10.sp)
                if (isDuet) {
                    Text(text = (track.voiceB ?: "").uppercase(), color = VOICE_B, fontFamily = Deck.Mono, fontSize = 10.sp)
                    Text(text = "TOGETHER", color = Color.White, fontFamily = Deck.Mono, fontSize = 10.sp)
                }
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, if (showTranslation) Deck.Primary.copy(alpha = 0.4f) else Deck.Border, RoundedCornerShape(50))
                    .background(if (showTranslation) Deck.Primary.copy(alpha = 0.10f) else Color.Transparent)
                    .clickable { showTranslation = !showTranslation }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Translate, contentDescription = null, tint = if (showTranslation) Deck.Primary else Deck.Muted, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(text = "ES", color = if (showTranslation) Deck.Primary else Deck.Muted, fontFamily = Deck.Mono, fontSize = 10.sp)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(416.dp)
                .verticalScroll(scroll)
                .padding(vertical = 160.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            lines.forEachIndexed { i, timed ->
                val isActive = i == activeIndex
                val isPast = i < activeIndex
                val color = voiceColor(timed.line.voice)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSeek(timed.line.start) }
                        .padding(horizontal = 4.dp),
                ) {
                    val annotated = buildAnnotatedString {
                        timed.words.forEachIndexed { w, word ->
                            val progress = when {
                                isActive -> ((time - word.start) / (word.end - word.start)).coerceIn(0f, 1f)
                                isPast -> 1f
                                else -> 0f
                            }
                            val unsung = if (isActive) Color.White.copy(alpha = 0.28f) else Deck.Muted
                            val sung = if (isActive || isPast) color else Deck.Muted
                            withStyle(SpanStyle(color = lerp(unsung, sung, progress))) {
                                append(word.text)
                            }
                            if (w < timed.words.lastIndex) append(" ")
                        }
                    }
                    Text(
                        text = annotated,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isDuet && timed.line.voice != "both") 18.sp else 22.sp,
                        lineHeight = 26.sp,
                        modifier = Modifier.alpha(
                            when {
                                isActive -> 1f
                                isPast -> 0.35f
                                else -> 0.5f
                            },
                        ),
                    )
                    if (showTranslation) {
                        Text(
                            text = timed.line.translation,
                            color = Deck.Muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RelatedPanel(track: Track, related: List<Track>, queuedIds: Set<String>, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column {
            Text(
                text = "MOOD PROFILE",
                color = Deck.Muted,
                fontFamily = Deck.Mono,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                track.moods.forEach { mood ->
                    Text(
                        text = mood,
                        color = accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(50))
                            .background(accent.copy(alpha = 0.08f))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MeterCard("ENERGY", track.energy, "${(track.energy * 100).roundToInt()}", accent, Modifier.weight(1f))
                MeterCard("VALENCE", track.valence, "${(track.valence * 100).roundToInt()}", accent, Modifier.weight(1f))
                MeterCard("TEMPO", (track.bpm / 160f).coerceAtMost(1f), "${track.bpm} BPM", accent, Modifier.weight(1f))
            }
        }

        Column {
            Text(
                text = "RELATED SIGNALS",
                color = Deck.Muted,
                fontFamily = Deck.Mono,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                related.take(3).forEach { item ->
                    val queued = queuedIds.contains(item.id)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Deck.Border, RoundedCornerShape(16.dp))
                            .background(Deck.Background.copy(alpha = 0.4f)),
                    ) {
                        Image(
                            painter = painterResource(item.art),
                            contentDescription = "${item.album} artwork",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        )
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = item.title, color = Deck.Foreground, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
                            Text(text = item.artist, color = Deck.Muted, fontSize = 10.sp, maxLines = 1)
                            Text(text = formatBadge(item.format), color = Deck.Primary.copy(alpha = 0.8f), fontFamily = Deck.Mono, fontSize = 8.sp, maxLines = 1)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = if (queued) "IN QUEUE" else "NOT QUEUED",
                                color = if (queued) Deck.Muted else Deck.Signal,
                                fontFamily = Deck.Mono,
                                fontSize = 8.sp,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MeterCard(label: String, value: Float, display: String, accent: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Deck.Border, RoundedCornerShape(12.dp))
            .background(Deck.Background.copy(alpha = 0.4f))
            .padding(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, color = Deck.Muted, fontFamily = Deck.Mono, fontSize = 9.sp, letterSpacing = 1.sp)
            Text(text = display, color = Deck.Foreground, fontFamily = Deck.Mono, fontSize = 9.sp)
        }
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.05f))) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(value.coerceIn(0f, 1f))
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(listOf(Deck.Primary, accent)), RoundedCornerShape(50)),
            )
        }
    }
}
