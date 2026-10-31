package com.futura.audio.ui.alarm

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Stealth camouflage screen: shows a dim clock on a black background.
 * Double-tap the background to reveal the PIN pad, then enter the 4-digit PIN.
 *
 * @param correctPinHash lowercase hex SHA-256 of the PIN, computed with [hashPin].
 * @param nextAlarmLabel text shown under the clock, e.g. "07:00 AM". Passed in so it
 *   reflects the real scheduled alarm rather than a hardcoded value.
 */
@Composable
fun AlarmCamouflageScreen(
    correctPinHash: String,
    nextAlarmLabel: String,
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(getFormattedTime()) }
    var currentDate by remember { mutableStateOf(getFormattedDate()) }
    var showPinPad by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Update real-time clock every second
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getFormattedTime()
            currentDate = getFormattedDate()
            delay(1000)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black) // OLED true black #000000
            .pointerInput(Unit) {
                // Double-tap anywhere on the background toggles the PIN pad
                detectTapGestures(
                    onDoubleTap = {
                        showPinPad = !showPinPad
                        enteredPin = ""
                        errorMessage = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // --- Layer 1: dim minimalist clock ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(24.dp)
        ) {
            Text(
                text = currentTime,
                color = Color(0xFF222222), // dim gray that blends into OLED black
                fontSize = 72.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = currentDate,
                color = Color(0xFF1A1A1A),
                fontSize = 16.sp,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Next alarm: $nextAlarmLabel",
                color = Color(0xFF111111),
                fontSize = 14.sp
            )
        }

        // --- Layer 2: PIN pad overlay ---
        AnimatedVisibility(
            visible = showPinPad,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable(enabled = false) {}, // consume clicks so they don't reach the clock layer
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "ENTER SYSTEM PIN",
                        color = if (errorMessage) Color.Red else Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // PIN mask indicators
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        for (i in 0 until 4) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (i < enteredPin.length) Color.White else Color.DarkGray
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    val buttons = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("C", "0", "⌫")
                    )

                    buttons.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            row.forEach { label ->
                                PinButton(label = label) { digit ->
                                    when (digit) {
                                        "C" -> enteredPin = ""
                                        "⌫" -> {
                                            if (enteredPin.isNotEmpty()) {
                                                enteredPin = enteredPin.dropLast(1)
                                            }
                                        }
                                        else -> {
                                            if (enteredPin.length < 4) {
                                                enteredPin += digit
                                                if (enteredPin.length == 4) {
                                                    if (hashPin(enteredPin) == correctPinHash.lowercase()) {
                                                        onUnlocked()
                                                    } else {
                                                        errorMessage = true
                                                        coroutineScope.launch {
                                                            delay(500)
                                                            enteredPin = ""
                                                            errorMessage = false
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinButton(label: String, onClick: (String) -> Unit) {
    Surface(
        shape = CircleShape,
        color = Color(0xFF1C1C1E),
        modifier = Modifier
            .size(72.dp)
            .clickable { onClick(label) }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun getFormattedTime(): String {
    val sdf = SimpleDateFormat("hh:mm", Locale.getDefault())
    return sdf.format(Date())
}

private fun getFormattedDate(): String {
    val sdf = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
    return sdf.format(Date())
}

/**
 * Returns the lowercase hex SHA-256 of [pin]. Store this hash, never the PIN itself.
 * Note: a 4-digit PIN has only 10,000 possibilities, so a leaked hash can be brute-forced
 * offline. Treat this as concealment, not as a security boundary.
 */
fun hashPin(pin: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}
