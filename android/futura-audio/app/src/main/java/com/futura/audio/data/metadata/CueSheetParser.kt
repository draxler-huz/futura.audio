package com.futura.audio.data.metadata

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Minimal CUE sheet parser for FILE / TRACK / INDEX / TITLE / PERFORMER lines.
 */
object CueSheetParser {

    data class CueIndex(
        val number: Int,
        /** INDEX time as mm:ss:ff (CD frames). */
        val time: String,
        val timeMs: Long,
    )

    data class CueTrack(
        val number: Int,
        val title: String?,
        val performer: String?,
        val indices: List<CueIndex>,
    )

    data class CueSheet(
        val fileName: String?,
        val fileType: String?,
        val title: String?,
        val performer: String?,
        val tracks: List<CueTrack>,
    )

    suspend fun parse(file: File): CueSheet? = withContext(Dispatchers.IO) {
        if (!file.isFile || !file.canRead()) return@withContext null
        parse(file.readText(Charsets.UTF_8))
    }

    fun parse(text: String): CueSheet? {
        if (text.isBlank()) return null
        var fileName: String? = null
        var fileType: String? = null
        var sheetTitle: String? = null
        var sheetPerformer: String? = null
        val tracks = mutableListOf<CueTrack>()
        var currentNumber: Int? = null
        var currentTitle: String? = null
        var currentPerformer: String? = null
        var currentIndices = mutableListOf<CueIndex>()

        fun flushTrack() {
            val n = currentNumber ?: return
            tracks += CueTrack(n, currentTitle, currentPerformer, currentIndices.toList())
            currentNumber = null
            currentTitle = null
            currentPerformer = null
            currentIndices = mutableListOf()
        }

        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("REM ", ignoreCase = true)) continue
            when {
                line.startsWith("FILE ", ignoreCase = true) -> {
                    val rest = line.substring(5).trim()
                    val quoted = quotedValue(rest)
                    fileName = quoted ?: rest.substringBefore(' ').trim().trim('"')
                    fileType = rest.substringAfterLast(' ').trim().takeIf {
                        it.isNotEmpty() && !it.equals(fileName, ignoreCase = true)
                    }
                }
                line.startsWith("TRACK ", ignoreCase = true) -> {
                    flushTrack()
                    currentNumber = line.split(Regex("\\s+")).getOrNull(1)?.toIntOrNull()
                }
                line.startsWith("TITLE ", ignoreCase = true) -> {
                    val v = quotedValue(line.substring(6).trim())
                    if (currentNumber != null) currentTitle = v else sheetTitle = v
                }
                line.startsWith("PERFORMER ", ignoreCase = true) -> {
                    val v = quotedValue(line.substring(10).trim())
                    if (currentNumber != null) currentPerformer = v else sheetPerformer = v
                }
                line.startsWith("INDEX ", ignoreCase = true) -> {
                    val parts = line.split(Regex("\\s+"))
                    val num = parts.getOrNull(1)?.toIntOrNull() ?: continue
                    val time = parts.getOrNull(2) ?: continue
                    currentIndices += CueIndex(num, time, cueTimeToMs(time))
                }
            }
        }
        flushTrack()
        if (fileName == null && tracks.isEmpty()) return null
        return CueSheet(fileName, fileType, sheetTitle, sheetPerformer, tracks)
    }

    /** Converts mm:ss:ff (75 frames/sec) to milliseconds. */
    fun cueTimeToMs(time: String): Long {
        val parts = time.split(':')
        if (parts.size < 3) return 0L
        val mm = parts[0].toLongOrNull() ?: return 0L
        val ss = parts[1].toLongOrNull() ?: return 0L
        val ff = parts[2].toLongOrNull() ?: return 0L
        return ((mm * 60 + ss) * 1000L) + (ff * 1000L / 75L)
    }

    private fun quotedValue(rest: String): String? {
        val start = rest.indexOf('"')
        if (start < 0) return rest.trim().ifEmpty { null }
        val end = rest.indexOf('"', start + 1)
        if (end < 0) return rest.substring(start + 1).trim().ifEmpty { null }
        return rest.substring(start + 1, end)
    }
}
