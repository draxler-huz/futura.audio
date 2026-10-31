package com.futura.audio.data.scan

import java.io.File
import java.io.RandomAccessFile

/**
 * Skips 0-byte, unreadable, or header-invalid audio files during indexing.
 */
object CorruptFileDetector {

    data class Verdict(
        val ok: Boolean,
        val reason: String? = null,
    )

    fun evaluate(file: File): Verdict {
        if (!file.exists()) return Verdict(false, "missing")
        if (!file.isFile) return Verdict(false, "not_a_file")
        if (!file.canRead()) return Verdict(false, "unreadable")
        if (file.length() <= 0L) return Verdict(false, "zero_byte")
        if (file.length() < 12L) return Verdict(false, "too_small")

        val format = FormatResolver.resolve(file)
        if (format == FormatResolver.AudioFormat.UNKNOWN) {
            return Verdict(false, "header_invalid")
        }

        // Extra header sanity: must be able to read at least 12 bytes without error.
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val buf = ByteArray(12)
                val n = raf.read(buf)
                if (n < 12) Verdict(false, "short_read") else Verdict(true)
            }
        } catch (e: Exception) {
            Verdict(false, e.message ?: "read_error")
        }
    }

    fun isCorrupt(file: File): Boolean = !evaluate(file).ok
}
