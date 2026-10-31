package com.futura.audio.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private const val TRANSITION_MILLIS = 700

@Composable
fun FuturaApp(viewModel: FuturaViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(FuturaColors.OledBlack),
        contentAlignment = Alignment.Center,
    ) {
        val cardShape = RoundedCornerShape(if (maxWidth > 420.dp) 44.dp else 0.dp)
        Box(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .fillMaxSize()
                .clip(cardShape)
                .background(FuturaColors.OledBlack)
                .then(
                    if (maxWidth > 420.dp) {
                        Modifier.border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.10f),
                            shape = cardShape,
                        )
                    } else {
                        Modifier
                    },
                ),
        ) {
            AnimatedContent(
                targetState = state.screen,
                transitionSpec = {
                    screenTransition(initialState, targetState)
                },
                label = "Futura screen transition",
            ) { screen ->
                when (screen) {
                    FuturaScreen.ALARM -> WakeUpOverlay(
                        deviceSeconds = state.deviceSeconds,
                        onSnooze = viewModel::snooze,
                        onDismiss = viewModel::dismiss,
                    )

                    FuturaScreen.DASHBOARD -> MusicDashboard(
                        state = state,
                        onTogglePlay = viewModel::togglePlay,
                        onSkip = viewModel::skip,
                        onWakeNow = viewModel::wakeNow,
                    )

                    FuturaScreen.LOCKED -> DeckLockedScreen(
                        deviceSeconds = state.deviceSeconds,
                        onRearm = viewModel::rearmDemo,
                    )
                }
            }
        }
    }
}

private fun screenTransition(
    initial: FuturaScreen,
    target: FuturaScreen,
): ContentTransform {
    val animationSpec = tween<Float>(
        durationMillis = TRANSITION_MILLIS,
        easing = FastOutSlowInEasing,
    )
    return when {
        initial == FuturaScreen.ALARM && target == FuturaScreen.DASHBOARD ->
            (slideInHorizontally(
                initialOffsetX = { width -> width },
                animationSpec = tween(TRANSITION_MILLIS, easing = FastOutSlowInEasing),
            ) + fadeIn(animationSpec)) togetherWith
                (slideOutHorizontally(
                    targetOffsetX = { width -> -width },
                    animationSpec = tween(TRANSITION_MILLIS, easing = FastOutSlowInEasing),
                ) + fadeOut(animationSpec))

        initial == FuturaScreen.DASHBOARD && target == FuturaScreen.ALARM ->
            (slideInHorizontally(
                initialOffsetX = { width -> -width },
                animationSpec = tween(TRANSITION_MILLIS, easing = FastOutSlowInEasing),
            ) + fadeIn(animationSpec)) togetherWith
                (slideOutHorizontally(
                    targetOffsetX = { width -> width },
                    animationSpec = tween(TRANSITION_MILLIS, easing = FastOutSlowInEasing),
                ) + fadeOut(animationSpec))

        target == FuturaScreen.LOCKED ->
            (fadeIn(animationSpec) + scaleIn(initialScale = 0.95f, animationSpec = animationSpec)) togetherWith
                (fadeOut(animationSpec) + scaleOut(targetScale = 0.95f, animationSpec = animationSpec))

        initial == FuturaScreen.LOCKED && target == FuturaScreen.ALARM ->
            fadeIn(animationSpec) togetherWith fadeOut(animationSpec)

        else -> fadeIn(animationSpec) togetherWith fadeOut(animationSpec)
    }.using(SizeTransform(clip = false))
}
