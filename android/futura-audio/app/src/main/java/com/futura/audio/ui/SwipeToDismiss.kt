package com.futura.audio.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoubleArrow
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val COMMIT_THRESHOLD = 0.85f

/**
 * Slide-to-confirm control from the alarm prototype. Drag the thumb past 85% of the track to
 * fire [onComplete]; release earlier and it springs back.
 */
@Composable
fun SwipeToDismiss(label: String, onComplete: () -> Unit) {
    val density = LocalDensity.current
    val thumbSize = 64.dp
    val trackPadding = 6.dp
    val thumbPx = with(density) { thumbSize.toPx() }
    val padPx = with(density) { trackPadding.toPx() }

    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val maxOffset = (trackWidthPx - thumbPx - padPx * 2).coerceAtLeast(0f)

    var dragX by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val animatedX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val thumbX = if (dragging) dragX else animatedX.value
    val progress = if (maxOffset == 0f) 0f else thumbX / maxOffset

    fun settle(value: Float) {
        if (maxOffset > 0f && value / maxOffset >= COMMIT_THRESHOLD) {
            scope.launch {
                animatedX.animateTo(maxOffset, tween(150))
                onComplete()
                delay(900)
                dragX = 0f
                animatedX.snapTo(0f)
            }
        } else {
            scope.launch { animatedX.animateTo(0f, tween(300)) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .onSizeChanged { trackWidthPx = it.width.toFloat() }
            .clip(RoundedCornerShape(50))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.03f)),
    ) {
        Box(
            modifier = Modifier
                .height(76.dp)
                .width(with(density) { (thumbX + thumbPx + padPx * 2).toDp() })
                .clip(RoundedCornerShape(50))
                .background(FuturaColors.Cyan.copy(alpha = 0.12f)),
        )

        Text(
            text = label,
            color = FuturaColors.Foreground.copy(alpha = (1f - progress * 1.4f).coerceIn(0f, 1f) * 0.7f),
            fontFamily = FontFamilyMono,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = 64.dp),
        )

        Box(
            modifier = Modifier
                .offset { IntOffset((padPx + thumbX).roundToInt(), padPx.roundToInt()) }
                .size(thumbSize)
                .clip(CircleShape)
                .background(Color.Black)
                .border(1.dp, FuturaColors.Cyan.copy(alpha = 0.4f), CircleShape)
                .pointerInput(maxOffset) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragging = true
                            dragX = animatedX.value
                        },
                        onDragEnd = {
                            dragging = false
                            settle(dragX)
                        },
                        onDragCancel = {
                            dragging = false
                            settle(0f)
                        },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            dragX = (dragX + amount).coerceIn(0f, maxOffset)
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            if (progress >= COMMIT_THRESHOLD) {
                Icon(Icons.Filled.Lock, contentDescription = "Dismiss", tint = FuturaColors.Cyan, modifier = Modifier.size(24.dp))
            } else {
                Icon(Icons.Filled.DoubleArrow, contentDescription = "Swipe to dismiss", tint = FuturaColors.Cyan, modifier = Modifier.size(28.dp))
            }
        }
    }
}
