package com.futura.audio.data.metadata

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Reads embedded cover-art bytes into memory (no disk I/O beyond the source file).
 */
class EmbeddedArtworkReader(context: Context) {
    private val appContext = context.applicationContext

    init {
        Logger.getLogger("org.jaudiotagger").level = Level.OFF
    }

    suspend fun read(file: File): ByteArray? = withContext(Dispatchers.IO) {
        readBlocking(file)
    }

    fun readBlocking(file: File): ByteArray? {
        val fromJaudio = runCatching {
            val audioFile = AudioFileIO.read(file)
            val artwork = audioFile.tag?.firstArtwork ?: return@runCatching null
            artwork.binaryData?.takeIf { it.isNotEmpty() }
        }.getOrNull()
        if (fromJaudio != null) return fromJaudio
        return readMmr(file.absolutePath)
    }

    suspend fun read(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(appContext, uri)
            retriever.embeddedPicture?.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun readMmr(path: String): ByteArray? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            retriever.embeddedPicture?.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }
}
