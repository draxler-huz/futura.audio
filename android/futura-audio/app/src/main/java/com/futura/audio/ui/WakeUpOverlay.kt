package com.futura.audio.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Monospace face used by the alarm screens (the prototype's JetBrains Mono falls back to the system mono). */
internal val FontFamilyMono: FontFamily = FontFamily.Monospace

/** Wake-up screen from the alarm prototype: status chips, large clock, snooze, and swipe-to-dismiss. */
@Composable
fun WakeUpOverlay(
    deviceSeconds: Int,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(FuturaColors.OledBlack)) {
        Box(
            modifier = Modifier
                .offset(y = maxHeight * 0.38f - 144.dp)
                .align(Alignment.TopCenter)
                .size(288.dp)
                .clip(CircleShape)
                .background(FuturaColors.Neon.copy(alpha = 0.08f)),
        )

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            StatusBar()
            TimeSurface(deviceSeconds = deviceSeconds, modifier = Modifier.weight(1f))
            ActionDock(onSnooze = onSnooze, onDismiss = onDismiss)
        }
    }
}

@Composable
private fun StatusBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, FuturaColors.Neon.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Alarm, contentDescription = null, tint = FuturaColors.Neon, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text("ALARM ARMED //", color = FuturaColors.Neon.copy(alpha = 0.6f), fontFamily = FontFamilyMono, fontSize = 9.sp, letterSpacing = 2.sp)
                Text(NEXT_ALARM_LABEL, color = FuturaColors.Neon, fontFamily = FontFamilyMono, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, FuturaColors.Cyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Bluetooth, contentDescription = null, tint = FuturaColors.Cyan, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("BT: $AUDIO_OUTPUT_DEVICE", color = FuturaColors.Cyan, fontFamily = FontFamilyMono, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
                Text("• ${AUDIO_OUTPUT_MODE.uppercase()}", color = FuturaColors.Cyan.copy(alpha = 0.6f), fontFamily = FontFamilyMono, fontSize = 9.sp, letterSpacing = 2.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun TimeSurface(deviceSeconds: Int, modifier: Modifier) {
    val clock = splitClock(deviceSeconds)
    val pulse by rememberInfiniteTransition(label = "ready").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "ready-alpha",
    )

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("WAKE CYCLE 01", color = FuturaColors.Muted, fontFamily = FontFamilyMono, fontSize = 11.sp, letterSpacing = 4.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = clock.hhmm,
            color = FuturaColors.Neon,
            fontFamily = FontFamilyMono,
            fontWeight = FontWeight.Bold,
            fontSize = 92.sp,
            textAlign = TextAlign.Center,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(clock.meridiem, color = FuturaColors.Neon.copy(alpha = 0.8f), fontFamily = FontFamilyMono, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = 2.sp)
            Spacer(Modifier.width(12.dp))
            Text(":${clock.seconds}", color = FuturaColors.Muted, fontFamily = FontFamilyMono, fontSize = 14.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).alpha(pulse).clip(CircleShape).background(FuturaColors.Cyan))
            Spacer(Modifier.width(8.dp))
            Text(
                "ROULETTE QUEUE READY",
                color = FuturaColors.Cyan.copy(alpha = pulse),
                fontFamily = FontFamilyMono,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                letterSpacing = 3.sp,
            )
        }
    }
}

@Composable
private fun ActionDock(onSnooze: () -> Unit, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(50))
                .border(1.dp, FuturaColors.Neon.copy(alpha = 0.35f), RoundedCornerShape(50))
                .background(FuturaColors.Neon.copy(alpha = 0.10f))
                .clickable(onClick = onSnooze),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("SNOOZE", color = FuturaColors.Neon, fontFamily = FontFamilyMono, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = 4.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Shuffle, contentDescription = null, tint = FuturaColors.Neon.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("START ROULETTE QUEUE", color = FuturaColors.Neon.copy(alpha = 0.7f), fontFamily = FontFamilyMono, fontSize = 11.sp, letterSpacing = 3.sp)
            }
        }

        SwipeToDismiss(label = "DISMISS ALARM & LOCK DECK", onComplete = onDismiss)
    }
}
