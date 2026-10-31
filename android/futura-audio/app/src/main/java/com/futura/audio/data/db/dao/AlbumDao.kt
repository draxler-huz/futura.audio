package com.futura.audio.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.futura.audio.data.db.entity.AlbumEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums ORDER BY title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums ORDER BY title COLLATE NOCASE ASC")
    suspend fun getAll(): List<AlbumEntity>

    @Query("SELECT * FROM albums WHERE id = :id")
    suspend fun getById(id: String): AlbumEntity?

    suspend fun getByIdOrCreate(title: String, artistId: String): AlbumEntity {
        return getById(title) ?: AlbumEntity(
            id = title,
            title = title,
            artistId = artistId,
            year = 2024,
            hue = Math.floorMod(title.hashCode(), 360),
        ).also { upsert(it) }
    }

    @Query(
        """
        SELECT * FROM albums
        WHERE title = :title COLLATE NOCASE AND artistId = :artistId
        LIMIT 1
        """,
    )
    suspend fun getByTitleAndArtist(title: String, artistId: String): AlbumEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(albums: List<AlbumEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(album: AlbumEntity)
}
