package com.futura.audio.ui.library

/**
 * One library entry. Playback is indexed by position in [LibraryCatalog.tracks], which is the same
 * order the view model uses for selectTrack and loadTrackPcm.
 *
 * The 36 library entries are generated from the design 2 prototype's lib/library-data.ts. The five
 * design 1.1 preview tracks come first, so the app still opens on Neon Meridian.
 */
data class LibraryTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val family: String,
    val durationSeconds: Int,
    val plays: Int,
    val addedDaysAgo: Int,
    val lastPlayedDaysAgo: Int?,
    val valence: Float,
    val energy: Float,
    val favorite: Boolean,
    val folder: String,
    val fileName: String,
)

object LibraryCatalog {
    private fun t(
        id: String,
        title: String,
        artist: String,
        album: String,
        genre: String,
        family: String,
        durationSeconds: Int,
        plays: Int,
        addedDaysAgo: Int,
        lastPlayedDaysAgo: Int?,
        valence: Float,
        energy: Float,
        favorite: Boolean,
        folder: String,
        fileName: String,
    ) = LibraryTrack(
        id = id,
        title = title,
        artist = artist,
        album = album,
        genre = genre,
        family = family,
        durationSeconds = durationSeconds,
        plays = plays,
        addedDaysAgo = addedDaysAgo,
        lastPlayedDaysAgo = lastPlayedDaysAgo,
        valence = valence,
        energy = energy,
        favorite = favorite,
        folder = folder,
        fileName = fileName,
    )

