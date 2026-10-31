package com.futura.audio.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.futura.audio.data.db.entity.TrackEntity

@Dao
interface TrackFtsDao {
    /** Inserts without specifying rowid so SQLite assigns a fresh FTS rowid. */
    @Query(
        """
        INSERT INTO tracks_fts(trackId, title, artistName, albumTitle)
        VALUES (:trackId, :title, :artistName, :albumTitle)
        """,
    )
    suspend fun insertRaw(
        trackId: String,
        title: String,
        artistName: String,
        albumTitle: String,
    )

    @Query("DELETE FROM tracks_fts WHERE trackId = :trackId")
    suspend fun deleteByTrackId(trackId: String)

    @Query("DELETE FROM tracks_fts WHERE trackId IN (:trackIds)")
    suspend fun deleteByTrackIds(trackIds: List<String>)

    @Query("DELETE FROM tracks_fts")
    suspend fun clearAll()

    @Transaction
    suspend fun upsert(
        trackId: String,
        title: String,
        artistName: String,
        albumTitle: String,
    ) {
        deleteByTrackId(trackId)
        insertRaw(trackId, title, artistName, albumTitle)
    }

    /**
     * FTS4 MATCH over title / artistName / albumTitle.
     * [query] should already be sanitized (no bare special tokens).
     */
    @Query(
        """
        SELECT t.* FROM tracks AS t
        WHERE t.id IN (
            SELECT trackId FROM tracks_fts WHERE tracks_fts MATCH :query
        )
        ORDER BY t.title COLLATE NOCASE ASC
        """,
    )
    suspend fun search(query: String): List<TrackEntity>
}
