package com.futura.audio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Locked state after dismissal, ported from the alarm prototype's deck-locked view. */
@Composable
fun DeckLockedScreen(
    deviceSeconds: Int,
    onRearm: () -> Unit,
) {
    val clock = splitClock(deviceSeconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FuturaColors.OledBlack)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                .background(Color.White.copy(alpha = 0.03f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = FuturaColors.Muted, modifier = Modifier.size(32.dp))
        }

        Spacer(Modifier.height(32.dp))
        Text(
            text = "DECK LOCKED",
            color = FuturaColors.Foreground,
            fontFamily = FontFamilyMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            letterSpacing = 6.sp,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = clock.hhmm,
                color = FuturaColors.Foreground.copy(alpha = 0.4f),
                fontFamily = FontFamilyMono,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.width(8.dp))
            Text(clock.meridiem, color = FuturaColors.Foreground.copy(alpha = 0.4f), fontFamily = FontFamilyMono, fontSize = 20.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Alarm dismissed • Next: $NEXT_ALARM_LABEL tomorrow",
            color = FuturaColors.Muted,
            fontFamily = FontFamilyMono,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .border(1.dp, FuturaColors.Neon.copy(alpha = 0.3f), RoundedCornerShape(50))
                .clickable(onClick = onRearm)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Replay, contentDescription = null, tint = FuturaColors.Neon, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("REPLAY WAKE-UP DEMO", color = FuturaColors.Neon, fontFamily = FontFamilyMono, fontSize = 11.sp, letterSpacing = 2.sp)
        }
    }
}
