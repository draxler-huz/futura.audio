package com.futura.audio.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** User-created playlist. Smart filter chips are not stored here. */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAtEpochMs: Long,
)
