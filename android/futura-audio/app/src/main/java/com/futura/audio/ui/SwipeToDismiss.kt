package com.futura.audio.ui

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val COMMIT_THRESHOLD = 0.85f
private const val KEY_STEP = 0.2f
private val THUMB_SIZE = 64.dp
private val TRACK_PADDING = 6.dp

@Composable
fun SwipeToDismiss(label: String, onComplete: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var offsetPx by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(RoundedCornerShape(38.dp))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(38.dp))
            .background(Color.White.copy(alpha = 0.03f)),
    ) {
        val maxOffsetPx = with(density) {
            (maxWidth - THUMB_SIZE - TRACK_PADDING * 2).coerceAtLeast(0.dp).toPx()
        }
        val visualOffsetPx by animateFloatAsState(
            targetValue = offsetPx,
            animationSpec = if (dragging) snap() else tween(durationMillis = 300),
            label = "dismiss thumb",
        )
        val progress = if (maxOffsetPx == 0f) 0f else (visualOffsetPx / maxOffsetPx).coerceIn(0f, 1f)
        val targetFillWidth = with(density) {
            (visualOffsetPx + (THUMB_SIZE + TRACK_PADDING * 2).toPx()).toDp().coerceAtMost(maxWidth)
        }
        val fillWidth by animateDpAsState(
            targetValue = targetFillWidth,
            animationSpec = if (dragging) snap() else tween(durationMillis = 300),
            label = "dismiss track fill",
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(fillWidth)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            FuturaColors.Cyan.copy(alpha = 0.05f),
                            FuturaColors.Cyan.copy(alpha = 0.25f),
                        ),
                    ),
                ),
        )
        Text(
            text = label,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 64.dp),
            color = FuturaColors.Foreground.copy(alpha = (1f - progress * 1.4f).coerceAtLeast(0f)),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.75.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = TRACK_PADDING)
                .offset { IntOffset(visualOffsetPx.roundToInt(), 0) }
                .size(THUMB_SIZE)
                .clip(CircleShape)
                .border(1.dp, FuturaColors.Cyan.copy(alpha = 0.65f), CircleShape)
                .background(FuturaColors.OledBlack)
                .pointerInput(maxOffsetPx) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragging = true },
                        onDragCancel = {
                            dragging = false
                            offsetPx = 0f
                        },
                        onDragEnd = {
                            dragging = false
                            settleDismiss(
                                context = context,
                                scope = scope,
                                value = offsetPx,
                                maxValue = maxOffsetPx,
                                onOffsetChange = { offsetPx = it },
                                onComplete = onComplete,
                            )
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        offsetPx = (offsetPx + dragAmount).coerceIn(0f, maxOffsetPx)
                    }
                }
                .semantics {
                        role = Role.Switch
                        progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                        stateDescription = "${(progress * 100).roundToInt()} percent"
                    }
                .onKeyEvent { event ->
                    when (event.key) {
                        Key.Enter, Key.NumPadEnter, Key.Spacebar -> {
                            settleDismiss(
                                context = context,
                                scope = scope,
                                value = maxOffsetPx,
                                maxValue = maxOffsetPx,
                                onOffsetChange = { offsetPx = it },
                                onComplete = onComplete,
                            )
                            true
                        }

                        Key.DirectionRight -> {
                            val next = (offsetPx + maxOffsetPx * KEY_STEP).coerceAtMost(maxOffsetPx)
                            settleDismiss(
                                context = context,
                                scope = scope,
                                value = next,
                                maxValue = maxOffsetPx,
                                onOffsetChange = { offsetPx = it },
                                onComplete = onComplete,
                            )
                            true
                        }

                        Key.DirectionLeft, Key.Escape -> {
                            offsetPx = 0f
                            true
                        }

                        else -> false
                    }
                }
                .focusable(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (progress >= COMMIT_THRESHOLD) {
                    Icons.Outlined.Lock
                } else {
                    Icons.Filled.KeyboardDoubleArrowRight
                },
                contentDescription = label,
                tint = FuturaColors.Cyan,
                modifier = Modifier.size(if (progress >= COMMIT_THRESHOLD) 24.dp else 28.dp),
            )
        }
    }
}

private fun settleDismiss(
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    value: Float,
    maxValue: Float,
    onOffsetChange: (Float) -> Unit,
    onComplete: () -> Unit,
) {
    if (maxValue > 0f && value / maxValue >= COMMIT_THRESHOLD) {
        onOffsetChange(maxValue)
        val vibrator = context.getSystemService(Vibrator::class.java)
        vibrator?.vibrate(VibrationEffect.createOneShot(30L, VibrationEffect.DEFAULT_AMPLITUDE))
        onComplete()
        scope.launch {
            delay(900L)
            onOffsetChange(0f)
        }
    } else {
        onOffsetChange(0f)
    }
}
