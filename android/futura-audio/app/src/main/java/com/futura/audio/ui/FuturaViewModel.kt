package com.futura.audio.ui

import android.app.Application
import android.content.Intent
import com.futura.audio.audio.TrackDecoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.futura.audio.alarm.AlarmCoordinator
import com.futura.audio.alarm.DiscreteAlarmState
import com.futura.audio.alarm.AlarmSchedulerImpl
import com.futura.audio.engine.OboePlayer
import com.futura.audio.service.FuturaAlarmService
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class FuturaViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(FuturaUiState())
    val uiState: StateFlow<FuturaUiState> = _uiState.asStateFlow()
    private val coordinator = AlarmCoordinator.getInstance()
    private var oboePlayer: OboePlayer? = null
    private var loadedTrackIndex: Int? = null
    private var activeDecodeJob: Job? = null

    init {
        viewModelScope.launch {
            while (true) {
                delay(1_000)
                tick()
            }
        }
        viewModelScope.launch {
            coordinator.alarmState.collect { alarmState ->
                when (alarmState) {
                    is DiscreteAlarmState.Firing -> {
                        oboePlayer?.stop()
                        loadedTrackIndex = null
                        _uiState.update {
                            it.copy(
                                screen = FuturaScreen.ALARM,
                                snoozeEndsAt = null,
                                trackElapsedSeconds = 0,
                                playing = false,
                            )
                        }
                    }

                    DiscreteAlarmState.Disarmed -> {
                        oboePlayer?.stop()
                        loadedTrackIndex = null
                        _uiState.update {
                            it.copy(
                                screen = FuturaScreen.LOCKED,
                                snoozeEndsAt = null,
                                playing = false,
                            )
                        }
                    }

                    DiscreteAlarmState.Armed,
                    is DiscreteAlarmState.QueueActive -> Unit
                }
            }
        }
    }

    fun snooze() {
        val nextTrackIndex = Random.nextInt(ROULETTE_QUEUE.size)
        val track = ROULETTE_QUEUE[nextTrackIndex]
        coordinator.onSnoozeTriggered(
            trackId = track.id,
            isLossless = true,
        )
        updateWithTransition {
            it.copy(
                screen = FuturaScreen.DASHBOARD,
                snoozeEndsAt = it.elapsedSeconds + SNOOZE_DURATION_SECONDS,
                trackIndex = nextTrackIndex,
                trackElapsedSeconds = 0,
                playing = false,
            )
        }
        if (loadedTrackIndex == nextTrackIndex) {
            startLoadedTrack(nextTrackIndex)
        }
        sendServiceAction(FuturaAlarmService.ACTION_SNOOZE_TO_PLAYER)
    }

    fun dismiss() {
        oboePlayer?.stop()
        loadedTrackIndex = null
        coordinator.onDismissTriggered()
        updateWithTransition {
            it.copy(
                screen = FuturaScreen.LOCKED,
                snoozeEndsAt = null,
                playing = false,
            )
        }
        sendServiceAction(FuturaAlarmService.ACTION_DISMISS_ALARM)
    }

    fun wakeNow() {
        oboePlayer?.stop()
        loadedTrackIndex = null
        AlarmSchedulerImpl(getApplication<Application>()).cancelAlarm()
        coordinator.onAlarmTriggered(System.currentTimeMillis())
        updateWithTransition {
            it.copy(
                screen = FuturaScreen.ALARM,
                snoozeEndsAt = null,
                trackElapsedSeconds = 0,
                playing = false,
            )
        }
        FuturaAlarmService.start(getApplication<Application>())
    }

    fun rearmDemo() {
        coordinator.resetToArmed()
        updateWithTransition {
            FuturaUiState(
                elapsedSeconds = it.elapsedSeconds,
            )
        }
    }

    fun togglePlay() {
        val state = _uiState.value
        if (loadedTrackIndex != state.trackIndex) return

        val player = oboePlayer ?: return
        if (state.playing) {
            player.pause()
            _uiState.update { it.copy(playing = false) }
        } else {
            player.play()
            _uiState.update { it.copy(playing = player.isPlaying()) }
        }
    }

    fun skip(direction: Int) {
        require(direction == -1 || direction == 1)
        oboePlayer?.stop()
        loadedTrackIndex = null
        _uiState.update { state ->
            state.copy(
                trackIndex = (state.trackIndex + direction + ROULETTE_QUEUE.size) %
                    ROULETTE_QUEUE.size,
                trackElapsedSeconds = 0,
                playing = false,
            )
        }
    }

    fun selectTrack(trackIndex: Int) {
        require(trackIndex in ROULETTE_QUEUE.indices)
        if (trackIndex == _uiState.value.trackIndex) return
        oboePlayer?.stop()
        loadedTrackIndex = null
        _uiState.update {
            it.copy(trackIndex = trackIndex, trackElapsedSeconds = 0, playing = false)
        }
    }

    /**
     * Feeds decoded, interleaved Float32 stereo PCM at 44.1 kHz into the Oboe engine.
     * No audio assets or decoder were present in the supplied project snapshot.
     */
    fun loadTrackPcm(trackIndex: Int, pcmData: FloatArray) {
        require(trackIndex in ROULETTE_QUEUE.indices)
        val player = oboePlayer ?: OboePlayer().also { oboePlayer = it }
        player.loadTrack(pcmData)
        loadedTrackIndex = trackIndex
        if (_uiState.value.screen == FuturaScreen.DASHBOARD &&
            _uiState.value.trackIndex == trackIndex
        ) {
            player.play()
            _uiState.update { it.copy(playing = player.isPlaying(), trackElapsedSeconds = 0) }
        }
    }

    fun loadTrackUri(
        trackIndex: Int,
        uriString: String,
        onComplete: (Result<Unit>) -> Unit,
    ) {
        require(trackIndex in ROULETTE_QUEUE.indices)
        activeDecodeJob?.cancel()
        oboePlayer?.stop()
        loadedTrackIndex = null
        activeDecodeJob = viewModelScope.launch {
            val decoded = TrackDecoder(getApplication<Application>())
                .decodeUriToFloatArray(uriString)
            val loadResult = decoded.mapCatching { pcmData ->
                val player = oboePlayer ?: OboePlayer().also { oboePlayer = it }
                player.loadTrack(pcmData)
                loadedTrackIndex = trackIndex
                if (_uiState.value.screen == FuturaScreen.DASHBOARD &&
                    _uiState.value.trackIndex == trackIndex
                ) {
                    player.play()
                    _uiState.update {
                        it.copy(
                            playing = player.isPlaying(),
                            trackElapsedSeconds = 0,
                        )
                    }
                }
            }
            onComplete(loadResult)
        }
    }

    private fun startLoadedTrack(trackIndex: Int) {
        val player = oboePlayer ?: return
        if (loadedTrackIndex != trackIndex) return
        player.play()
        _uiState.update { it.copy(playing = player.isPlaying()) }
    }

    private fun tick() {
        val player = oboePlayer
        val previous = _uiState.value
        val elapsed = previous.elapsedSeconds + 1
        if (previous.screen == FuturaScreen.DASHBOARD &&
            previous.snoozeEndsAt != null &&
            elapsed >= previous.snoozeEndsAt
        ) {
            player?.stop()
            loadedTrackIndex = null
            coordinator.onAlarmTriggered(System.currentTimeMillis())
            _uiState.update {
                it.copy(
                    elapsedSeconds = it.elapsedSeconds + 1,
                    screen = FuturaScreen.ALARM,
                    snoozeEndsAt = null,
                    trackElapsedSeconds = 0,
                    playing = false,
                )
            }
            return
        }

        val trackElapsed = if (previous.screen == FuturaScreen.DASHBOARD &&
            previous.playing &&
            loadedTrackIndex == previous.trackIndex &&
            player != null
        ) {
            (player.currentPositionFrames() / PCM_SAMPLE_RATE).toInt()
        } else {
            previous.trackElapsedSeconds
        }
        if (previous.screen == FuturaScreen.DASHBOARD &&
            trackElapsed >= ROULETTE_QUEUE[previous.trackIndex].durationSeconds
        ) {
            player?.stop()
            loadedTrackIndex = null
            _uiState.update {
                it.copy(
                    elapsedSeconds = it.elapsedSeconds + 1,
                    trackIndex = (it.trackIndex + 1) % ROULETTE_QUEUE.size,
                    trackElapsedSeconds = 0,
                    playing = false,
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                elapsedSeconds = it.elapsedSeconds + 1,
                trackElapsedSeconds = trackElapsed,
                playing = it.playing && player?.isPlaying() == true,
            )
        }
    }

    private fun updateWithTransition(transform: (FuturaUiState) -> FuturaUiState) {
        _uiState.update { transform(it).copy(sliding = true) }
        viewModelScope.launch {
            delay(SLIDE_DURATION_MILLIS)
            _uiState.update { it.copy(sliding = false) }
        }
    }

    private fun sendServiceAction(action: String) {
        val intent = Intent(getApplication<Application>(), FuturaAlarmService::class.java)
            .setAction(action)
        if (action == FuturaAlarmService.ACTION_DISMISS_ALARM) {
            getApplication<Application>().startService(intent)
        } else {
            androidx.core.content.ContextCompat.startForegroundService(
                getApplication<Application>(),
                intent,
            )
        }
    }

    override fun onCleared() {
        oboePlayer?.stop()
        super.onCleared()
    }

    companion object {
        private const val PCM_SAMPLE_RATE = 44_100L
        private const val SLIDE_DURATION_MILLIS = 700L
    }
}
