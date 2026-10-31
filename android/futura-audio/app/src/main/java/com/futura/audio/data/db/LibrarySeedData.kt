package com.futura.audio.data.db

import com.futura.audio.data.db.entity.AlbumEntity
import com.futura.audio.data.db.entity.ArtistEntity
import com.futura.audio.data.db.entity.TrackEntity

/**
 * Port of design `lib/library-data.ts` (artists, albums, tracks).
 * Ids and field shapes are kept identical to the prototype.
 */
object LibrarySeedData {
    val artists: List<ArtistEntity> = listOf(
        ArtistEntity("a1", "Neon Halcyon", "Synthwave", "Electronic", 200),
        ArtistEntity("a2", "MXRA", "Techno", "Electronic", 260),
        ArtistEntity("a3", "Orbit Lanes", "House", "Electronic", 180),
        ArtistEntity("a4", "Hexa Bloom", "Drum & Bass", "Electronic", 300),
        ArtistEntity("a5", "Kairo Drift", "Ambient", "Chill", 140),
        ArtistEntity("a6", "Low Tide Club", "Lo-fi", "Chill", 110),
        ArtistEntity("a7", "Velvet Static", "Shoegaze", "Indie", 330),
        ArtistEntity("a8", "Juniper Echo", "Indie Rock", "Indie", 20),
        ArtistEntity("a9", "Cassia Moon", "Dream Pop", "Indie", 350),
        ArtistEntity("a10", "Saint Vapor", "Alt R&B", "Soul", 60),
    )

    val albums: List<AlbumEntity> = listOf(
        AlbumEntity("al1", "Chrome Horizon", "a1", 2025, 205),
        AlbumEntity("al2", "Night Transit", "a1", 2023, 230),
        AlbumEntity("al3", "Pressure Systems", "a2", 2026, 270),
        AlbumEntity("al4", "Sunroof Sessions", "a3", 2024, 170),
        AlbumEntity("al5", "Fractal Rush", "a4", 2025, 300),
        AlbumEntity("al6", "Quiet Satellites", "a5", 2022, 150),
        AlbumEntity("al7", "Tape Weather", "a6", 2024, 100),
        AlbumEntity("al8", "Bloomfuzz", "a7", 2023, 325),
        AlbumEntity("al9", "Paper Lanterns", "a8", 2026, 25),
        AlbumEntity("al10", "Lunar Lullabies", "a9", 2025, 355),
        AlbumEntity("al11", "Mirage Hours", "a10", 2024, 55),
        AlbumEntity("al12", "Glass Ceiling", "a10", 2026, 40),
    )

    /**
     * Row shape from the design: title, albumId, durationSec, plays, addedDaysAgo,
     * lastPlayedDaysAgo, valence, energy, favorite?
     */
    private data class Row(
        val title: String,
        val albumId: String,
        val duration: Int,
        val plays: Int,
        val addedDaysAgo: Int,
        val lastPlayedDaysAgo: Int?,
        val valence: Float,
        val energy: Float,
        val favorite: Boolean = false,
    )

