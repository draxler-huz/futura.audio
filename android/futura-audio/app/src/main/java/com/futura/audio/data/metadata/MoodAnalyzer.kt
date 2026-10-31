package com.futura.audio.data.metadata

import android.content.Context
import android.net.Uri
import com.futura.audio.audio.TrackDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Approximate mood features from PCM via [TrackDecoder].
 *
 * - **Energy** from RMS + spectral centroid of mono-mixed frames, mapped to 0..1.
 * - **Valence** from a coarse major/minor chroma heuristic (not a full key detector).
 *
 * Approximations: only the first [MAX_ANALYSIS_SAMPLES] stereo samples are analyzed
 * to keep indexing bounded; chroma uses a simplified Goertzel set at A440 pitch classes.
 */
class MoodAnalyzer(context: Context) {
    private val decoder = TrackDecoder(context)

    data class Mood(
        val energy: Float,
        val valence: Float,
        val highEnergy: Boolean,
        val positiveValence: Boolean,
    )

    suspend fun analyzeFile(file: File): Mood = withContext(Dispatchers.IO) {
        val uri = Uri.fromFile(file).toString()
        analyzeUri(uri)
    }

    suspend fun analyzeUri(uriString: String): Mood = withContext(Dispatchers.IO) {
        val pcm = decoder.decodeUriToFloatArray(uriString).getOrNull()
            ?: return@withContext DEFAULT_MOOD
        analyzePcm(pcm)
    }

    fun analyzePcm(stereoPcm: FloatArray): Mood {
        if (stereoPcm.size < 4) return DEFAULT_MOOD
        val limited = if (stereoPcm.size > MAX_ANALYSIS_SAMPLES) {
            stereoPcm.copyOf(MAX_ANALYSIS_SAMPLES)
        } else {
            stereoPcm
        }
        val mono = FloatArray(limited.size / 2)
        for (i in mono.indices) {
            mono[i] = 0.5f * (limited[i * 2] + limited[i * 2 + 1])
        }
        val energy = computeEnergy(mono)
        val valence = computeValence(mono)
        return Mood(
            energy = energy,
            valence = valence,
            highEnergy = energy >= HIGH_ENERGY_THRESHOLD,
            positiveValence = valence >= POSITIVE_VALENCE_THRESHOLD,
        )
    }

    /**
     * Energy ≈ blend of RMS loudness and spectral centroid brightness, logistic-mapped to 0..1.
     */
    private fun computeEnergy(mono: FloatArray): Float {
        var sumSq = 0.0
        for (s in mono) sumSq += (s * s).toDouble()
        val rms = sqrt(sumSq / mono.size).toFloat()
        val centroid = spectralCentroidHz(mono, SAMPLE_RATE)
        // Heuristic normalization: RMS ~0.2 and centroid ~4 kHz → mid energy.
        val rmsNorm = min(1f, rms / 0.25f)
        val centNorm = min(1f, centroid / 6000f)
        val raw = 0.65f * rmsNorm + 0.35f * centNorm
        return raw.coerceIn(0f, 1f)
    }

    /**
     * Valence approximation: compare major vs minor triad energy in a 12-bin chroma vector.
     * Positive when major-ish components dominate; not a reliable key finder.
     */
    private fun computeValence(mono: FloatArray): Float {
        val chroma = chromaVector(mono, SAMPLE_RATE)
        // Average major (0,4,7) vs minor (0,3,7) relative strengths across roots.
        var major = 0.0
        var minor = 0.0
        for (root in 0 until 12) {
            val m = chroma[root] + chroma[(root + 4) % 12] + chroma[(root + 7) % 12]
            val n = chroma[root] + chroma[(root + 3) % 12] + chroma[(root + 7) % 12]
            major += m
            minor += n
        }
        val denom = major + minor
        if (denom <= 1e-9) return 0.5f
        // Map major share to 0..1 around 0.5.
        return (major / denom).toFloat().coerceIn(0f, 1f)
    }

