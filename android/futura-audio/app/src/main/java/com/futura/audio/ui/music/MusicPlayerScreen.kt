package com.futura.audio.ui.music

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.DoubleArrow
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.futura.audio.ui.FuturaViewModel
import com.futura.audio.ui.ROULETTE_QUEUE
import com.futura.audio.ui.library.LibraryScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.icons.filled.LibraryMusic
import kotlinx.coroutines.delay

/**
 * Music screen, driven by [FuturaViewModel]. Track index, playing flag and elapsed time come from
 * the view model's state. Play/pause, previous, next and Wake Now call the view model.
 *
 * No PCM is loaded in this snapshot, so play does nothing until a track is supplied through
 * `loadTrackPcm`. The elapsed time stays at zero until then.
 */
@Composable
fun MusicPlayerScreen(viewModel: FuturaViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val queue = remember { MusicCatalog.queueTracks() }
    val index = state.trackIndex.coerceIn(ROULETTE_QUEUE.indices)
    val current = queue[index]
    val time = state.trackElapsedSeconds.toFloat()
    val playing = state.playing

    var eqOn by remember { mutableStateOf(false) }
    var inspectorOpen by remember { mutableStateOf(false) }
    var alarmOpen by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(DeckTab.LYRICS) }
    var wakeArmed by remember { mutableStateOf(false) }
    var libraryOpen by remember { mutableStateOf(false) }

    val accent = rememberDominantColor(current.art, Color(current.accent))
    val queuedIds = remember(queue) { queue.map { it.id }.toSet() }
    val suggestions: List<Track> = emptyList()
    val related = remember(current.id) { MusicCatalog.relatedFor(current, queue) }

    // The Wake Now control needs a second tap within three seconds, so a stray tap can't ring the alarm.
    LaunchedEffect(wakeArmed) {
        if (wakeArmed) {
            delay(3_000)
            wakeArmed = false
        }
    }

    val onPlay: (String) -> Unit = { id ->
        val target = queue.indexOfFirst { it.id == id }
        if (target >= 0) viewModel.selectTrack(target)
    }

    BackHandler(enabled = inspectorOpen) { inspectorOpen = false }
    BackHandler(enabled = alarmOpen) { alarmOpen = false }
    BackHandler(enabled = libraryOpen) { libraryOpen = false }

    Box(modifier = Modifier.fillMaxSize().background(Deck.Background)) {
        AmbientBackground(art = current.art, accent = accent)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            TopBar(
                formatLabel = formatBadge(current.format),
                eqOn = eqOn,
                onAlarmDeck = { alarmOpen = true },
                onInspector = { inspectorOpen = true },
                onToggleEq = { eqOn = !eqOn },
            )
            EqStrip(open = eqOn, playing = playing)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 160.dp),
            ) {
                HeroArtwork(
                    art = current.art,
                    album = current.album,
                    title = current.title,
                    bpm = current.bpm,
                    playing = playing,
                    accent = accent,
                )
                TrackInfo(track = current, accent = accent, onOpenLyrics = { tab = DeckTab.LYRICS })
                WaveformScrubber(
                    trackId = current.id,
                    duration = current.duration,
                    time = time,
                    playing = playing,
                    onSeek = {}, // The engine has no seek call yet, so the scrubber only displays progress.
                )
                PanelDeck(
                    tab = tab,
                    onTabChange = { tab = it },
                    accent = accent,
                    track = current,
                    time = time,
                    onSeek = {},
                    queue = queue,
                    currentId = current.id,
                    suggestions = suggestions,
                    queuedIds = queuedIds,
                    related = related,
                    onPlay = onPlay,
                )
            }
        }

        ControlDock(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
            playing = playing,
            progress = (time / current.duration).coerceIn(0f, 1f),
            wakeArmed = wakeArmed,
            onTogglePlay = viewModel::togglePlay,
            onPrev = { viewModel.skip(-1) },
            onNext = { viewModel.skip(1) },
            onLibrary = { libraryOpen = true },
            onWake = {
                if (wakeArmed) {
                    wakeArmed = false
                    viewModel.wakeNow()
                } else {
                    wakeArmed = true
                }
            },
        )

        AnimatedVisibility(
            visible = libraryOpen,
            modifier = Modifier.fillMaxSize(),
            enter = slideInHorizontally(tween(450)) { it } + fadeIn(tween(450)),
            exit = slideOutHorizontally(tween(450)) { it } + fadeOut(tween(450)),
        ) {
            LibraryScreen(
                currentIndex = index,
                onPlay = { target ->
                    val alreadyPlayingThis = state.playing && state.trackIndex == target
                    viewModel.selectTrack(target)
                    if (!alreadyPlayingThis) viewModel.togglePlay()
                    libraryOpen = false
                },
                onClose = { libraryOpen = false },
            )
        }

        StreamInspector(
            open = inspectorOpen,
            format = current.format,
            eqOn = eqOn,
            onClose = { inspectorOpen = false },
        )
        AlarmDeckOverlay(
            open = alarmOpen,
            track = current,
            onClose = { alarmOpen = false },
        )
    }
}

