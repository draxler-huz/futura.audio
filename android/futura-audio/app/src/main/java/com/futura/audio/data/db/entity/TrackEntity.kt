package com.futura.audio.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Library track. Core fields match design `lib/library-data.ts` Track.
 *
 * [valence] / [energy] are indexer or seed values (0f..1f).
 * [valenceOverride] / [energyOverride] are user edits; re-indexing must never overwrite them.
 * Effective mood values: override ?: base.
 */
@Entity(
    tableName = "tracks",
    foreignKeys = [
        ForeignKey(
            entity = ArtistEntity::class,
            parentColumns = ["id"],
            childColumns = ["artistId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("artistId"),
        Index("albumId"),
    ],
)
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artistId: String,
    val albumId: String,
    /** Duration in seconds. */
    val duration: Int,
    val plays: Int,
    val addedDaysAgo: Int,
    val lastPlayedDaysAgo: Int?,
    val valence: Float,
    val energy: Float,
    /** User override for valence; null means use [valence]. */
    val valenceOverride: Float? = null,
    /** User override for energy; null means use [energy]. */
    val energyOverride: Float? = null,
    val favorite: Boolean,
    val folder: String,
    val fileName: String,
) {
    fun effectiveValence(): Float = valenceOverride ?: valence
    fun effectiveEnergy(): Float = energyOverride ?: energy
}
