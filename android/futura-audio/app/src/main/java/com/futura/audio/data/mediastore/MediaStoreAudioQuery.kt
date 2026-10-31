package com.futura.audio.data.mediastore

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Async MediaStore audio query (item 26).
 * Runs on [Dispatchers.IO] via [ContentResolver]; does not write Room yet.
 */
class MediaStoreAudioQuery(
    private val contentResolver: ContentResolver,
) {

    data class AudioItem(
        val mediaStoreId: Long,
        val contentUri: Uri,
        val title: String,
        val artist: String?,
        val album: String?,
        val durationMs: Long,
        val dateAddedEpochSec: Long,
        val relativePath: String?,
        val displayName: String?,
        val mimeType: String?,
        val sizeBytes: Long,
        val absolutePath: String?,
    )

    suspend fun queryAllAudio(): List<AudioItem> = withContext(Dispatchers.IO) {
        val collection: Uri =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            add(MediaStore.Audio.Media.MIME_TYPE)
            add(MediaStore.Audio.Media.SIZE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                add(MediaStore.Audio.Media.DATA)
            }
        }.toTypedArray()

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val results = mutableListOf<AudioItem>()
        contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val displayNameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val relativePathCol =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
                } else {
                    -1
                }
            val dataCol =
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    @Suppress("DEPRECATION")
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                } else {
                    -1
                }

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(collection, id)
                results += AudioItem(
                    mediaStoreId = id,
                    contentUri = contentUri,
                    title = cursor.getString(titleCol).orEmpty(),
                    artist = cursor.getString(artistCol),
                    album = cursor.getString(albumCol),
                    durationMs = cursor.getLong(durationCol),
                    dateAddedEpochSec = cursor.getLong(dateAddedCol),
                    relativePath = if (relativePathCol >= 0) cursor.getString(relativePathCol) else null,
                    displayName = cursor.getString(displayNameCol),
                    mimeType = cursor.getString(mimeCol),
                    sizeBytes = cursor.getLong(sizeCol),
                    absolutePath = if (dataCol >= 0) cursor.getString(dataCol) else null,
                )
            }
        }
        results
    }
}
