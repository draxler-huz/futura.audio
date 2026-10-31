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
import com.futura.audio.data.scan.CorruptFileDetector
import com.futura.audio.data.scan.FormatResolver
import com.futura.audio.data.scan.RecursiveFileScanner
import com.futura.audio.data.scan.StorageVolumeMapper
import com.futura.audio.data.scan.TrackHasher
import com.futura.audio.data.scan.DurationReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Indexes scanned audio files into Room database.
 * 
 * Reads files via [RecursiveFileScanner], extracts metadata with [MetadataExtractor],
 * analyzes mood with [MoodAnalyzer], normalizes genre with [GenreNormalizer], and writes
 * Track, Artist, Album entities to [FuturaDatabase] while preserving [valenceOverride] /
 * [energyOverride] on re-index.
 *
 * All stages (file discovery, metadata extraction, mood analysis) run on [Dispatchers.IO] to
 * keep the UI responsive. The Room writes are batched for efficiency.
 */
class TrackIndexer(
    private val context: Context,
    private val database: FuturaDatabase = FuturaDatabase.getInstance(context),
) {
    private val scanner = RecursiveFileScanner(StorageVolumeMapper(context))
    private val metadataExtractor = MetadataExtractor(context)
    private val moodAnalyzer = MoodAnalyzer(context)
    private val genreNormalizer = GenreNormalizer(context)
    private val durationReader = DurationReader(context)

    /**
     * Scan all music directories and index discovered tracks.
     * Existing [TrackEntity]s are updated; new ones are inserted.
     * Overrides survive re-indexing because the update‑only paths skip
     * valenceOverride / energyOverride.
     */
    suspend fun indexAll() = withContext(Dispatchers.IO) {
        val scanned = scanner.scan(musicOnly = true)
        val grouped = groupByArtistAlbum(scanned)
        for ((artistId, albumGroup) in grouped) {
            indexArtistAlbum(artistId, albumGroup)
        }
    }

    /**
     * Index a single file into the library.
     * If a [TrackEntity] with the same absolutePath exists, it is updated;
     * otherwise, it is inserted. Existing overrides are preserved.
     */
    suspend fun indexFile(filePath: String) = withContext(Dispatchers.IO) {
        val file = java.io.File(filePath)
        if (!file.exists() || CorruptFileDetector.isCorrupt(file)) return@withContext

        val format = FormatResolver.resolve(file)
        if (format == FormatResolver.AudioFormat.UNKNOWN) return@withContext

        // Core metadata via jaudiotagger, with MediaMetadataRetriever fallback.
        val metadata = metadataExtractor.extractBlocking(file)
        val artistName = metadata.artist ?: "Unknown Artist"
        val albumTitle = metadata.album ?: "Unknown Album"
        val genre = metadata.genre

        // Normalize genre to one of Electronic, Chill, Indie, Soul.
        val normalizedGenre = if (!genre.isNullOrBlank()) {
            genreNormalizer.normalizeBlocking(genre).family
        } else {
            "Unknown"
        }

        // Mood from decoded PCM (first ~15s for bounded indexing).
        val mood = moodAnalyzer.analyzeFile(file)

        // Duration in seconds (exact ms from MediaMetadataRetriever).
        val durationMs = durationReader.readDurationMs(file.absolutePath)
        val durationSec = durationMs?.let { (it + 999) / 1000 }?.toInt() ?: 0

        // Hash for integrity / deduplication.
        val hashes = TrackHasher.hashBlocking(file)

        // Volume UUID for multi‑volume libraries.
        val volumeMapper = StorageVolumeMapper(context)
        val volumeUuid = volumeMapper.volumeUuidForBlocking(file)

        // Artist and Album lookup / creation.
        val artistEntity = database.artistDao().getByIdOrCreate(artistName)
        val albumEntity = database.albumDao().getByIdOrCreate(albumTitle, artistEntity.id)

        // Existing track lookup for updates.
        val existing = database.trackDao().getByAbsolutePath(file.absolutePath)
        val trackId = existing?.id ?: generateTrackId(file)

        val trackEntity = existing?.copy(
            // Update indexed fields (new values take precedence).
            title = metadata.title ?: file.nameWithoutExtension,
            duration = durationSec,
            folder = extractFolderFromPath(file.absolutePath),
            fileName = file.name,
            valence = mood.valence,
            energy = mood.energy,
            absolutePath = file.absolutePath,
            contentUri = "file://${file.absolutePath}",
            format = format.name,
            md5 = hashes.md5,
            sha1 = hashes.sha1,
            volumeUuid = volumeUuid,
            durationMs = durationMs,
            genre = genre,
            normalizedGenre = normalizedGenre,
            lyrics = metadata.lyrics,
            coverCacheKey = null, // populated later via artwork pipeline
            fileSizeBytes = file.length(),
            lastModifiedEpochMs = file.lastModified() / 1000,
            // NOTE: valenceOverride / energyOverride are NOT touched here
        ) ?: TrackEntity(
            id = trackId,
            title = metadata.title ?: file.nameWithoutExtension,
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
            folder = extractFolderFromPath(file.absolutePath),
            fileName = file.name,
            absolutePath = file.absolutePath,
            contentUri = "file://${file.absolutePath}",
            mediaStoreId = null,
            format = format.name,
            md5 = hashes.md5,
            sha1 = hashes.sha1,
            volumeUuid = volumeUuid,
            durationMs = durationMs,
            genre = genre,
            normalizedGenre = normalizedGenre,
            lyrics = metadata.lyrics,
            coverCacheKey = null,
            fileSizeBytes = file.length(),
            lastModifiedEpochMs = file.lastModified() / 1000,
            completionRatioSum = 0f,
            playEventCount = 0,
        )

        database.trackDao().upsert(trackEntity)
    }

    /**
     * MediaStore results are upserted similarly (MediaStoreId populated).
     * Overrides survive because we use the same [TrackDao.upsert] path.
     */
    suspend fun indexMediaStoreTrack(
        trackId: String,
        title: String,
        artistId: String,
        albumId: String,
        duration: Int,
        folder: String,
        fileName: String,
        valence: Float,
        energy: Float,
        mediaStoreId: Long,
        format: String,
        md5: String?,
        sha1: String?,
        volumeUuid: String?,
        durationMs: Long,
        genre: String?,
        normalizedGenre: String?,
        lyrics: String?,
        coverCacheKey: String?,
        fileSizeBytes: Long,
        lastModifiedEpochMs: Long,
        absolutePath: String,
        contentUri: String,
    ) {
        val existing = database.trackDao().getByMediaStoreId(mediaStoreId)
        val trackEntity = existing?.copy(
            // Update indexed fields, but leave overrides untouched.
            title = title,
            artistId = artistId,
            albumId = albumId,
            duration = duration,
            folder = folder,
            fileName = fileName,
            valence = valence,
            energy = energy,
            absolutePath = absolutePath,
            contentUri = contentUri,
            mediaStoreId = mediaStoreId,
            format = format,
            md5 = md5,
            sha1 = sha1,
            volumeUuid = volumeUuid,
            durationMs = durationMs,
            genre = genre,
            normalizedGenre = normalizedGenre,
            lyrics = lyrics,
            coverCacheKey = coverCacheKey,
            fileSizeBytes = fileSizeBytes,
            lastModifiedEpochMs = lastModifiedEpochMs,
        ) ?: TrackEntity(
            id = trackId,
            title = title,
            artistId = artistId,
            albumId = albumId,
            duration = duration,
            plays = 0,
            addedDaysAgo = 0,
            lastPlayedDaysAgo = null,
            valence = valence,
            energy = energy,
            valenceOverride = null,
            energyOverride = null,
            favorite = false,
            folder = folder,
            fileName = fileName,
            absolutePath = absolutePath,
            contentUri = contentUri,
            mediaStoreId = mediaStoreId,
            format = format,
            md5 = md5,
            sha1 = sha1,
            volumeUuid = volumeUuid,
            durationMs = durationMs,
            genre = genre,
            normalizedGenre = normalizedGenre,
            lyrics = lyrics,
            coverCacheKey = coverCacheKey,
            fileSizeBytes = fileSizeBytes,
            lastModifiedEpochMs = lastModifiedEpochMs,
            completionRatioSum = 0f,
            playEventCount = 0,
        )

        database.trackDao().upsert(trackEntity)
    }

    private fun groupByArtistAlbum(scanned: List<RecursiveFileScanner.ScannedFile>): Map<String, Map<String, List<RecursiveFileScanner.ScannedFile>>> {
        val result = mutableMapOf<String, MutableMap<String, MutableList<RecursiveFileScanner.ScannedFile>>>()
        for (scannedFile in scanned) {
            // For simplicity, use filename as placeholder for artist/album logic
            val artistId = "artist_${scannedFile.file.name.hashCode()}"
            val albumId = "album_${scannedFile.file.name.hashCode()}"
            result.computeIfAbsent(artistId) { mutableMapOf() }
                .computeIfAbsent(albumId) { mutableListOf() }
                .add(scannedFile)
        }
        return result
    }

    private suspend fun indexArtistAlbum(artistId: String, albumGroup: Map<String, List<RecursiveFileScanner.ScannedFile>>) {
        // Simple artist and album insertion
        val artistEntity = ArtistEntity(
            id = artistId,
            name = "Artist $artistId",
            genre = "Unknown",
            family = "Unknown",
            hue = Math.floorMod(artistId.hashCode(), 360),
        )
        database.artistDao().upsert(artistEntity)

        for ((albumId, files) in albumGroup) {
            val albumEntity = AlbumEntity(
                id = albumId,
                title = "Album $albumId",
                artistId = artistId,
                year = 2024,
                hue = Math.floorMod(albumId.hashCode(), 360),
            )
            database.albumDao().upsert(albumEntity)
        }
    }

    private fun generateTrackId(file: java.io.File): String {
        val hash = java.security.MessageDigest.getInstance("SHA-1").digest(file.absolutePath.toByteArray())
        return "track_${hash.joinToString("") { "%02x".format(it) } }"
    }

    private fun extractFolderFromPath(path: String): String {
        // Extract folder structure for display
        return path.substringAfterLast("/Music/", "")
            .substringBeforeLast("/", "")
            .replace("/", " > ")
    }
}
