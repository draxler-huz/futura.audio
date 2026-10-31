package com.futura.audio.data.scan

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale

/**
 * Detects audio format from file extension + header magic bytes.
 * Covers Opus, ALAC, AIFF, WavPack, DSD, and common mp3/flac/m4a/wav/ogg.
 */
object FormatResolver {

    enum class AudioFormat(val id: String) {
        MP3("mp3"),
        FLAC("flac"),
        M4A("m4a"),
        AAC("aac"),
        ALAC("alac"),
        WAV("wav"),
        AIFF("aiff"),
        OGG("ogg"),
        OPUS("opus"),
        WAVPACK("wv"),
        DSD_DSF("dsf"),
        DSD_DFF("dff"),
        UNKNOWN("unknown"),
    }

    fun resolve(file: File): AudioFormat {
        val byExt = fromExtension(file.extension)
        val byMagic = fromMagic(file)
        // Prefer magic when it is more specific (ALAC inside MP4, Opus inside Ogg).
        return when {
            byMagic == AudioFormat.ALAC -> AudioFormat.ALAC
            byMagic == AudioFormat.OPUS -> AudioFormat.OPUS
            byMagic != AudioFormat.UNKNOWN -> byMagic
            else -> byExt
        }
    }

    fun fromExtension(extension: String): AudioFormat {
        return when (extension.lowercase(Locale.US)) {
            "mp3", "mp2", "mpga" -> AudioFormat.MP3
            "flac" -> AudioFormat.FLAC
            "m4a", "mp4", "m4b" -> AudioFormat.M4A
            "aac" -> AudioFormat.AAC
            "wav", "wave" -> AudioFormat.WAV
            "aiff", "aif", "aifc" -> AudioFormat.AIFF
            "ogg", "oga" -> AudioFormat.OGG
            "opus" -> AudioFormat.OPUS
            "wv" -> AudioFormat.WAVPACK
            "dsf" -> AudioFormat.DSD_DSF
            "dff", "dsd" -> AudioFormat.DSD_DFF
            else -> AudioFormat.UNKNOWN
        }
    }

    fun isSupportedAudioExtension(extension: String): Boolean =
        fromExtension(extension) != AudioFormat.UNKNOWN

    private fun fromMagic(file: File): AudioFormat {
        if (!file.isFile || !file.canRead() || file.length() < 12) return AudioFormat.UNKNOWN
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val header = ByteArray(64)
                val read = raf.read(header)
                if (read < 12) return AudioFormat.UNKNOWN
                detectMagic(header, read, raf)
            }
        } catch (_: Exception) {
            AudioFormat.UNKNOWN
        }
    }

    private fun detectMagic(header: ByteArray, length: Int, raf: RandomAccessFile): AudioFormat {
        // ID3 → treat as MP3 container
        if (length >= 3 && header[0] == 'I'.code.toByte() &&
            header[1] == 'D'.code.toByte() && header[2] == '3'.code.toByte()
        ) {
            return AudioFormat.MP3
        }
        // MPEG frame sync
        if (length >= 2 && header[0] == 0xFF.toByte() &&
            (header[1].toInt() and 0xE0) == 0xE0
        ) {
            return AudioFormat.MP3
        }
        // fLaC
        if (startsWith(header, length, "fLaC")) return AudioFormat.FLAC
        // OggS → check for OpusHead
        if (startsWith(header, length, "OggS")) {
            return if (containsAscii(header, length, "OpusHead") || peekOpus(raf)) {
                AudioFormat.OPUS
            } else {
                AudioFormat.OGG
            }
        }
        // RIFF....WAVE
        if (startsWith(header, length, "RIFF") && length >= 12 &&
            header[8] == 'W'.code.toByte() && header[9] == 'A'.code.toByte() &&
            header[10] == 'V'.code.toByte() && header[11] == 'E'.code.toByte()
        ) {
            return AudioFormat.WAV
        }
        // FORM....AIFF / AIFC
        if (startsWith(header, length, "FORM") && length >= 12) {
            val type = String(header, 8, 4, Charsets.US_ASCII)
            if (type == "AIFF" || type == "AIFC") return AudioFormat.AIFF
        }
        // WavPack "wvpk"
        if (startsWith(header, length, "wvpk")) return AudioFormat.WAVPACK
        // DSF "DSD "
        if (startsWith(header, length, "DSD ")) return AudioFormat.DSD_DSF
        // DFF "FRM8"
        if (startsWith(header, length, "FRM8")) return AudioFormat.DSD_DFF
        // ISO BMFF (ftyp) → m4a / alac
        if (length >= 12 && header[4] == 'f'.code.toByte() && header[5] == 't'.code.toByte() &&
            header[6] == 'y'.code.toByte() && header[7] == 'p'.code.toByte()
        ) {
            return if (containsAlacBrand(header, length) || scanForAlac(raf)) {
                AudioFormat.ALAC
            } else {
                AudioFormat.M4A
            }
        }
        return AudioFormat.UNKNOWN
    }

    private fun peekOpus(raf: RandomAccessFile): Boolean {
        return try {
            val pos = raf.filePointer
            raf.seek(0)
            val buf = ByteArray(512)
            val n = raf.read(buf)
            raf.seek(pos)
            n > 0 && containsAscii(buf, n, "OpusHead")
        } catch (_: Exception) {
            false
        }
    }

    private fun containsAlacBrand(header: ByteArray, length: Int): Boolean {
        val brands = listOf("alac", "ALAC")
        return brands.any { containsAscii(header, length, it) }
    }

    /** Light scan of the first 64 KiB for an 'alac' atom (approximation). */
    private fun scanForAlac(raf: RandomAccessFile): Boolean {
        return try {
            val pos = raf.filePointer
            raf.seek(0)
            val buf = ByteArray(65_536)
            val n = raf.read(buf)
            raf.seek(pos)
            n > 0 && containsAscii(buf, n, "alac")
        } catch (_: Exception) {
            false
        }
    }

    private fun startsWith(data: ByteArray, length: Int, ascii: String): Boolean {
        if (length < ascii.length) return false
        for (i in ascii.indices) {
            if (data[i] != ascii[i].code.toByte()) return false
        }
        return true
    }

    private fun containsAscii(data: ByteArray, length: Int, ascii: String): Boolean {
        if (ascii.isEmpty() || length < ascii.length) return false
        outer@ for (i in 0..(length - ascii.length)) {
            for (j in ascii.indices) {
                if (data[i + j] != ascii[j].code.toByte()) continue@outer
            }
            return true
        }
        return false
    }

    @Suppress("unused")
    private fun leInt(data: ByteArray, offset: Int): Int =
        ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int
}
