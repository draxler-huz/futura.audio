package com.futura.audio.data.scan

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Exact duration in milliseconds via [MediaMetadataRetriever].
 */
class DurationReader(context: Context) {
    private val appContext = context.applicationContext

    suspend fun readDurationMs(file: File): Long? = withContext(Dispatchers.IO) {
        readDurationMsBlocking(file.absolutePath)
    }

    suspend fun readDurationMs(uri: Uri): Long? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(appContext, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.takeIf { it > 0L }
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
                // ignore
            }
        }
    }

    suspend fun readDurationMs(pathOrUri: String): Long? = withContext(Dispatchers.IO) {
        if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://")) {
            readDurationMs(Uri.parse(pathOrUri))
        } else {
            readDurationMsBlocking(pathOrUri)
        }
    }

    private fun readDurationMsBlocking(absolutePath: String): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.takeIf { it > 0L }
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
                // ignore
            }
        }
    }
}
