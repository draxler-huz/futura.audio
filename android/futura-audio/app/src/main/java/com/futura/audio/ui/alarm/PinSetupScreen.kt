package com.futura.audio.ui.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * First-run screen that sets the stealth PIN. Calls [onPinCreated] with the SHA-256 hash
 * once the PIN is entered twice and both entries match. The raw PIN is never passed out.
 */
@Composable
fun PinSetupScreen(
    onPinCreated: (pinHash: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var firstPinAttempt by remember { mutableStateOf("") }
    var confirmPinAttempt by remember { mutableStateOf("") }
    var isConfirmStep by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val currentInput = if (isConfirmStep) confirmPinAttempt else firstPinAttempt

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = if (isConfirmStep) "CONFIRM STEALTH PIN" else "SET INITIAL STEALTH PIN",
                color = if (errorMessage.isNotEmpty()) Color.Red else Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = errorMessage, color = Color.Red, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                                if (i < currentInput.length) Color(0xFF00FF66) else Color.DarkGray
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
                                "C" -> {
                                    if (isConfirmStep) confirmPinAttempt = "" else firstPinAttempt = ""
                                    errorMessage = ""
                                }
                                "⌫" -> {
                                    if (isConfirmStep && confirmPinAttempt.isNotEmpty()) {
                                        confirmPinAttempt = confirmPinAttempt.dropLast(1)
                                    } else if (!isConfirmStep && firstPinAttempt.isNotEmpty()) {
                                        firstPinAttempt = firstPinAttempt.dropLast(1)
                                    }
                                }
                                else -> {
                                    if (currentInput.length < 4) {
                                        if (isConfirmStep) {
                                            confirmPinAttempt += digit
                                            if (confirmPinAttempt.length == 4) {
                                                if (confirmPinAttempt == firstPinAttempt) {
                                                    onPinCreated(hashPin(confirmPinAttempt))
                                                } else {
                                                    errorMessage = "PINs do not match. Try again."
                                                    isConfirmStep = false
                                                    firstPinAttempt = ""
                                                    confirmPinAttempt = ""
                                                }
                                            }
                                        } else {
                                            firstPinAttempt += digit
                                            if (firstPinAttempt.length == 4) {
                                                isConfirmStep = true
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