    val tracks: List<LibraryTrack> = listOf(
        // Design 1.1 preview tracks. Ids match MusicCatalog. Genre and folder are placeholders.
        t("neon-meridian", "Neon Meridian", "Vela Kade & Oris", "Signal Bloom", "Design preview", "Electronic", 236, 0, 0, null, 0.62f, 0.78f, false, "Music/Electronic/Design preview/Vela Kade & Oris/Signal Bloom", "01 - Neon Meridian.flac"),
        t("glass-orbit", "Glass Orbit", "Lumen Arc", "Refraction", "Design preview", "Electronic", 212, 0, 0, null, 0.71f, 0.52f, false, "Music/Electronic/Design preview/Lumen Arc/Refraction", "01 - Glass Orbit.flac"),
        t("amber-signal", "Amber Signal", "Kōdo", "Topograph", "Design preview", "Electronic", 254, 0, 0, null, 0.48f, 0.84f, false, "Music/Electronic/Design preview/Kōdo/Topograph", "01 - Amber Signal.flac"),
        t("deep-current", "Deep Current", "Marin Sølv", "Abyssal", "Design preview", "Electronic", 301, 0, 0, null, 0.4f, 0.34f, false, "Music/Electronic/Design preview/Marin Sølv/Abyssal", "01 - Deep Current.flac"),
        t("night-protocol", "Night Protocol", "Hexa Drive", "Lattice", "Design preview", "Electronic", 228, 0, 0, null, 0.35f, 0.81f, false, "Music/Electronic/Design preview/Hexa Drive/Lattice", "01 - Night Protocol.flac"),
        // Library tracks from the design 2 prototype, in its order.
        t("lib-1", "Afterimage Drive", "Neon Halcyon", "Chrome Horizon", "Synthwave", "Electronic", 241, 128, 40, 0, 0.72f, 0.82f, true, "Music/Electronic/Synthwave/Neon Halcyon/Chrome Horizon (2025)", "01 - Afterimage Drive.flac"),
        t("lib-2", "Chrome Horizon", "Neon Halcyon", "Chrome Horizon", "Synthwave", "Electronic", 263, 96, 40, 1, 0.8f, 0.74f, false, "Music/Electronic/Synthwave/Neon Halcyon/Chrome Horizon (2025)", "02 - Chrome Horizon.flac"),
        t("lib-3", "Coastline Protocol", "Neon Halcyon", "Chrome Horizon", "Synthwave", "Electronic", 219, 0, 3, null, 0.66f, 0.7f, false, "Music/Electronic/Synthwave/Neon Halcyon/Chrome Horizon (2025)", "03 - Coastline Protocol.flac"),
        t("lib-4", "Midnight Overpass", "Neon Halcyon", "Night Transit", "Synthwave", "Electronic", 287, 54, 210, 6, 0.42f, 0.66f, false, "Music/Electronic/Synthwave/Neon Halcyon/Night Transit (2023)", "01 - Midnight Overpass.flac"),
        t("lib-5", "Transit Hymn", "Neon Halcyon", "Night Transit", "Synthwave", "Electronic", 302, 12, 210, 30, 0.35f, 0.48f, false, "Music/Electronic/Synthwave/Neon Halcyon/Night Transit (2023)", "02 - Transit Hymn.flac"),
        t("lib-6", "Bassline Theory", "MXRA", "Pressure Systems", "Techno", "Electronic", 376, 201, 12, 0, 0.3f, 0.95f, true, "Music/Electronic/Techno/MXRA/Pressure Systems (2026)", "01 - Bassline Theory.flac"),
        t("lib-7", "Ion Storm", "MXRA", "Pressure Systems", "Techno", "Electronic", 412, 77, 12, 2, 0.18f, 0.92f, false, "Music/Electronic/Techno/MXRA/Pressure Systems (2026)", "02 - Ion Storm.flac"),
        t("lib-8", "Static Pressure", "MXRA", "Pressure Systems", "Techno", "Electronic", 355, 0, 1, null, 0.22f, 0.88f, false, "Music/Electronic/Techno/MXRA/Pressure Systems (2026)", "03 - Static Pressure.flac"),
        t("lib-9", "Sunroof", "Orbit Lanes", "Sunroof Sessions", "House", "Electronic", 228, 143, 90, 0, 0.92f, 0.8f, true, "Music/Electronic/House/Orbit Lanes/Sunroof Sessions (2024)", "01 - Sunroof.flac"),
        t("lib-10", "Golden Hour Loop", "Orbit Lanes", "Sunroof Sessions", "House", "Electronic", 246, 61, 90, 4, 0.86f, 0.68f, false, "Music/Electronic/House/Orbit Lanes/Sunroof Sessions (2024)", "02 - Golden Hour Loop.flac"),
        t("lib-11", "Velvet Floor", "Orbit Lanes", "Sunroof Sessions", "House", "Electronic", 271, 0, 5, null, 0.74f, 0.72f, false, "Music/Electronic/House/Orbit Lanes/Sunroof Sessions (2024)", "03 - Velvet Floor.flac"),
        t("lib-12", "Fractal Rush", "Hexa Bloom", "Fractal Rush", "Drum & Bass", "Electronic", 312, 88, 25, 1, 0.55f, 0.98f, false, "Music/Electronic/Drum & Bass/Hexa Bloom/Fractal Rush (2025)", "01 - Fractal Rush.flac"),
        t("lib-13", "Kinetic Bloom", "Hexa Bloom", "Fractal Rush", "Drum & Bass", "Electronic", 298, 34, 25, 9, 0.62f, 0.94f, false, "Music/Electronic/Drum & Bass/Hexa Bloom/Fractal Rush (2025)", "02 - Kinetic Bloom.flac"),
        t("lib-14", "Zero Gravity", "Hexa Bloom", "Fractal Rush", "Drum & Bass", "Electronic", 284, 0, 2, null, 0.48f, 0.9f, false, "Music/Electronic/Drum & Bass/Hexa Bloom/Fractal Rush (2025)", "03 - Zero Gravity.flac"),
        t("lib-15", "Quiet Satellites", "Kairo Drift", "Quiet Satellites", "Ambient", "Chill", 458, 72, 400, 3, 0.62f, 0.12f, true, "Music/Chill/Ambient/Kairo Drift/Quiet Satellites (2022)", "01 - Quiet Satellites.flac"),
        t("lib-16", "Lagrange Point", "Kairo Drift", "Quiet Satellites", "Ambient", "Chill", 521, 18, 400, 45, 0.48f, 0.08f, false, "Music/Chill/Ambient/Kairo Drift/Quiet Satellites (2022)", "02 - Lagrange Point.flac"),
        t("lib-17", "Ozone Lullaby", "Kairo Drift", "Quiet Satellites", "Ambient", "Chill", 489, 0, 400, null, 0.7f, 0.15f, false, "Music/Chill/Ambient/Kairo Drift/Quiet Satellites (2022)", "03 - Ozone Lullaby.flac"),
        t("lib-18", "Tape Weather", "Low Tide Club", "Tape Weather", "Lo-fi", "Chill", 162, 110, 60, 0, 0.6f, 0.3f, false, "Music/Chill/Lo-fi/Low Tide Club/Tape Weather (2024)", "01 - Tape Weather.flac"),
        t("lib-19", "Rainy Window", "Low Tide Club", "Tape Weather", "Lo-fi", "Chill", 148, 87, 60, 2, 0.38f, 0.22f, true, "Music/Chill/Lo-fi/Low Tide Club/Tape Weather (2024)", "02 - Rainy Window.flac"),
        t("lib-20", "Coffee Ring", "Low Tide Club", "Tape Weather", "Lo-fi", "Chill", 171, 0, 8, null, 0.68f, 0.34f, false, "Music/Chill/Lo-fi/Low Tide Club/Tape Weather (2024)", "03 - Coffee Ring.flac"),
        t("lib-21", "Bloomfuzz", "Velvet Static", "Bloomfuzz", "Shoegaze", "Indie", 334, 45, 150, 12, 0.34f, 0.6f, false, "Music/Indie/Shoegaze/Velvet Static/Bloomfuzz (2023)", "01 - Bloomfuzz.flac"),
        t("lib-22", "Underwater Choir", "Velvet Static", "Bloomfuzz", "Shoegaze", "Indie", 367, 22, 150, 20, 0.2f, 0.42f, false, "Music/Indie/Shoegaze/Velvet Static/Bloomfuzz (2023)", "02 - Underwater Choir.flac"),
        t("lib-23", "Haze Signal", "Velvet Static", "Bloomfuzz", "Shoegaze", "Indie", 312, 0, 150, null, 0.26f, 0.55f, false, "Music/Indie/Shoegaze/Velvet Static/Bloomfuzz (2023)", "03 - Haze Signal.flac"),
        t("lib-24", "Paper Lanterns", "Juniper Echo", "Paper Lanterns", "Indie Rock", "Indie", 213, 66, 4, 1, 0.78f, 0.58f, false, "Music/Indie/Indie Rock/Juniper Echo/Paper Lanterns (2026)", "01 - Paper Lanterns.flac"),
        t("lib-25", "Exit Wounds", "Juniper Echo", "Paper Lanterns", "Indie Rock", "Indie", 237, 15, 4, 5, 0.15f, 0.7f, false, "Music/Indie/Indie Rock/Juniper Echo/Paper Lanterns (2026)", "02 - Exit Wounds.flac"),
        t("lib-26", "Quarry Lights", "Juniper Echo", "Paper Lanterns", "Indie Rock", "Indie", 198, 0, 4, null, 0.58f, 0.52f, false, "Music/Indie/Indie Rock/Juniper Echo/Paper Lanterns (2026)", "03 - Quarry Lights.flac"),
        t("lib-27", "Lunar Lullaby", "Cassia Moon", "Lunar Lullabies", "Dream Pop", "Indie", 276, 92, 30, 0, 0.64f, 0.26f, true, "Music/Indie/Dream Pop/Cassia Moon/Lunar Lullabies (2025)", "01 - Lunar Lullaby.flac"),
        t("lib-28", "Moth Season", "Cassia Moon", "Lunar Lullabies", "Dream Pop", "Indie", 254, 38, 30, 7, 0.28f, 0.32f, false, "Music/Indie/Dream Pop/Cassia Moon/Lunar Lullabies (2025)", "02 - Moth Season.flac"),
        t("lib-29", "Dew Point", "Cassia Moon", "Lunar Lullabies", "Dream Pop", "Indie", 241, 0, 30, null, 0.52f, 0.2f, false, "Music/Indie/Dream Pop/Cassia Moon/Lunar Lullabies (2025)", "03 - Dew Point.flac"),
        t("lib-30", "Mirage", "Saint Vapor", "Mirage Hours", "Alt R&B", "Soul", 229, 156, 120, 0, 0.56f, 0.46f, true, "Music/Soul/Alt R&B/Saint Vapor/Mirage Hours (2024)", "01 - Mirage.flac"),
        t("lib-31", "Heatwave Halo", "Saint Vapor", "Mirage Hours", "Alt R&B", "Soul", 244, 49, 120, 3, 0.8f, 0.55f, false, "Music/Soul/Alt R&B/Saint Vapor/Mirage Hours (2024)", "02 - Heatwave Halo.flac"),
        t("lib-32", "Slow Burn", "Saint Vapor", "Mirage Hours", "Alt R&B", "Soul", 268, 26, 120, 15, 0.24f, 0.38f, false, "Music/Soul/Alt R&B/Saint Vapor/Mirage Hours (2024)", "03 - Slow Burn.flac"),
        t("lib-33", "Glass Ceiling", "Saint Vapor", "Glass Ceiling", "Alt R&B", "Soul", 233, 9, 6, 2, 0.44f, 0.62f, false, "Music/Soul/Alt R&B/Saint Vapor/Glass Ceiling (2026)", "01 - Glass Ceiling.flac"),
        t("lib-34", "Yearning Static", "Saint Vapor", "Glass Ceiling", "Alt R&B", "Soul", 252, 0, 6, null, 0.12f, 0.28f, false, "Music/Soul/Alt R&B/Saint Vapor/Glass Ceiling (2026)", "02 - Yearning Static.flac"),
        t("lib-35", "Ultraviolet", "Saint Vapor", "Glass Ceiling", "Alt R&B", "Soul", 219, 0, 6, null, 0.82f, 0.86f, false, "Music/Soul/Alt R&B/Saint Vapor/Glass Ceiling (2026)", "03 - Ultraviolet.flac"),
        t("lib-36", "Echo Park Ghost", "Juniper Echo", "Paper Lanterns", "Indie Rock", "Indie", 205, 3, 4, 60, 0.4f, 0.45f, false, "Music/Indie/Indie Rock/Juniper Echo/Paper Lanterns (2026)", "04 - Echo Park Ghost.flac"),
    )
}
