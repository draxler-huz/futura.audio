package com.futura.audio.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.futura.audio.data.db.entity.ArtistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtistDao {
    @Query("SELECT * FROM artists ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM artists ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<ArtistEntity>

    @Query("SELECT * FROM artists WHERE id = :id")
    suspend fun getById(id: String): ArtistEntity?

    suspend fun getByIdOrCreate(name: String): ArtistEntity {
        return getById(name) ?: ArtistEntity(
            id = name,
            name = name,
            genre = "Unknown",
            family = "Unknown",
            popularity = 0,
        ).also { upsert(it) }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(artists: List<ArtistEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(artist: ArtistEntity)

    @Query("DELETE FROM artists WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
