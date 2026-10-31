package com.futura.audio.data.metadata

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Tag extraction via jaudiotagger with [MediaMetadataRetriever] fallback.
 * Supports ID3v1/v2, FLAC, MP4 (and other formats jaudiotagger can open).
 */
class MetadataExtractor(context: Context) {
    private val appContext = context.applicationContext

    init {
        // Quiet jaudiotagger's noisy JUL logging on Android.
        Logger.getLogger("org.jaudiotagger").level = Level.OFF
    }

    data class Tags(
        val title: String?,
        val artist: String?,
        val album: String?,
        val albumArtist: String?,
        val genre: String?,
        val year: Int?,
        val trackNumber: Int?,
        val lyrics: String?,
    )

    suspend fun extract(file: File): Tags = withContext(Dispatchers.IO) {
        extractBlocking(file)
    }

    fun extractBlocking(file: File): Tags {
        val fromJaudio = runCatching { extractJaudio(file) }.getOrNull()
        if (fromJaudio != null && !fromJaudio.isBlankish()) return fromJaudio
        val fromMmr = extractMmr(file.absolutePath)
        return merge(fromJaudio, fromMmr)
    }

    suspend fun extract(uri: Uri): Tags = withContext(Dispatchers.IO) {
        extractMmrUri(uri)
    }

    private fun extractJaudio(file: File): Tags {
        val audioFile = AudioFileIO.read(file)
        val tag = audioFile.tag ?: return Tags(null, null, null, null, null, null, null, null)
        return Tags(
            title = tag.getFirst(FieldKey.TITLE).nullIfBlank(),
            artist = tag.getFirst(FieldKey.ARTIST).nullIfBlank(),
            album = tag.getFirst(FieldKey.ALBUM).nullIfBlank(),
            albumArtist = tag.getFirst(FieldKey.ALBUM_ARTIST).nullIfBlank(),
            genre = tag.getFirst(FieldKey.GENRE).nullIfBlank(),
            year = tag.getFirst(FieldKey.YEAR).nullIfBlank()?.take(4)?.toIntOrNull(),
            trackNumber = tag.getFirst(FieldKey.TRACK).nullIfBlank()
                ?.substringBefore('/')
                ?.toIntOrNull(),
            lyrics = tag.getFirst(FieldKey.LYRICS).nullIfBlank(),
        )
    }

    private fun extractMmr(path: String): Tags {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            tagsFromRetriever(retriever)
        } catch (_: Exception) {
            Tags(null, null, null, null, null, null, null, null)
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun extractMmrUri(uri: Uri): Tags {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(appContext, uri)
            tagsFromRetriever(retriever)
        } catch (_: Exception) {
            Tags(null, null, null, null, null, null, null, null)
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun tagsFromRetriever(retriever: MediaMetadataRetriever): Tags {
        return Tags(
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).nullIfBlank(),
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST).nullIfBlank(),
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).nullIfBlank(),
            albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                .nullIfBlank(),
            genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE).nullIfBlank(),
            year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                ?.take(4)?.toIntOrNull(),
            trackNumber = null,
            lyrics = null,
        )
    }

    private fun merge(primary: Tags?, fallback: Tags): Tags {
        if (primary == null) return fallback
        return Tags(
            title = primary.title ?: fallback.title,
            artist = primary.artist ?: fallback.artist,
            album = primary.album ?: fallback.album,
            albumArtist = primary.albumArtist ?: fallback.albumArtist,
            genre = primary.genre ?: fallback.genre,
            year = primary.year ?: fallback.year,
            trackNumber = primary.trackNumber ?: fallback.trackNumber,
            lyrics = primary.lyrics ?: fallback.lyrics,
        )
    }

    private fun Tags.isBlankish(): Boolean =
        title.isNullOrBlank() && artist.isNullOrBlank() && album.isNullOrBlank()

    private fun String?.nullIfBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}
