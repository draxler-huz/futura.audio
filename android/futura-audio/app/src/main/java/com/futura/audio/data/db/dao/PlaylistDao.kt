package com.futura.audio.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.futura.audio.data.db.entity.PlaylistEntity
import com.futura.audio.data.db.entity.PlaylistTrackEntity
import com.futura.audio.data.db.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getById(id: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTracks(rows: List<PlaylistTrackEntity>)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearTracks(playlistId: String)

    @Query(
        """
        DELETE FROM playlist_tracks
        WHERE playlistId = :playlistId AND trackId = :trackId
        """,
    )
    suspend fun deleteTrackRow(playlistId: String, trackId: String)

    @Query(
        """
        SELECT COALESCE(MAX(position), -1) FROM playlist_tracks
        WHERE playlistId = :playlistId
        """,
    )
    suspend fun maxPosition(playlistId: String): Int

    @Query(
        """
        SELECT trackId FROM playlist_tracks
        WHERE playlistId = :playlistId
        ORDER BY position ASC
        """,
    )
    suspend fun getTrackIdsOrdered(playlistId: String): List<String>

    @Query(
        """
        SELECT t.* FROM tracks AS t
        INNER JOIN playlist_tracks AS pt ON pt.trackId = t.id
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.position ASC
        """,
    )
    fun observeTracks(playlistId: String): Flow<List<TrackEntity>>

    @Query(
        """
        SELECT t.* FROM tracks AS t
        INNER JOIN playlist_tracks AS pt ON pt.trackId = t.id
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.position ASC
        """,
    )
    suspend fun getTracks(playlistId: String): List<TrackEntity>

    @Transaction
    suspend fun replaceTracks(playlistId: String, trackIds: List<String>) {
        clearTracks(playlistId)
        upsertTracks(
            trackIds.mapIndexed { index, trackId ->
                PlaylistTrackEntity(
                    playlistId = playlistId,
                    trackId = trackId,
                    position = index,
                )
            },
        )
    }

    /** Appends [trackId] at the end of the playlist (item 33). */
    @Transaction
    suspend fun addTrack(playlistId: String, trackId: String) {
        val next = maxPosition(playlistId) + 1
        upsertTracks(
            listOf(
                PlaylistTrackEntity(
                    playlistId = playlistId,
                    trackId = trackId,
                    position = next,
                ),
            ),
        )
    }

    /** Removes [trackId] and compact-renumbers remaining positions (item 33). */
    @Transaction
    suspend fun removeTrack(playlistId: String, trackId: String) {
        deleteTrackRow(playlistId, trackId)
        val remaining = getTrackIdsOrdered(playlistId)
        clearTracks(playlistId)
        upsertTracks(
            remaining.mapIndexed { index, id ->
                PlaylistTrackEntity(
                    playlistId = playlistId,
                    trackId = id,
                    position = index,
                )
            },
        )
    }

    /** Replaces membership order with [orderedTrackIds] (item 33). */
    @Transaction
    suspend fun reorderTracks(playlistId: String, orderedTrackIds: List<String>) {
        replaceTracks(playlistId, orderedTrackIds)
    }
}
