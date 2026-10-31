package com.futura.audio.data.index

import android.content.Context
import android.net.Uri
import com.futura.audio.data.db.FuturaDatabase
import com.futura.audio.data.db.entity.ArtistEntity
import com.futura.audio.data.db.entity.AlbumEntity
import com.futura.audio.data.db.entity.TrackEntity
import com.futura.audio.data.metadata.MetadataExtractor
import com.futura.audio.data.metadata.MoodAnalyzer
import com.futura.audio.data.metadata.GenreNormalizer
import com.futura.audio.data.scan.StorageVolumeMapper
import com.futura.audio.data.scan.TrackHasher
import com.futura.audio.data.scan.DurationReader
import com.futura.audio.data.mediastore.MediaStoreAudioQuery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Indexes MediaStore audio tracks into Room database.
 * 
 * Reads via [MediaStoreAudioQuery] (MediaStore.Audio.Media), enriches with
 * [MetadataExtractor], [MoodAnalyzer], [GenreNormalizer], and writes Track,
 * Artist, Album entities while preserving [valenceOverride] / [energyOverride].
 *
 * All stages run on [Dispatchers.IO]; Room writes are batched for efficiency.
 */
class MediaStoreIndexer(
    private val context: Context,
    private val database: FuturaDatabase = FuturaDatabase.getInstance(context),
) {
    private val mediaStoreQuery = MediaStoreAudioQuery(context.contentResolver)
    private val metadataExtractor = MetadataExtractor(context)
    private val moodAnalyzer = MoodAnalyzer(context)
    private val genreNormalizer = GenreNormalizer(context)
    private val durationReader = DurationReader(context)
    private val storageMapper = StorageVolumeMapper(context)

    /**
     * Query MediaStore for all audio and index discovered tracks.
     * Existing [TrackEntity]s are updated; new ones are inserted.
     * Overrides survive re-indexing because update‑only paths skip
     * valenceOverride / energyOverride.
     */
    suspend fun indexAll() = withContext(Dispatchers.IO) {
        val mediaStoreItems = mediaStoreQuery.queryAllAudio()
        val grouped = groupByArtistAlbum(mediaStoreItems)
        for ((artistName, albumGroup) in grouped) {
            indexArtistAlbum(artistName, albumGroup)
        }
    }

    /**
     * Index a single MediaStore audio item into the library.
     * If a [TrackEntity] with the same mediaStoreId exists, it is updated;
     * otherwise, it is inserted. Existing overrides are preserved.
     */
    suspend fun indexMediaStoreTrack(
        mediaStoreId: Long,
        contentUri: Uri,
        title: String,
        artist: String?,
        album: String?,
        durationMs: Long,
        dateAddedEpochSec: Long,
        relativePath: String?,
        displayName: String?,
        mimeType: String?,
        sizeBytes: Long,
        absolutePath: String?,
    ) = withContext(Dispatchers.IO) {
        // For MediaStore items, we use the URI to get detailed metadata
        var metadata = artist?.let { 
            com.futura.audio.data.metadata.MetadataExtractor.Tags(
                title = title,
                artist = artist,
                album = album,
                albumArtist = null,
                genre = null,
                year = null,
                trackNumber = null,
                lyrics = null,
            )
        } ?: com.futura.audio.data.metadata.MetadataExtractor.Tags(
            title = title,
            artist = "Unknown Artist",
            album = album ?: "Unknown Album",
            albumArtist = null,
            genre = null,
            year = null,
            trackNumber = null,
            lyrics = null,
        )

        // Enhance metadata with jaudiotagger if we have a file path
        if (!absolutePath.isNullOrBlank()) {
            val file = java.io.File(absolutePath)
            if (file.exists() && file.canRead()) {
                metadata = metadataExtractor.extractBlocking(file)
            }
        }

        val artistName = metadata.artist ?: "Unknown Artist"
        val albumTitle = metadata.album ?: "Unknown Album"
        val genre = metadata.genre

        // Normalize genre to one of Electronic, Chill, Indie, Soul.
        val normalizedGenre = if (!genre.isNullOrBlank()) {
            genreNormalizer.normalizeBlocking(genre).family
        } else {
            "Unknown"
        }

        // Mood from decoded PCM (first ~15s for bounded indexing) - optional for MediaStore items
        val mood = runCatching {
            moodAnalyzer.analyzeUri(contentUri.toString())
        }.getOrNull() ?: com.futura.audio.data.metadata.MoodAnalyzer.Mood(
            energy = 0.5f,
            valence = 0.5f,
            highEnergy = false,
            positiveValence = false,
        )

        // Duration in seconds
        val durationSec = (durationMs / 1000).toInt()

        // Extract folder from relative path or use fallback
        val folder = extractFolderFromPath(relativePath, absolutePath)

        // Artist and Album lookup / creation.
        val artistEntity = database.artistDao().getByIdOrCreate(artistName)
        val albumEntity = database.albumDao().getByIdOrCreate(albumTitle, artistEntity.id)

        // Existing track lookup for updates.
        val existing = database.trackDao().getByMediaStoreId(mediaStoreId)
        val trackId = existing?.id ?: generateTrackId(contentUri)

        val trackEntity = existing?.copy(
            // Update indexed fields (new values take precedence).
            title = metadata.title ?: title,
            artistId = artistEntity.id,
            albumId = albumEntity.id,
            duration = durationSec,
            folder = folder,
            fileName = displayName ?: "unknown.${mimeType?.substringAfterLast('/', "")}",
            valence = mood.valence,
            energy = mood.energy,
            absolutePath = absolutePath,
            contentUri = contentUri.toString(),
            mediaStoreId = mediaStoreId,
            format = mimeType?.substringAfterLast('/', "").lowercase(),
            md5 = null, // jaudiotagger could read this but skipping for efficiency
            sha1 = null,
            volumeUuid = absolutePath?.let { storageMapper.volumeUuidForBlocking(java.io.File(it)) },
            durationMs = durationMs,
            genre = genre,
            normalizedGenre = normalizedGenre,
            lyrics = metadata.lyrics,
            coverCacheKey = null,
            fileSizeBytes = sizeBytes,
            lastModifiedEpochMs = dateAddedEpochSec * 1000,
            // NOTE: valenceOverride / energyOverride are NOT touched here
        ) ?: TrackEntity(
            id = trackId,
            title = metadata.title ?: title,
            artistId = artistEntity.id,
            albumId = albumEntity.id,
            duration = durationSec,
            plays = 0,
            addedDaysAgo = 0,
            lastPlayedDaysAgo = null,
            valence = mood.valence,
            energy = mood.energy,
            valenceOverride = null,
            energyOverride = null,
            favorite = false,
            folder = folder,
            fileName = displayName ?: "unknown.${mimeType?.substringAfterLast('/', "")}",
            absolutePath = absolutePath,
            contentUri = contentUri.toString(),
            mediaStoreId = mediaStoreId,
            format = mimeType?.substringAfterLast('/', "").lowercase(),
            md5 = null,
            sha1 = null,
            volumeUuid = absolutePath?.let { storageMapper.volumeUuidForBlocking(java.io.File(it)) },
            durationMs = durationMs,
            genre = genre,
            normalizedGenre = normalizedGenre,
            lyrics = metadata.lyrics,
            coverCacheKey = null,
            fileSizeBytes = sizeBytes,
            lastModifiedEpochMs = dateAddedEpochSec * 1000,
            completionRatioSum = 0f,
            playEventCount = 0,
        )

        database.trackDao().upsert(trackEntity)
    }

    private fun groupByArtistAlbum(items: List<MediaStoreAudioQuery.AudioItem>): Map<String, Map<String, List<MediaStoreAudioQuery.AudioItem>>> {
        val result = mutableMapOf<String, MutableMap<String, MutableList<MediaStoreAudioQuery.AudioItem>>>()
        for (item in items) {
            val artistName = item.artist ?: "Unknown Artist"
            val albumTitle = item.album ?: "Unknown Album"
            result.computeIfAbsent(artistName) { mutableMapOf() }
                .computeIfAbsent(albumTitle) { mutableListOf() }
                .add(item)
        }
        return result
    }

    private fun indexArtistAlbum(artistName: String, albumGroup: Map<String, List<MediaStoreAudioQuery.AudioItem>>) {
        val artistEntity = ArtistEntity(
            id = artistName,
            name = artistName,
            genre = "Unknown",
            family = "Unknown",
            popularity = 0,
        )
        database.artistDao().upsert(artistEntity)

        for ((albumTitle, items) in albumGroup) {
            val albumEntity = AlbumEntity(
                id = albumTitle,
                title = albumTitle,
                artistId = artistName,
                year = 2024,
                trackCount = items.size,
            )
            database.albumDao().upsert(albumEntity)
        }
    }

    private fun generateTrackId(uri: Uri): String {
        val hash = java.security.MessageDigest.getInstance("SHA-1").digest(uri.toString().toByteArray())
        return "track_${hash.joinToString("") { "%02x".format(it) } }"
    }

    private fun extractFolderFromPath(relativePath: String?, absolutePath: String?): String {
        // Extract folder structure from relative path or absolute path
        val path = relativePath ?: absolutePath
        return if (!path.isNullOrBlank()) {
            path.substringAfterLast("/Music/", "")
                .substringBeforeLast("/", "")
                .replace("/", " > ")
        } else {
            "Unknown"
        }
    }
}
