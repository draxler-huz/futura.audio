package com.futura.audio.engine

class OboePlayer {
    companion object {
        private const val CHANNEL_COUNT = 2

        init {
            System.loadLibrary("futura_audio")
        }
    }

    init {
        nativeInit()
    }

    private external fun nativeInit(): Boolean
    private external fun nativeSetDataSource(pcmData: FloatArray)
    private external fun nativePlay()
    private external fun nativePause()
    private external fun nativeStop()
    private external fun nativeIsPlaying(): Boolean
    private external fun nativeGetCurrentPosition(): Long

    fun loadTrack(pcmBuffer: FloatArray) {
        require(pcmBuffer.size % CHANNEL_COUNT == 0) {
            "PCM must be interleaved stereo Float32 at 44.1 kHz."
        }
        nativeSetDataSource(pcmBuffer)
    }

    fun play() = nativePlay()
    fun pause() = nativePause()
    fun stop() = nativeStop()
    fun isPlaying(): Boolean = nativeIsPlaying()
    fun currentPositionFrames(): Long = nativeGetCurrentPosition()

    fun isInitialized(): Boolean = nativeInit()
}
