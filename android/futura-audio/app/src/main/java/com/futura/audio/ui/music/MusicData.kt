package com.futura.audio.ui.music

import androidx.annotation.DrawableRes
import com.futura.audio.R
import com.futura.audio.ui.library.LibraryCatalog
import com.futura.audio.ui.library.LibraryTrack
import com.futura.audio.ui.library.moodOf
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

data class AudioFormat(val codec: String, val bitDepth: Int, val sampleRateKhz: Double)

/** voice is "A", "B", or "both" (sung together). */
data class LyricLine(val start: Float, val voice: String, val text: String, val translation: String)

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    @DrawableRes val art: Int,
    val duration: Float,
    val format: AudioFormat,
    val bpm: Int,
    val key: String,
    val accent: Long,
    val moods: List<String>,
    val energy: Float,
    val valence: Float,
    val voiceA: String?,
    val voiceB: String?,
    val lyrics: List<LyricLine>?,
    val related: List<String>,
)

data class TimedWord(val text: String, val start: Float, val end: Float)
data class TimedLine(val line: LyricLine, val end: Float, val words: List<TimedWord>)

private fun lyric(start: Float, voice: String, text: String, translation: String) =
    LyricLine(start, voice, text, translation)

private val NEON_MERIDIAN_LYRICS = listOf(
    lyric(8f, "A", "Static on the skyline, I can hear you breathing", "Estática en el horizonte, puedo oírte respirar"),
    lyric(14f, "A", "Every signal fading to a single frequency", "Cada señal se apaga en una sola frecuencia"),
    lyric(20f, "B", "I was counting satellites to find your name", "Contaba satélites para encontrar tu nombre"),
    lyric(26f, "B", "Tracing every orbit back to where it came", "Siguiendo cada órbita hasta su origen"),
    lyric(32f, "both", "Meet me at the meridian", "Encuéntrame en el meridiano"),
    lyric(37f, "both", "Where the city lights begin", "Donde empiezan las luces de la ciudad"),
    lyric(42f, "A", "Hold the line, hold the light", "Sostén la línea, sostén la luz"),
    lyric(47f, "B", "We're the echo in the night", "Somos el eco en la noche"),
    lyric(56f, "A", "Chrome reflections on the water", "Reflejos de cromo sobre el agua"),
    lyric(62f, "A", "Turning seconds into hours", "Convirtiendo segundos en horas"),
    lyric(68f, "B", "Every word you never said", "Cada palabra que nunca dijiste"),
    lyric(74f, "B", "Plays in loops inside my head", "Se repite en bucle en mi cabeza"),
    lyric(80f, "both", "Meet me at the meridian", "Encuéntrame en el meridiano"),
    lyric(85f, "both", "Where the city lights begin", "Donde empiezan las luces de la ciudad"),
    lyric(90f, "A", "Hold the line, hold the light", "Sostén la línea, sostén la luz"),
    lyric(95f, "B", "We're the echo in the night", "Somos el eco en la noche"),
    lyric(110f, "B", "If the power ever fails", "Si la energía alguna vez falla"),
    lyric(116f, "A", "I will find you by the trails", "Te encontraré por los rastros"),
    lyric(122f, "B", "Of the neon that we left", "Del neón que dejamos atrás"),
    lyric(128f, "A", "Burning softly in my chest", "Ardiendo suave en mi pecho"),
    lyric(134f, "both", "Meet me at the meridian", "Encuéntrame en el meridiano"),
    lyric(140f, "both", "Where the city lights begin", "Donde empiezan las luces de la ciudad"),
    lyric(146f, "A", "Hold the line", "Sostén la línea"),
    lyric(150f, "B", "Hold the light", "Sostén la luz"),
    lyric(154f, "both", "We're the echo in the night", "Somos el eco en la noche"),
    lyric(165f, "A", "Static on the skyline", "Estática en el horizonte"),
    lyric(172f, "B", "Fading to a frequency", "Desvaneciéndose en una frecuencia"),
    lyric(180f, "both", "Meet me at the meridian", "Encuéntrame en el meridiano"),
    lyric(188f, "both", "Meridian", "Meridiano"),
)

