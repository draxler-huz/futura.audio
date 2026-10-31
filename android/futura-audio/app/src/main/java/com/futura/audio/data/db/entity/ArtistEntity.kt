package com.futura.audio.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Library artist. Field names match design `lib/library-data.ts` Artist.
 * [family] is one of Electronic | Chill | Indie | Soul.
 */
@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val genre: String,
    val family: String,
    val hue: Int,
)
