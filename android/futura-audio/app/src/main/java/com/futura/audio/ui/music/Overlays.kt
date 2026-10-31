package com.futura.audio.ui.music

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futura.audio.ui.NEXT_ALARM_LABEL
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Stream inspector drawer: codec, bit depth, sample rate, latency, and signal path. */
@Composable
fun StreamInspector(open: Boolean, format: AudioFormat, eqOn: Boolean, onClose: () -> Unit) {
    val sampleRateHz = format.sampleRateKhz * 1000
    val rawKbps = format.bitDepth * sampleRateHz * 2 / 1000
    val bitrate = Math.round(rawKbps * 0.6)
    val rawRounded = Math.round(rawKbps)
    val latencyMs = 8.3
    val bufferFrames = Math.round(latencyMs / 1000 * sampleRateHz)
    val path = listOf("Decoder", "Resampler", if (eqOn) "DSP / EQ" else "DSP bypass", "HAL", "DAC")

    val stats = listOf(
        Triple("Codec", format.codec, "Lossless"),
        Triple("Bit depth", "${format.bitDepth}-bit", "Integer PCM"),
        Triple("Sample rate", "%.1f kHz".format(format.sampleRateKhz), "Native, no SRC"),
        Triple("Bitrate", "${"%,d".format(bitrate)} kbps", "of ${"%,d".format(rawRounded)} raw"),
        Triple("Audio HAL", "AAudio", "MMAP exclusive"),
        Triple("Output", "USB DAC", "UAC 2.0 • 2ch"),
    )

    AnimatedVisibility(
        visible = open,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { -it },
        exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { -it },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(onClick = onClose),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .border(1.dp, Deck.Primary.copy(alpha = 0.2f), RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .background(Deck.Card.copy(alpha = 0.97f))
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("DIAGNOSTICS", color = Deck.Muted, fontFamily = Deck.Mono, fontSize = 10.sp, letterSpacing = 2.sp)
                        Text("Stream Inspector", color = Deck.Foreground, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .border(1.dp, Deck.Signal.copy(alpha = 0.3f), RoundedCornerShape(50))
                                .background(Deck.Signal.copy(alpha = 0.10f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(Deck.Signal))
                            Spacer(Modifier.width(6.dp))
                            Text("BIT-PERFECT", color = Deck.Signal, fontFamily = Deck.Mono, fontSize = 10.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(50))
                                .border(1.dp, Deck.Border, RoundedCornerShape(50))
                                .clickable(onClick = onClose),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close stream inspector", tint = Deck.Muted, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                stats.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { (label, value, sub) ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(bottom = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, Deck.Border, RoundedCornerShape(12.dp))
                                    .background(Deck.Background.copy(alpha = 0.6f))
                                    .padding(12.dp),
                            ) {
                                Text(label.uppercase(), color = Deck.Muted, fontFamily = Deck.Mono, fontSize = 9.sp, letterSpacing = 1.sp)
                                Text(value, color = Deck.Foreground, fontFamily = Deck.Mono, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(sub, color = Deck.Muted, fontFamily = Deck.Mono, fontSize = 9.sp)
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Deck.Signal.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .background(Deck.Signal.copy(alpha = 0.04f))
                        .padding(16.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("OUTPUT LATENCY", color = Deck.Muted, fontFamily = Deck.Mono, fontSize = 9.sp, letterSpacing = 1.sp)
                        Text("OPTIMAL", color = Deck.Signal, fontFamily = Deck.Mono, fontSize = 9.sp, letterSpacing = 1.sp)
                    }
                    Text(
                        text = "$latencyMs ms",
                        color = Deck.Signal,
                        fontFamily = Deck.Mono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.05f))) {
                        Box(Modifier.fillMaxWidth((latencyMs / 40).toFloat()).fillMaxSize().background(Deck.Signal, RoundedCornerShape(50)))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Buffer $bufferFrames frames @ ${format.sampleRateKhz} kHz • threshold 40 ms",
                        color = Deck.Muted,
                        fontFamily = Deck.Mono,
                        fontSize = 9.sp,
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text("SIGNAL PATH", color = Deck.Muted, fontFamily = Deck.Mono, fontSize = 9.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    path.forEachIndexed { i, step ->
                        val bypassed = step == "Resampler" || step == "DSP bypass"
                        Text(
                            text = step,
                            color = if (bypassed) Deck.Muted else Deck.Primary,
                            fontFamily = Deck.Mono,
                            fontSize = 9.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, if (bypassed) Deck.Border else Deck.Primary.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .background(if (bypassed) Color.Transparent else Deck.Primary.copy(alpha = 0.05f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                        if (i < path.lastIndex) {
                            Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = Deck.Muted, modifier = Modifier.size(10.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/** Full-screen alarm deck: clock, next alarm, wake signal, and a way back to the player. */
@Composable
fun AlarmDeckOverlay(open: Boolean, track: Track, onClose: () -> Unit) {
    AnimatedVisibility(
        visible = open,
        enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -it / 12 },
        exit = fadeOut(tween(500)) + slideOutVertically(tween(500)) { -it / 12 },
    ) {
        val now by produceState(initialValue = Date()) {
            while (true) {
                value = Date()
                delay(1000)
            }
        }
        val timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        val dateText = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(now).uppercase()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Deck.Background)
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Alarm, contentDescription = null, tint = Deck.Signal, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text("PHASE 1 • ALARM DECK", color = Deck.Muted, fontFamily = Deck.Mono, fontSize = 11.sp, letterSpacing = 2.sp)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(timeText, color = Deck.Foreground, fontFamily = Deck.Mono, fontWeight = FontWeight.Bold, fontSize = 72.sp)
                Spacer(Modifier.height(8.dp))
                Text(dateText, color = Deck.Muted, fontFamily = Deck.Mono, fontSize = 12.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(32.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, Deck.Signal.copy(alpha = 0.3f), RoundedCornerShape(50))
                        .background(Deck.Signal.copy(alpha = 0.05f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(Deck.Signal))
                    Spacer(Modifier.width(10.dp))
                    Text("NEXT ALARM $NEXT_ALARM_LABEL • WEEKDAYS", color = Deck.Signal, fontFamily = Deck.Mono, fontSize = 11.sp, letterSpacing = 1.sp)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(0.9f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Deck.Border, RoundedCornerShape(16.dp))
                        .background(Deck.Card.copy(alpha = 0.6f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(track.art),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("WAKE SIGNAL", color = Deck.Muted, fontFamily = Deck.Mono, fontSize = 9.sp, letterSpacing = 1.sp)
                        Text(track.title, color = Deck.Foreground, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                        Text(track.artist, color = Deck.Muted, fontSize = 12.sp, maxLines = 1)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onClose)
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, tint = Deck.Primary, modifier = Modifier.size(24.dp))
                    Text("OPEN PLAYER DECK", color = Deck.Primary, fontFamily = Deck.Mono, fontSize = 11.sp, letterSpacing = 2.sp)
                }
            }
        }
    }
}