@Composable
private fun AmbientBackground(art: Int, accent: Color) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(art),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.2f,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x8C030507), Deck.Background),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.18f), Color.Transparent))),
        )
    }
}

@Composable
private fun TopBar(
    formatLabel: String,
    eqOn: Boolean,
    onAlarmDeck: () -> Unit,
    onInspector: () -> Unit,
    onToggleEq: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .height(40.dp)
                .clip(RoundedCornerShape(50))
                .border(1.dp, Deck.Border, RoundedCornerShape(50))
                .background(Deck.Card.copy(alpha = 0.6f))
                .clickable(onClick = onAlarmDeck)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Lock, contentDescription = "Alarm deck", tint = Deck.Muted, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = "ALARM DECK", color = Deck.Muted, fontFamily = Deck.Mono, fontSize = 11.sp, letterSpacing = 1.sp)
        }

        val pulse by rememberInfiniteTransition(label = "dot").animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
            label = "dot-alpha",
        )
        Row(
            modifier = Modifier
                .height(40.dp)
                .clip(RoundedCornerShape(50))
                .border(1.dp, Deck.Primary.copy(alpha = 0.25f), RoundedCornerShape(50))
                .background(Deck.Primary.copy(alpha = 0.05f))
                .clickable(onClick = onInspector)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .alpha(pulse)
                    .clip(CircleShape)
                    .background(Deck.Signal),
            )
            Spacer(Modifier.width(8.dp))
            Text(text = formatLabel, color = Deck.Primary, fontFamily = Deck.Mono, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Filled.DoubleArrow, contentDescription = "Open stream inspector", tint = Deck.Primary, modifier = Modifier.size(12.dp))
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(
                    1.dp,
                    if (eqOn) Deck.Signal.copy(alpha = 0.4f) else Deck.Border,
                    CircleShape,
                )
                .background(if (eqOn) Deck.Signal.copy(alpha = 0.10f) else Deck.Card.copy(alpha = 0.6f))
                .clickable(onClick = onToggleEq),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Tune,
                contentDescription = "Equalizer",
                tint = if (eqOn) Deck.Signal else Deck.Muted,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ControlDock(
    modifier: Modifier,
    playing: Boolean,
    progress: Float,
    wakeArmed: Boolean,
    onTogglePlay: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onLibrary: () -> Unit,
    onWake: () -> Unit,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(50))
                .background(Deck.Card.copy(alpha = 0.85f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DockButton(icon = Icons.Filled.SkipPrevious, label = "Previous", tint = Deck.Foreground, size = 24.dp, onClick = onPrev)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Deck.Primary)
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = Deck.PrimaryForeground,
                    modifier = Modifier.size(28.dp),
                )
            }
            DockButton(icon = Icons.Filled.SkipNext, label = "Next", tint = Deck.Foreground, size = 24.dp, onClick = onNext)
            DockButton(icon = Icons.Filled.LibraryMusic, label = "Library matrix", tint = Deck.Primary, onClick = onLibrary)
            DockButton(
                icon = Icons.Filled.Alarm,
                label = if (wakeArmed) "Tap again to ring the alarm now" else "Wake now",
                tint = if (wakeArmed) Deck.Destructive else Deck.Muted,
                onClick = onWake,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 24.dp)
                .fillMaxWidth(progress)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(Deck.Primary, Deck.Signal))),
        )
    }
}

@Composable
private fun DockButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    size: androidx.compose.ui.unit.Dp = 22.dp,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(size))
    }
}
