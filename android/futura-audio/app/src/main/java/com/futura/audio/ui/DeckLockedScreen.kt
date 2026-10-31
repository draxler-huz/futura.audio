package com.futura.audio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
                .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                .background(Color.White.copy(alpha = 0.03f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = FuturaColors.Muted,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.size(32.dp))
        Text(
            text = "DECK LOCKED",
            color = FuturaColors.Foreground,
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 5.4.sp,
        )
        Spacer(Modifier.size(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = clock.hhmm,
                color = FuturaColors.Foreground.copy(alpha = 0.4f),
                fontFamily = FontFamily.Monospace,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = clock.meridiem,
                color = FuturaColors.Foreground.copy(alpha = 0.4f),
                fontFamily = FontFamily.Monospace,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.size(8.dp))
        Text(
            text = "Alarm dismissed • Next: $NEXT_ALARM_LABEL tomorrow",
            color = FuturaColors.Muted,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            letterSpacing = 1.4.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(32.dp))
        Surface(
            onClick = onRearm,
            modifier = Modifier.border(
                1.dp,
                FuturaColors.Neon.copy(alpha = 0.3f),
                CircleShape,
            ),
            shape = CircleShape,
            color = Color.Transparent,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Replay,
                    contentDescription = null,
                    tint = FuturaColors.Neon,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "REPLAY WAKE-UP DEMO",
                    color = FuturaColors.Neon,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp,
                )
            }
        }
    }
}
