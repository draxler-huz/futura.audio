package com.futura.audio.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Circular recently-played journal row.
 * Cap / eviction is enforced by [com.futura.audio.data.library.RecentlyPlayedJournal].
 */
@Entity(
    tableName = "play_journal",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("trackId"),
        Index("playedAtEpochMs"),
    ],
)
data class PlayJournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: String,
    val playedAtEpochMs: Long,
    /** Fraction of the track heard in this play event (0f..1f). */
    val completionRatio: Float,
)