private val GLASS_ORBIT_LYRICS = listOf(
    lyric(12f, "A", "Light bends where you used to stand", "La luz se curva donde solías estar"),
    lyric(19f, "A", "Prisms melting in my hand", "Prismas derritiéndose en mi mano"),
    lyric(26f, "A", "We are glass in slow rotation", "Somos cristal en rotación lenta"),
    lyric(33f, "A", "Fragile, bright, in calculation", "Frágiles, brillantes, en cálculo"),
    lyric(44f, "A", "Stay in orbit, stay in orbit", "Quédate en órbita, quédate en órbita"),
    lyric(51f, "A", "Don't let gravity decide", "No dejes que la gravedad decida"),
    lyric(62f, "A", "Every turn a softer landing", "Cada giro, un aterrizaje más suave"),
    lyric(69f, "A", "Every arc a place to hide", "Cada arco, un lugar para esconderse"),
    lyric(84f, "A", "Stay in orbit", "Quédate en órbita"),
    lyric(90f, "A", "Stay in orbit with me", "Quédate en órbita conmigo"),
)

object MusicCatalog {
    /** Index-aligned with ROULETTE_QUEUE, which the view model plays from. */
    val QUEUE_TRACK_IDS = listOf("neon-meridian", "glass-orbit", "amber-signal", "deep-current", "night-protocol")

    /** Every library entry in playback order. The index matches the view model's track index. */
    fun queueTracks(): List<Track> = LibraryCatalog.tracks.map { trackFor(it) }

    private fun trackFor(lib: LibraryTrack): Track =
        tracks.firstOrNull { it.id == lib.id } ?: Track(
            id = lib.id,
            title = lib.title,
            artist = lib.artist,
            album = lib.album,
            art = R.drawable.art_silent_array, // placeholder: library entries have no artwork yet
            duration = lib.durationSeconds.toFloat(),
            format = AudioFormat("FLAC", 16, 44.1), // placeholder: the library has no format data yet
            bpm = (80 + lib.energy * 60).roundToInt(), // estimated from energy, not measured
            key = "—",
            accent = accentFor(lib.family),
            moods = listOf(moodOf(lib)),
            energy = lib.energy,
            valence = lib.valence,
            voiceA = lib.artist,
            voiceB = null,
            lyrics = null,
            related = emptyList(),
        )

    private fun accentFor(family: String): Long = when (family) {
        "Chill" -> 0xFF00FF66L
        "Indie" -> 0xFFD946EFL
        "Soul" -> 0xFFFF9F1CL
        else -> 0xFF00E5FFL
    }

    /** Catalog relations when the track has them, otherwise the closest tracks in energy. */
    fun relatedFor(current: Track, queue: List<Track>): List<Track> =
        if (current.related.isNotEmpty()) {
            current.related.map { track(it) }
        } else {
            queue.filter { it.id != current.id }.sortedBy { abs(it.energy - current.energy) }.take(3)
        }

    const val INITIAL_TRACK_ID = "neon-meridian"
    val INITIAL_QUEUE_IDS = listOf("glass-orbit", "amber-signal", "deep-current")
    const val INITIAL_TIME = 134f