    private fun spectralCentroidHz(mono: FloatArray, sampleRate: Int): Float {
        val n = min(mono.size, FFT_SIZE)
        if (n < 32) return 0f
        // Lightweight DFT magnitude on a downsampled window (approximation).
        val hop = max(1, n / FFT_SIZE)
        val window = FloatArray(FFT_SIZE)
        var wi = 0
        var i = 0
        while (wi < FFT_SIZE && i < mono.size) {
            window[wi++] = mono[i]
            i += hop
        }
        var weighted = 0.0
        var total = 0.0
        val bins = FFT_SIZE / 2
        for (k in 1 until bins) {
            val re = goertzelReal(window, k, FFT_SIZE)
            val im = goertzelImag(window, k, FFT_SIZE)
            val mag = sqrt(re * re + im * im)
            val freq = k.toDouble() * sampleRate / FFT_SIZE
            weighted += mag * freq
            total += mag
        }
        if (total <= 1e-12) return 0f
        return (weighted / total).toFloat()
    }

    private fun chromaVector(mono: FloatArray, sampleRate: Int): DoubleArray {
        val chroma = DoubleArray(12)
        val n = min(mono.size, CHROMA_SAMPLES)
        if (n < 64) return chroma
        val slice = if (mono.size > n) mono.copyOf(n) else mono
        // Analyze one octave around mid-range fundamentals (C3..B4 approx).
        for (pc in 0 until 12) {
            var sum = 0.0
            for (oct in 3..5) {
                val midi = 12 * oct + pc
                val freq = 440.0 * Math.pow(2.0, (midi - 69) / 12.0)
                if (freq >= sampleRate / 2.0) continue
                sum += goertzelMagnitude(slice, freq, sampleRate)
            }
            chroma[pc] = sum
        }
        // L1 normalize
        val s = chroma.sum()
        if (s > 1e-12) {
            for (i in chroma.indices) chroma[i] /= s
        }
        return chroma
    }

    private fun goertzelMagnitude(samples: FloatArray, freq: Double, sampleRate: Int): Double {
        val n = samples.size
        val k = (0.5 + (n * freq) / sampleRate).toInt()
        val omega = (2.0 * PI * k) / n
        val cosine = cos(omega)
        val sine = sin(omega)
        val coeff = 2.0 * cosine
        var q0: Double
        var q1 = 0.0
        var q2 = 0.0
        for (i in 0 until n) {
            q0 = coeff * q1 - q2 + samples[i]
            q2 = q1
            q1 = q0
        }
        val real = q1 - q2 * cosine
        val imag = q2 * sine
        return sqrt(real * real + imag * imag)
    }

    private fun goertzelReal(samples: FloatArray, k: Int, n: Int): Double {
        val omega = (2.0 * PI * k) / n
        val cosine = cos(omega)
        val coeff = 2.0 * cosine
        var q0: Double
        var q1 = 0.0
        var q2 = 0.0
        for (i in 0 until n) {
            q0 = coeff * q1 - q2 + samples[i]
            q2 = q1
            q1 = q0
        }
        return q1 - q2 * cosine
    }

    private fun goertzelImag(samples: FloatArray, k: Int, n: Int): Double {
        val omega = (2.0 * PI * k) / n
        val sine = sin(omega)
        val cosine = cos(omega)
        val coeff = 2.0 * cosine
        var q0: Double
        var q1 = 0.0
        var q2 = 0.0
        for (i in 0 until n) {
            q0 = coeff * q1 - q2 + samples[i]
            q2 = q1
            q1 = q0
        }
        return q2 * sine
    }

    companion object {
        const val HIGH_ENERGY_THRESHOLD = 0.5f
        const val POSITIVE_VALENCE_THRESHOLD = 0.5f
        private const val SAMPLE_RATE = 44_100
        private const val FFT_SIZE = 512
        private const val CHROMA_SAMPLES = 44_100 // ~1s mono
        /** Stereo float samples (~ first 15s @ 44.1 kHz stereo). */
        private const val MAX_ANALYSIS_SAMPLES = 44_100 * 2 * 15
        private val DEFAULT_MOOD = Mood(
            energy = 0.5f,
            valence = 0.5f,
            highEnergy = false,
            positiveValence = false,
        )
    }
}
