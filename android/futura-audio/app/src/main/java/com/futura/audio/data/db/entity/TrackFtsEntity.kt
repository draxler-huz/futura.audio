package com.futura.audio.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * FTS4 virtual table over track title + artist name + album title (item Stage C).
 * Populated by [com.futura.audio.data.scan.LibraryIndexer] / [com.futura.audio.data.library.LibraryFtsSearch].
 * Uses FTS4 (not FTS5) for broader device/SQLite compatibility.
 */
@Fts4
@Entity(tableName = "tracks_fts")
data class TrackFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Int = 0,
    val trackId: String,
    val title: String,
    val artistName: String,
    val albumTitle: String,
)
