package com.futura.audio.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.futura.audio.data.db.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE ASC")
    suspend fun getAll(): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getById(id: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE absolutePath = :absolutePath LIMIT 1")
    suspend fun getByAbsolutePath(absolutePath: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE mediaStoreId = :mediaStoreId LIMIT 1")
    suspend fun getByMediaStoreId(mediaStoreId: Long): TrackEntity?

    @Query("SELECT * FROM tracks WHERE favorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun observeFavorites(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE favorite = 1 ORDER BY title COLLATE NOCASE ASC")
    suspend fun getFavorites(): List<TrackEntity>

    @Query(
        """
        SELECT * FROM tracks
        WHERE playEventCount > 0
        ORDER BY (completionRatioSum * 1.0 / playEventCount) DESC, plays DESC
        LIMIT :limit
        """,
    )
    suspend fun getMostPlayedByCompletion(limit: Int): List<TrackEntity>

    @Query("SELECT * FROM tracks ORDER BY plays DESC LIMIT :limit")
    suspend fun getMostPlayedByPlays(limit: Int): List<TrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tracks: List<TrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(track: TrackEntity)

    @Update
    suspend fun update(track: TrackEntity)

    @Query("DELETE FROM tracks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("UPDATE tracks SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("UPDATE tracks SET plays = :plays WHERE id = :id")
    suspend fun setPlays(id: String, plays: Int)

    @Query(
        """
        UPDATE tracks SET
            plays = plays + 1,
            completionRatioSum = completionRatioSum + :completionRatio,
            playEventCount = playEventCount + 1,
            lastPlayedDaysAgo = 0
        WHERE id = :id
        """,
    )
    suspend fun recordPlayCompletion(id: String, completionRatio: Float)

    /**
     * Updates indexer-owned fields only. Leaves valenceOverride / energyOverride,
     * favorite, plays, completionRatioSum, and playEventCount untouched.
     */
    @Query(
        """
        UPDATE tracks SET
            title = :title,
            artistId = :artistId,
            albumId = :albumId,
            duration = :duration,
            folder = :folder,
            fileName = :fileName,
            valence = :valence,
            energy = :energy,
            absolutePath = :absolutePath,
            contentUri = :contentUri,
            mediaStoreId = :mediaStoreId,
            format = :format,
            md5 = :md5,
            sha1 = :sha1,
            volumeUuid = :volumeUuid,
            durationMs = :durationMs,
            genre = :genre,
            normalizedGenre = :normalizedGenre,
            lyrics = :lyrics,
            coverCacheKey = :coverCacheKey,
            fileSizeBytes = :fileSizeBytes,
            lastModifiedEpochMs = :lastModifiedEpochMs
        WHERE id = :id
        """,
    )
    suspend fun updateIndexedFields(
        id: String,
        title: String,
        artistId: String,
        albumId: String,
        duration: Int,
        folder: String,
        fileName: String,
        valence: Float,
        energy: Float,
        absolutePath: String?,
        contentUri: String?,
        mediaStoreId: Long?,
        format: String?,
        md5: String?,
        sha1: String?,
        volumeUuid: String?,
        durationMs: Long?,
        genre: String?,
        normalizedGenre: String?,
        lyrics: String?,
        coverCacheKey: String?,
        fileSizeBytes: Long?,
        lastModifiedEpochMs: Long?,
    )

    @Query(
        """
        UPDATE tracks SET
            valenceOverride = :valenceOverride,
            energyOverride = :energyOverride
        WHERE id = :id
        """,
    )
    suspend fun setMoodOverrides(
        id: String,
        valenceOverride: Float?,
        energyOverride: Float?,
    )
}
