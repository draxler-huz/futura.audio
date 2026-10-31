package com.futura.audio.data.scan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.DigestInputStream
import java.security.MessageDigest

/**
 * Streamed MD5 and SHA-1 over the full file (no full-file buffering).
 */
object TrackHasher {

    data class Digests(
        val md5: String,
        val sha1: String,
    )

    suspend fun hash(file: File): Digests = withContext(Dispatchers.IO) {
        hashBlocking(file)
    }

    fun hashBlocking(file: File): Digests {
        val md5 = MessageDigest.getInstance("MD5")
        val sha1 = MessageDigest.getInstance("SHA-1")
        val buffer = ByteArray(DEFAULT_BUFFER)
        FileInputStream(file).use { fis ->
            DigestInputStream(fis, md5).use { md5Stream ->
                DigestInputStream(md5Stream, sha1).use { sha1Stream ->
                    while (sha1Stream.read(buffer) >= 0) {
                        // digests updated by DigestInputStream
                    }
                }
            }
        }
        return Digests(
            md5 = md5.digest().toHex(),
            sha1 = sha1.digest().toHex(),
        )
    }

    private fun ByteArray.toHex(): String {
        val sb = StringBuilder(size * 2)
        for (b in this) {
            sb.append(HEX[(b.toInt() ushr 4) and 0x0F])
            sb.append(HEX[b.toInt() and 0x0F])
        }
        return sb.toString()
    }

    private val HEX = "0123456789abcdef".toCharArray()
    private const val DEFAULT_BUFFER = 64 * 1024
}
