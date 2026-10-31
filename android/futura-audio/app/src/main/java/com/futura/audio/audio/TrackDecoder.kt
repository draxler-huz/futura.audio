package com.futura.audio.audio

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.floor
import kotlin.math.roundToLong

class TrackDecoder(context: Context) {
    private val appContext = context.applicationContext

    suspend fun decodeUriToFloatArray(uriString: String): Result<FloatArray> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(decodeBlocking(uriString))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Result.failure(failure)
            }
        }

    private fun decodeBlocking(uriString: String): FloatArray {
        if (uriString.isBlank()) {
            throw IllegalArgumentException("A track URI is required.")
        }

        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        var codecStarted = false
        try {
            setDataSource(extractor, uriString)

            var audioTrackIndex = -1
            var sourceFormat: MediaFormat? = null
            for (trackIndex in 0 until extractor.trackCount) {
                val candidateFormat = extractor.getTrackFormat(trackIndex)
                val mime = candidateFormat.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = trackIndex
                    sourceFormat = candidateFormat
                    break
                }
            }

            if (audioTrackIndex < 0 || sourceFormat == null) {
                throw IOException("The selected URI does not contain an audio track.")
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = sourceFormat.getString(MediaFormat.KEY_MIME)
                ?: throw IOException("The audio track has no decoder MIME type.")
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(sourceFormat, null, null, 0)
            codec.start()
            codecStarted = true

            val output = StereoPcmBuffer()
            var pcmFormat = readPcmFormat(sourceFormat, sourceFormat)
            val bufferInfo = MediaCodec.BufferInfo()
            var inputEnded = false
            var outputEnded = false

            while (!outputEnded) {
                if (!inputEnded) {
                    val inputIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)
                            ?: throw IOException("The audio decoder returned no input buffer.")
                        inputBuffer.clear()
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            inputEnded = true
                        } else {
                            val sampleTimeUs = extractor.sampleTime
                            codec.queueInputBuffer(
                                inputIndex,
                                0,
                                sampleSize,
                                sampleTimeUs,
                                0,
                            )
                            extractor.advance()
                        }
                    }
                }

                when (val outputIndex = codec.dequeueOutputBuffer(bufferInfo, CODEC_TIMEOUT_US)) {
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        pcmFormat = readPcmFormat(codec.outputFormat, sourceFormat)
                    }

                    MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> Unit
                    else -> if (outputIndex >= 0) {
                        try {
                            if (bufferInfo.size > 0) {
                                val outputBuffer = codec.getOutputBuffer(outputIndex)
                                    ?: throw IOException("The audio decoder returned no output buffer.")
                                appendStereoPcm(outputBuffer, bufferInfo, pcmFormat, output)
                            }
                            outputEnded =
                                (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                        } finally {
                            codec.releaseOutputBuffer(outputIndex, false)
                        }
                    }
                }
            }

            val decoded = output.toFloatArray()
            if (decoded.isEmpty()) {
                throw IOException("The audio decoder produced no PCM samples.")
            }
            return resampleToStereo44100(decoded, pcmFormat.sampleRate)
        } finally {
            if (codecStarted) {
                try {
                    codec?.stop()
                } catch (_: IllegalStateException) {
                    // A decoder can already be stopped after a platform codec error.
                }
            }
            try {
                codec?.release()
            } finally {
                extractor.release()
            }
        }
    }

    private fun setDataSource(extractor: MediaExtractor, uriString: String) {
        if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
            extractor.setDataSource(appContext, Uri.parse(uriString), null)
        } else {
            extractor.setDataSource(uriString)
        }
    }

    private fun readPcmFormat(
        outputFormat: MediaFormat,
        sourceFormat: MediaFormat,
    ): PcmFormat {
        val sampleRate = outputFormat.integerOr(
            MediaFormat.KEY_SAMPLE_RATE,
            sourceFormat.integerOr(MediaFormat.KEY_SAMPLE_RATE, 0),
        )
        val channelCount = outputFormat.integerOr(
            MediaFormat.KEY_CHANNEL_COUNT,
            sourceFormat.integerOr(MediaFormat.KEY_CHANNEL_COUNT, 0),
        )
        val pcmEncoding = outputFormat.integerOr(
            MediaFormat.KEY_PCM_ENCODING,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (sampleRate <= 0) throw IOException("The decoded audio sample rate is invalid.")
        if (channelCount !in 1..8) throw IOException("The decoded audio channel count is unsupported.")
        bytesPerSample(pcmEncoding)
        return PcmFormat(sampleRate, channelCount, pcmEncoding)
    }

    private fun appendStereoPcm(
        buffer: ByteBuffer,
        info: MediaCodec.BufferInfo,
        format: PcmFormat,
        output: StereoPcmBuffer,
    ) {
        val bytesPerSample = bytesPerSample(format.encoding)
        val bytesPerFrame = bytesPerSample * format.channelCount
        val start = info.offset
        val end = start + info.size
        if (start < 0 || end > buffer.capacity()) {
            throw IOException("The audio decoder returned an invalid output range.")
        }

        val pcm = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN).apply {
            position(start)
            limit(end)
        }
        val completeFrames = pcm.remaining() / bytesPerFrame
        repeat(completeFrames) {
            val first = readSample(pcm, format.encoding)
            val second = if (format.channelCount == 1) first else readSample(pcm, format.encoding)
            for (channel in 2 until format.channelCount) {
                readSample(pcm, format.encoding)
            }
            output.append(first, second)
        }
    }

    private fun readSample(buffer: ByteBuffer, encoding: Int): Float =
        when (encoding) {
            AudioFormat.ENCODING_PCM_FLOAT -> buffer.float.coerceIn(-1f, 1f)
            AudioFormat.ENCODING_PCM_16BIT -> buffer.short / 32768f
            AudioFormat.ENCODING_PCM_8BIT -> {
                val unsignedSample = buffer.get().toInt() and 0xFF
                (unsignedSample - 128) / 128f
            }
            AudioFormat.ENCODING_PCM_24BIT_PACKED -> {
                val b0 = buffer.get().toInt() and 0xFF
                val b1 = buffer.get().toInt() and 0xFF
                val b2 = buffer.get().toInt() and 0xFF
                var sample = b0 or (b1 shl 8) or (b2 shl 16)
                if ((sample and 0x800000) != 0) sample = sample or -0x1000000
                (sample / 8_388_608f).coerceIn(-1f, 1f)
            }
            AudioFormat.ENCODING_PCM_32BIT ->
                (buffer.int / 2_147_483_648f).coerceIn(-1f, 1f)
            else -> throw IOException("The decoder returned an unsupported PCM encoding: $encoding")
        }

    private fun bytesPerSample(encoding: Int): Int =
        when (encoding) {
            AudioFormat.ENCODING_PCM_8BIT -> 1
            AudioFormat.ENCODING_PCM_16BIT -> 2
            AudioFormat.ENCODING_PCM_24BIT_PACKED -> 3
            AudioFormat.ENCODING_PCM_FLOAT,
            AudioFormat.ENCODING_PCM_32BIT -> 4
            else -> throw IOException("The decoder returned an unsupported PCM encoding: $encoding")
        }

    private fun resampleToStereo44100(source: FloatArray, sourceRate: Int): FloatArray {
        if (sourceRate == OUTPUT_SAMPLE_RATE) return source

        val sourceFrames = source.size / OUTPUT_CHANNEL_COUNT
        if (sourceFrames == 0) throw IOException("The decoded audio contains no complete PCM frames.")
        val outputFrames = (sourceFrames.toDouble() * OUTPUT_SAMPLE_RATE / sourceRate)
            .roundToLong()
            .coerceAtLeast(1L)
        if (outputFrames > Int.MAX_VALUE / OUTPUT_CHANNEL_COUNT) {
            throw IOException("The decoded track is too large to buffer in memory.")
        }

        val resampled = FloatArray(outputFrames.toInt() * OUTPUT_CHANNEL_COUNT)
        for (frame in 0 until outputFrames.toInt()) {
            val sourcePosition = frame.toDouble() * sourceRate / OUTPUT_SAMPLE_RATE
            val leftFrame = floor(sourcePosition).toInt().coerceAtMost(sourceFrames - 1)
            val rightFrame = (leftFrame + 1).coerceAtMost(sourceFrames - 1)
            val blend = (sourcePosition - leftFrame).toFloat().coerceIn(0f, 1f)
            for (channel in 0 until OUTPUT_CHANNEL_COUNT) {
                val left = source[leftFrame * OUTPUT_CHANNEL_COUNT + channel]
                val right = source[rightFrame * OUTPUT_CHANNEL_COUNT + channel]
                resampled[frame * OUTPUT_CHANNEL_COUNT + channel] =
                    left + (right - left) * blend
            }
        }
        return resampled
    }

    private fun MediaFormat.integerOr(key: String, fallback: Int): Int =
        if (containsKey(key)) getInteger(key) else fallback

    private data class PcmFormat(
        val sampleRate: Int,
        val channelCount: Int,
        val encoding: Int,
    )

    private class StereoPcmBuffer {
        private var samples = FloatArray(INITIAL_CAPACITY)
        private var size = 0

        fun append(left: Float, right: Float) {
            ensureCapacity(size + OUTPUT_CHANNEL_COUNT)
            samples[size++] = left
            samples[size++] = right
        }

        fun toFloatArray(): FloatArray = samples.copyOf(size)

        private fun ensureCapacity(requiredCapacity: Int) {
            if (requiredCapacity < 0) throw IOException("The decoded track is too large.")
            if (requiredCapacity <= samples.size) return
            var nextCapacity = samples.size
            while (nextCapacity < requiredCapacity) {
                if (nextCapacity > Int.MAX_VALUE / 2) {
                    nextCapacity = Int.MAX_VALUE
                    break
                }
                nextCapacity *= 2
            }
            if (nextCapacity < requiredCapacity) {
                throw IOException("The decoded track is too large to buffer in memory.")
            }
            samples = samples.copyOf(nextCapacity)
        }
    }

    companion object {
        private const val CODEC_TIMEOUT_US = 10_000L
        private const val OUTPUT_SAMPLE_RATE = 44_100
        private const val OUTPUT_CHANNEL_COUNT = 2
        private const val INITIAL_CAPACITY = 16_384
    }
}
