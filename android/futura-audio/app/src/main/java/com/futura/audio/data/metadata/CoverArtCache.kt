package com.futura.audio.data.metadata

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Disk LRU cover-art cache. Stores scaled images as WEBP when possible.
 *
 * - API 30+: [Bitmap.CompressFormat.WEBP_LOSSY]; on failure falls back to
 *   [Bitmap.CompressFormat.WEBP_LOSSLESS], then JPEG.
 * - API &lt; 30: JPEG (WEBP_LOSSY / WEBP_LOSSLESS unavailable).
 */
class CoverArtCache(
    context: Context,
    private val maxBytes: Long = DEFAULT_MAX_BYTES,
    private val maxEdgePx: Int = DEFAULT_MAX_EDGE,
) {
    private val cacheDir = File(context.applicationContext.cacheDir, "cover_art").also { it.mkdirs() }
    private val mutex = Mutex()

    suspend fun get(cacheKey: String): File? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = fileFor(cacheKey) ?: return@withLock null
            if (!file.exists() || file.length() == 0L) return@withLock null
            // LRU touch
            file.setLastModified(System.currentTimeMillis())
            file
        }
    }

    /**
     * Decodes [bytes], scales to [maxEdgePx], writes to disk, returns cache key.
     */
    suspend fun put(sourceKey: String, bytes: ByteArray): String? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val key = sha1Hex(sourceKey)
            val target = File(cacheDir, "$key$EXT")
            if (target.exists() && target.length() > 0L) {
                target.setLastModified(System.currentTimeMillis())
                return@withLock key
            }
            val bitmap = decodeScaled(bytes) ?: return@withLock null
            try {
                if (!writeBitmap(bitmap, target)) return@withLock null
                trimToSizeLocked()
                key
            } finally {
                bitmap.recycle()
            }
        }
    }

    suspend fun remove(cacheKey: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            fileFor(cacheKey)?.delete()
            Unit
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock {
            cacheDir.listFiles()?.forEach { it.delete() }
            Unit
        }
    }

    private fun fileFor(cacheKey: String): File? {
        if (cacheKey.isBlank()) return null
        val safe = cacheKey.replace(Regex("[^a-fA-F0-9]"), "")
        if (safe.isEmpty()) return null
        return File(cacheDir, "$safe$EXT")
    }

    private fun decodeScaled(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        var w = bounds.outWidth
        var h = bounds.outHeight
        while (w / sample > maxEdgePx || h / sample > maxEdgePx) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return null
        val edge = maxOf(decoded.width, decoded.height)
        if (edge <= maxEdgePx) return decoded
        val scale = maxEdgePx.toFloat() / edge
        val tw = (decoded.width * scale).toInt().coerceAtLeast(1)
        val th = (decoded.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(decoded, tw, th, true)
        if (scaled !== decoded) decoded.recycle()
        return scaled
    }

    private fun writeBitmap(bitmap: Bitmap, target: File): Boolean {
        val tmp = File(cacheDir, "${target.name}.tmp")
        return try {
            val ok = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    // WEBP_LOSSY only on API 30+; fallback chain: LOSSLESS → JPEG.
                    compress(bitmap, tmp, Bitmap.CompressFormat.WEBP_LOSSY, 82) ||
                        compress(bitmap, tmp, Bitmap.CompressFormat.WEBP_LOSSLESS, 100) ||
                        compress(bitmap, tmp, Bitmap.CompressFormat.JPEG, 85)
                }
                else -> compress(bitmap, tmp, Bitmap.CompressFormat.JPEG, 85)
            }
            if (!ok) {
                tmp.delete()
                return false
            }
            if (target.exists()) target.delete()
            tmp.renameTo(target)
        } catch (_: Exception) {
            tmp.delete()
            false
        }
    }

    private fun compress(
        bitmap: Bitmap,
        file: File,
        format: Bitmap.CompressFormat,
        quality: Int,
    ): Boolean {
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(format, quality, out)
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun trimToSizeLocked() {
        val files = cacheDir.listFiles()?.filter { it.isFile && it.name.endsWith(EXT) }?.toMutableList()
            ?: return
        var total = files.sumOf { it.length() }
        if (total <= maxBytes) return
        files.sortBy { it.lastModified() }
        for (f in files) {
            if (total <= maxBytes) break
            val len = f.length()
            if (f.delete()) total -= len
        }
    }

    private fun sha1Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(input.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(digest.size * 2)
        for (b in digest) {
            sb.append(HEX[(b.toInt() ushr 4) and 0x0F])
            sb.append(HEX[b.toInt() and 0x0F])
        }
        return sb.toString()
    }

    companion object {
        private const val EXT = ".img"
        private const val DEFAULT_MAX_BYTES = 64L * 1024L * 1024L
        private const val DEFAULT_MAX_EDGE = 512
        private val HEX = "0123456789abcdef".toCharArray()
    }
}
