package com.futura.audio.data.metadata

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Embedded lyrics (via [MetadataExtractor]) plus sibling `.lrc` files with timecodes.
 */
class LyricsExtractor(
    private val metadataExtractor: MetadataExtractor,
) {

    data class LrcLine(
        val timeMs: Long,
        val text: String,
    )

    data class Lyrics(
        /** Plain or LRC-formatted text suitable for storage on [com.futura.audio.data.db.entity.TrackEntity.lyrics]. */
        val text: String?,
        val timedLines: List<LrcLine>,
        val source: Source,
    )

    enum class Source { EMBEDDED, LRC, NONE }

    suspend fun extract(file: File, embeddedLyrics: String? = null): Lyrics =
        withContext(Dispatchers.IO) {
            val embedded = embeddedLyrics
                ?: metadataExtractor.extractBlocking(file).lyrics
            val lrcFile = siblingLrc(file)
            val lrcText = lrcFile?.takeIf { it.isFile && it.canRead() }
                ?.readText(Charsets.UTF_8)
            val timed = lrcText?.let { parseLrc(it) }.orEmpty()
            when {
                timed.isNotEmpty() -> Lyrics(
                    text = lrcText,
                    timedLines = timed,
                    source = Source.LRC,
                )
                !embedded.isNullOrBlank() -> Lyrics(
                    text = embedded,
                    timedLines = parseLrc(embedded),
                    source = Source.EMBEDDED,
                )
                else -> Lyrics(null, emptyList(), Source.NONE)
            }
        }

    fun parseLrc(text: String): List<LrcLine> {
        val result = ArrayList<LrcLine>()
        // [mm:ss.xx] or [mm:ss.xxx]
        val tag = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?]""")
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            val matches = tag.findAll(line).toList()
            if (matches.isEmpty()) continue
            val lyricText = tag.replace(line, "").trim()
            if (lyricText.isEmpty()) continue
            for (m in matches) {
                val mm = m.groupValues[1].toLongOrNull() ?: continue
                val ss = m.groupValues[2].toLongOrNull() ?: continue
                val frac = m.groupValues[3]
                val ms = when {
                    frac.isNullOrEmpty() -> 0L
                    frac.length == 1 -> frac.toLong() * 100L
                    frac.length == 2 -> frac.toLong() * 10L
                    else -> frac.take(3).toLong()
                }
                result += LrcLine(timeMs = mm * 60_000L + ss * 1000L + ms, text = lyricText)
            }
        }
        return result.sortedBy { it.timeMs }
    }

    private fun siblingLrc(audio: File): File? {
        val base = audio.nameWithoutExtension
        val dir = audio.parentFile ?: return null
        val candidates = listOf(
            File(dir, "$base.lrc"),
            File(dir, "$base.LRC"),
        )
        return candidates.firstOrNull { it.isFile }
    }
}