    val tracks: List<Track> = listOf(
        Track(
            id = "neon-meridian", title = "Neon Meridian", artist = "Vela Kade & Oris", album = "Signal Bloom",
            art = R.drawable.art_neon_meridian, duration = 236f,
            format = AudioFormat("FLAC", 24, 96.0), bpm = 118, key = "F# min", accent = 0xFF00E5FFL,
            moods = listOf("Nocturnal", "Euphoric", "Driving", "Chrome-pop"), energy = 0.78f, valence = 0.62f,
            voiceA = "Vela Kade", voiceB = "Oris", lyrics = NEON_MERIDIAN_LYRICS,
            related = listOf("night-protocol", "glass-orbit", "silent-array"),
        ),
        Track(
            id = "glass-orbit", title = "Glass Orbit", artist = "Lumen Arc", album = "Refraction",
            art = R.drawable.art_glass_orbit, duration = 212f,
            format = AudioFormat("FLAC", 24, 192.0), bpm = 104, key = "D maj", accent = 0xFFD946EFL,
            moods = listOf("Dreamy", "Weightless", "Synthwave"), energy = 0.52f, valence = 0.71f,
            voiceA = "Lumen Arc", voiceB = null, lyrics = GLASS_ORBIT_LYRICS,
            related = listOf("neon-meridian", "deep-current", "silent-array"),
        ),
        Track(
            id = "amber-signal", title = "Amber Signal", artist = "Kōdo", album = "Topograph",
            art = R.drawable.art_amber_signal, duration = 254f,
            format = AudioFormat("ALAC", 24, 48.0), bpm = 126, key = "A min", accent = 0xFFFF9F1CL,
            moods = listOf("Warm", "Hypnotic", "Club"), energy = 0.84f, valence = 0.48f,
            voiceA = null, voiceB = null, lyrics = null,
            related = listOf("night-protocol", "neon-meridian", "deep-current"),
        ),
        Track(
            id = "deep-current", title = "Deep Current", artist = "Marin Sølv", album = "Abyssal",
            art = R.drawable.art_deep_current, duration = 301f,
            format = AudioFormat("FLAC", 16, 44.1), bpm = 92, key = "C# min", accent = 0xFF00FF66L,
            moods = listOf("Ambient", "Submerged", "Focus"), energy = 0.34f, valence = 0.4f,
            voiceA = null, voiceB = null, lyrics = null,
            related = listOf("glass-orbit", "silent-array", "amber-signal"),
        ),
        Track(
            id = "night-protocol", title = "Night Protocol", artist = "Hexa Drive", album = "Lattice",
            art = R.drawable.art_night_protocol, duration = 228f,
            format = AudioFormat("FLAC", 24, 96.0), bpm = 122, key = "E min", accent = 0xFF38BDF8L,
            moods = listOf("Cyber", "Tense", "Driving"), energy = 0.81f, valence = 0.35f,
            voiceA = null, voiceB = null, lyrics = null,
            related = listOf("neon-meridian", "amber-signal", "silent-array"),
        ),
        Track(
            id = "silent-array", title = "Silent Array", artist = "Oris", album = "Monolith Hours",
            art = R.drawable.art_silent_array, duration = 267f,
            format = AudioFormat("FLAC", 24, 48.0), bpm = 86, key = "Bb maj", accent = 0xFFF43F5EL,
            moods = listOf("Minimal", "Cinematic", "Late night"), energy = 0.41f, valence = 0.29f,
            voiceA = null, voiceB = null, lyrics = null,
            related = listOf("deep-current", "neon-meridian", "glass-orbit"),
        ),
    )

    private val index: Map<String, Track> = tracks.associateBy { it.id }

    fun track(id: String): Track = index.getValue(id)
}

fun formatTime(totalSeconds: Float): String {
    val safe = max(0, floor(totalSeconds.toDouble()).toInt())
    return "%02d:%02d".format(safe / 60, safe % 60)
}

private fun khz(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

fun formatBadge(format: AudioFormat): String =
    "${format.codec} • ${format.bitDepth}-bit / ${khz(format.sampleRateKhz)}kHz"

private const val MASK32 = 0xFFFFFFFFL

private fun imul(a: Long, b: Long): Long = ((a and MASK32) * (b and MASK32)) and MASK32

/** Deterministic waveform heights (0..1) for a track, matching the prototype's generatePeaks. */
fun generatePeaks(seed: String, count: Int): FloatArray {
    var h = 2166136261L
    for (c in seed) {
        h = h xor c.code.toLong()
        h = imul(h, 16777619L)
    }
    var state = h and MASK32
    fun random(): Double {
        state = (state + 0x6d2b79f5L) and MASK32
        var t = state
        t = imul(t xor (t ushr 15), t or 1L)
        t = (t xor ((t + imul(t xor (t ushr 7), t or 61L)) and MASK32)) and MASK32
        val r = (t xor (t ushr 14)) and MASK32
        return r.toDouble() / 4294967296.0
    }
    return FloatArray(count) { i ->
        val x = i.toDouble() / count
        val envelope = 0.35 + 0.65 * abs(sin(x * PI * 3.2 + 0.4)) * (0.6 + 0.4 * sin(x * PI))
        min(1.0, max(0.08, envelope * (0.55 + random() * 0.45))).toFloat()
    }
}

/** Splits each lyric line into timed words, matching the prototype's buildTimedLyrics. */
fun buildTimedLyrics(lines: List<LyricLine>, duration: Float): List<TimedLine> =
    lines.mapIndexed { i, line ->
        val nextStart = lines.getOrNull(i + 1)?.start ?: duration
        val end = min(nextStart - 0.2f, line.start + 5.5f)
        val span = (end - line.start) * 0.9f
        val tokens = line.text.split(" ")
        val totalChars = tokens.sumOf { it.length + 1 }
        var cursor = line.start
        val words = tokens.map { text ->
            val d = span * (text.length + 1) / totalChars
            val word = TimedWord(text, cursor, cursor + d)
            cursor += d
            word
        }
        TimedLine(line, end, words)
    }
