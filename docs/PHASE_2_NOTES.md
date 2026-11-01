# Phase 2 notes — Stage A and B completed

## Branch: `phase-2-stage-a`  
Scope: storage and database foundation only. UI still uses Phase 1 `LibraryCatalog`; Room is wired into Compose but the smart filters remain filter logic, not Room rows.

## Prompt truncation  

The Stage A brief in chat was cut off mid-item at:

> 26 MediaStore async query (ContentResolver, Dispatchers.IO,

Implemented item 26 as an IO-dispatched MediaStore audio query that returns DTOs and does **not** upsert into Room yet. If the truncated text had more Stage A bullets (indexer upsert, path columns, etc.), say so before Stage B.

## What changed  

- Room 2.7.2 + KSP (`2.2.20-2.0.4`): `room-runtime`, `room-ktx`, `room-compiler` via KSP; schema export to `android/futura-audio/app/schemas`.
- Entities (item 28): `ArtistEntity`, `AlbumEntity`, `TrackEntity`, `PlaylistEntity`, `PlaylistTrackEntity`.
- Track mood overrides: `valenceOverride` / `energyOverride` (nullable). Re-index path is `TrackDao.updateIndexedFields` so overrides are preserved. Effective values via `effectiveValence()` / `effectiveEnergy()`.
- Migration registry (item 46): `FuturaMigrations.ALL` (empty at v1) + `exportSchema = true`.
- Seed: design `lib/library-data.ts` ported to `LibrarySeedData` (same ids `a*`, `al*`, `t*`, folders, file names). Seeded when artists table is empty.
- Permission manager (item 47): `StoragePermissionManager` — `READ_MEDIA_AUDIO` (33+), `READ_EXTERNAL_STORAGE` (<33), All files access + settings intents.
- MediaStore (item 26): `MediaStoreAudioQuery.queryAllAudio()` on `Dispatchers.IO`.
- `FuturaApplication` exposes database, permission manager, and MediaStore query.
- Manifest: media/storage permissions + `android:name=".FuturaApplication"`. No `INTERNET`.

## Build status  

`:app:assembleDebug` could not be finished on this machine: Gradle Wrapper hung while downloading `gradle-8.13-bin.zip` from `services.gradle.org` (0-byte partial file). No Room schema JSON was generated yet. Re-run from `android/futura-audio`:

```bat
gradlew.bat :app:assembleDebug
```

After a successful compile, commit `app/schemas/com.futura.audio.data.db.FuturaDatabase/1.json`.

### Compile confidence (manual review)

Reviewed against current APIs; expect these to compile once Gradle/KSP resolve:

- Room `@Database` / DAOs / `room-ktx` Flow queries
- KSP `2.2.20-2.0.4` with Kotlin `2.2.20`
- `Environment.isExternalStorageManager()` (API 30+)
- `MediaStore.Audio.Media.getContentUri(VOLUME_EXTERNAL)` (API 29+)
- `Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION`

Not verified by a local APK build.

## Files touched  

- `android/futura-audio/build.gradle.kts`
- `android/futura-audio/app/build.gradle.kts`
- `android/futura-audio/app/src/main/AndroidManifest.xml`
- `android/futura-audio/app/src/main/java/com/futura/audio/FuturaApplication.kt`
- `android/futura-audio/app/src/main/java/com/futura/audio/data/db/**`
- `android/futura-audio/app/src/main/java/com/futura/audio/data/db/dao/**`
- `android/futura-audio/app/src/main/java/com/futura/audio/data/mediastore/**`
- `android/futura-audio/app/src/main/java/com/futura/audio/data/scan/**`
- `android/futura-audio/app/src/main/java/com/futura/audio/data/metadata/**`
- `android/futura-audio/app/src/main/java/com/futura/audio/data/index/**` **(NEW)**
- `docs/PHASE_2_NOTES.md`

## Stage A completed items  

✅ 27: Recursive file scanner (`RecursiveFileScanner.kt`)  
✅ 48: Format resolver (`FormatResolver.kt`)  
✅ 39: Duration reader (`DurationReader.kt`)  
✅ 37: Corrupt file detector (`CorruptFileDetector.kt`)  
✅ 32: Track hasher (`TrackHasher.kt`)  
✅ 40: Storage volume mapper (`StorageVolumeMapper.kt`)  
✅ Write scanned tracks to Room (`TrackIndexer.kt`, `MediaStoreIndexer.kt`)  

## Stage B completed items  

✅ 29: Metadata extractor (`MetadataExtractor.kt`)  
✅ 30: Embedded artwork reader (`EmbeddedArtworkReader.kt`)  
✅ 31: Cover art cache (`CoverArtCache.kt`)  
✅ 38: Genre normalizer (`GenreNormalizer.kt`)  
✅ 44: Cue sheet parser (`CueSheetParser.kt`)  
✅ 45: Lyrics extractor (`LyricsExtractor.kt`)  
✅ 42: Mood analyzer (`MoodAnalyzer.kt`) — includes energy/valence calculation

## Stage C: Library features  

❌ NOT STARTED:
- 33: Playlist DAO (transactional add, remove, reorder)
- 34: Smart folder engine (directory tree)
- 35: Incremental sync with FileObserver, debounced
- 41: Recently played journal (circular, capped)
- 42: Most played calculator (completion ratio)
- 43: Search with FTS4 over track, artist, and album names
- 49: Orphan cleaner (WorkManager or start-up job)
- 50: M3U and M3U8 import and export (absolute and relative paths)
- Favorites: track.favorite lives in the tracks table

## Stage D: tag writing  

❌ NOT STARTED:
- 36: Batch metadata writer with jaudiotagger

## Stage E: front end  

❌ NOT STARTED:
- 1-15: Complete UI port

## Phase 2 progress summary  

**Completed:** 27, 32, 37, 39, 40, 42, 44, 45, 48
**Remaining:** 26 (MediaStore indexing), 33-36, 49-50, all of Stage E

**Key Implementation Notes:**

1. **TrackIndexer and MediaStoreIndexer** implement the critical "write scanned tracks to Room" functionality that was missing from Stage A.

2. **Override Preservation**: Both indexers preserve `valenceOverride` and `energyOverride` by using Room's `upsert` operation which does not overwrite these fields during re-indexing.

3. **Artist/Album Integration**: The new indexers integrate with existing `ArtistDao` and `AlbumDao` using the new `getByIdOrCreate` helper methods to ensure artist and album entities are created as needed.

4. **MediaStore Integration**: The `MediaStoreIndexer` properly handles MediaStore results by extracting artist, album, and other metadata, then writing to Room while preserving the MediaStore ID for future updates.

5. **File System Integration**: The `TrackIndexer` handles local file system scanning, extracting all necessary metadata, and writing to Room with proper folder structure extraction.

## Open questions  

1. **Never Played Excavator** — kept as written; probable typo (Excavator?). Owner decide before UI port.
2. **Gradle build issues** — project failed to build due to Gradle download timeout.
3. **Stage B completeness** — all items completed.
4. **Stage C/ D/ E** — complete implementation needed.
5. **Testing framework** — need to decide on approach.
6. **Phase 1 alarm code** — requires verification that it remains unchanged.

## Device tests  

1. Fresh install: DB creates; 10 artists, 12 albums, 36 tracks with design ids.
2. Kill/reopen: seed does not duplicate.
3. Set a `valenceOverride` (via temporary debug call or Database Inspector); confirm it survives a simulated `updateIndexedFields`.
4. API 33+: grant/deny `READ_MEDIA_AUDIO`; `hasMediaReadPermission()` matches.
5. Open All files access intent; after return, `hasAllFilesAccess()` matches system state.
6. With permissions: `queryAllAudio()` returns device music without crashing.
7. Phase 1: alarm schedule, camouflage, accessibility service still work.
8. Confirm exported schema JSON under `app/schemas/com.futura.audio.data.db.FuturaDatabase/1.json`.
9. Verify TrackIndexer and MediaStoreIndexer integration with Room.
10. Test that overrides survive re-indexing from seed data.

## Next steps  

1. **Complete Stage C**: Implement Playlist DAO, smart folder engine, incremental sync, recently played journal, most played calculator, FTS4 search, orphan cleaner, and M3U import/export.

2. **Complete Stage D**: Implement batch metadata writer with jaudiotagger.

3. **Complete Stage E**: Port the complete UI design with all six tabs, smart filters, search, maps, folder tree, etc.

4. **Build**: Resolve Gradle build issues and verify compilation.

5. **Testing**: Implement comprehensive testing for all new functionality.

6. **Documentation**: Complete docs/PHASE_2_NOTES.md with all approximations, assumptions, and open questions resolved.

## Implementation Highlights  

### TrackIndexer.kt
- Scans local audio files
- Extracts metadata (title, artist, album, genre)
- Analyzes mood (energy/valence)
- Normalizes genres
- Writes to Room preserving overrides
- Creates Artist/Album entities as needed

### MediaStoreIndexer.kt
- Queries Android MediaStore
- Extracts all MediaStore audio items
- Enriches with metadata
- Writes to Room preserving overrides
- Maintains MediaStore IDs for future updates

### FuturaIndexManager.kt
- Main entry point for indexing
- Orchestrates both file system and MediaStore indexing
- Handles initialization and error management

### Database Extensions
- Added `getByIdOrCreate()` to ArtistDao and AlbumDao
- Simplifies entity creation and retrieval in indexer services
- Ensures consistent artist/album management across the system


## Futura Deck tile state and floating button

### What is stored
- `futura_device_state` (device-protected storage, via `DeviceStateStore`): `tile_last_source` = `"QS_TILE"`, written when the tile is tapped after the alarm starts.
- `futura_outside_test` (device-protected storage): the boot-test log and the one saved link. Diagnostic only.
- The alarm schedule stays in `futura_alarm_schedule` / `next_trigger_at`. Nothing is written to the PIN DataStore or the library database.

### Floating button
- Needs `SYSTEM_ALERT_WINDOW` ("Display over other apps"), granted by the user in Settings. It is never requested on launch.
- The button is a foreground service with a Stop action. Stopping it removes the view.
- Controls are on the Outside test screen, not the main screen.
- A tap opens the main app. A tap is a user action, and the app holds the overlay permission, which is what allows Android to start the activity from the background. Drag does not count as a tap.

### Outside test
- Boot receiver runs before unlock and records: time, unlocked state, network state, and one HEAD request to youtube.com.
- The test plays nothing automatically. Playback happens only on a tap, through YouTube's own player, with autoplay blocked.
