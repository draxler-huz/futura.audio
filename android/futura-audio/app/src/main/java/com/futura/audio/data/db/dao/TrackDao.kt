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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tracks: List<TrackEntity>)

    @Update
    suspend fun update(track: TrackEntity)

    /**
     * Updates indexer-owned fields only. Leaves valenceOverride / energyOverride untouched.
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
            energy = :energy
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
