package com.futura.audio.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.futura.audio.data.db.entity.PlayJournalEntryEntity
import com.futura.audio.data.db.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayJournalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: PlayJournalEntryEntity): Long

    @Query("SELECT COUNT(*) FROM play_journal")
    suspend fun count(): Int

    @Query(
        """
        DELETE FROM play_journal WHERE id IN (
            SELECT id FROM play_journal ORDER BY playedAtEpochMs ASC LIMIT :count
        )
        """,
    )
    suspend fun deleteOldest(count: Int)

    @Query("DELETE FROM play_journal")
    suspend fun clearAll()

    @Query("SELECT * FROM play_journal ORDER BY playedAtEpochMs DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<PlayJournalEntryEntity>

    @Query("SELECT * FROM play_journal ORDER BY playedAtEpochMs DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<PlayJournalEntryEntity>>

    @Query(
        """
        SELECT t.* FROM tracks AS t
        INNER JOIN play_journal AS j ON j.trackId = t.id
        WHERE j.id IN (
            SELECT MAX(id) FROM play_journal GROUP BY trackId
        )
        ORDER BY j.playedAtEpochMs DESC
        LIMIT :limit
        """,
    )
    suspend fun getRecentDistinctTracks(limit: Int): List<TrackEntity>
}
