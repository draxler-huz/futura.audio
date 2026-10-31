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
 *
 * Scan/index columns (Phase 2) are nullable for seed rows; [completionRatioSum] /
 * [playEventCount] default to 0 and accumulate play-completion stats.
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
        Index(value = ["absolutePath"], unique = false),
        Index(value = ["mediaStoreId"], unique = false),
        Index("favorite"),
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
    // --- Phase 2 scan / index fields ---
    val absolutePath: String? = null,
    val contentUri: String? = null,
    val mediaStoreId: Long? = null,
    val format: String? = null,
    val md5: String? = null,
    val sha1: String? = null,
    val volumeUuid: String? = null,
    val durationMs: Long? = null,
    val genre: String? = null,
    val normalizedGenre: String? = null,
    val lyrics: String? = null,
    val coverCacheKey: String? = null,
    val fileSizeBytes: Long? = null,
    val lastModifiedEpochMs: Long? = null,
    val completionRatioSum: Float = 0f,
    val playEventCount: Int = 0,
) {
    fun effectiveValence(): Float = valenceOverride ?: valence
    fun effectiveEnergy(): Float = energyOverride ?: energy
}