    private val rows: List<Row> = listOf(
        Row("Afterimage Drive", "al1", 241, 128, 40, 0, 0.72f, 0.82f, true),
        Row("Chrome Horizon", "al1", 263, 96, 40, 1, 0.8f, 0.74f),
        Row("Coastline Protocol", "al1", 219, 0, 3, null, 0.66f, 0.7f),
        Row("Midnight Overpass", "al2", 287, 54, 210, 6, 0.42f, 0.66f),
        Row("Transit Hymn", "al2", 302, 12, 210, 30, 0.35f, 0.48f),
        Row("Bassline Theory", "al3", 376, 201, 12, 0, 0.3f, 0.95f, true),
        Row("Ion Storm", "al3", 412, 77, 12, 2, 0.18f, 0.92f),
        Row("Static Pressure", "al3", 355, 0, 1, null, 0.22f, 0.88f),
        Row("Sunroof", "al4", 228, 143, 90, 0, 0.92f, 0.8f, true),
        Row("Golden Hour Loop", "al4", 246, 61, 90, 4, 0.86f, 0.68f),
        Row("Velvet Floor", "al4", 271, 0, 5, null, 0.74f, 0.72f),
        Row("Fractal Rush", "al5", 312, 88, 25, 1, 0.55f, 0.98f),
        Row("Kinetic Bloom", "al5", 298, 34, 25, 9, 0.62f, 0.94f),
        Row("Zero Gravity", "al5", 284, 0, 2, null, 0.48f, 0.9f),
        Row("Quiet Satellites", "al6", 458, 72, 400, 3, 0.62f, 0.12f, true),
        Row("Lagrange Point", "al6", 521, 18, 400, 45, 0.48f, 0.08f),
        Row("Ozone Lullaby", "al6", 489, 0, 400, null, 0.7f, 0.15f),
        Row("Tape Weather", "al7", 162, 110, 60, 0, 0.6f, 0.3f),
        Row("Rainy Window", "al7", 148, 87, 60, 2, 0.38f, 0.22f, true),
        Row("Coffee Ring", "al7", 171, 0, 8, null, 0.68f, 0.34f),
        Row("Bloomfuzz", "al8", 334, 45, 150, 12, 0.34f, 0.6f),
        Row("Underwater Choir", "al8", 367, 22, 150, 20, 0.2f, 0.42f),
        Row("Haze Signal", "al8", 312, 0, 150, null, 0.26f, 0.55f),
        Row("Paper Lanterns", "al9", 213, 66, 4, 1, 0.78f, 0.58f),
        Row("Exit Wounds", "al9", 237, 15, 4, 5, 0.15f, 0.7f),
        Row("Quarry Lights", "al9", 198, 0, 4, null, 0.58f, 0.52f),
        Row("Lunar Lullaby", "al10", 276, 92, 30, 0, 0.64f, 0.26f, true),
        Row("Moth Season", "al10", 254, 38, 30, 7, 0.28f, 0.32f),
        Row("Dew Point", "al10", 241, 0, 30, null, 0.52f, 0.2f),
        Row("Mirage", "al11", 229, 156, 120, 0, 0.56f, 0.46f, true),
        Row("Heatwave Halo", "al11", 244, 49, 120, 3, 0.8f, 0.55f),
        Row("Slow Burn", "al11", 268, 26, 120, 15, 0.24f, 0.38f),
        Row("Glass Ceiling", "al12", 233, 9, 6, 2, 0.44f, 0.62f),
        Row("Yearning Static", "al12", 252, 0, 6, null, 0.12f, 0.28f),
        Row("Ultraviolet", "al12", 219, 0, 6, null, 0.82f, 0.86f),
        Row("Echo Park Ghost", "al9", 205, 3, 4, 60, 0.4f, 0.45f),
    )

    val tracks: List<TrackEntity> = run {
        val albumById = albums.associateBy { it.id }
        val artistById = artists.associateBy { it.id }
        val trackCountByAlbum = mutableMapOf<String, Int>()
        rows.mapIndexed { index, row ->
            val album = albumById.getValue(row.albumId)
            val artist = artistById.getValue(album.artistId)
            val trackNo = (trackCountByAlbum[row.albumId] ?: 0) + 1
            trackCountByAlbum[row.albumId] = trackNo
            TrackEntity(
                id = "t${index + 1}",
                title = row.title,
                artistId = artist.id,
                albumId = album.id,
                duration = row.duration,
                plays = row.plays,
                addedDaysAgo = row.addedDaysAgo,
                lastPlayedDaysAgo = row.lastPlayedDaysAgo,
                valence = row.valence,
                energy = row.energy,
                valenceOverride = null,
                energyOverride = null,
                favorite = row.favorite,
                folder = "Music/${artist.family}/${artist.genre}/${artist.name}/${album.title} (${album.year})",
                fileName = "${trackNo.toString().padStart(2, '0')} - ${row.title}.flac",
            )
        }
    }
}
