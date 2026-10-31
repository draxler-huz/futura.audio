# Phase 2 notes — Stage A

Branch: `phase-2-stage-a`  
Scope: storage and database foundation only. UI still uses Phase 1 `LibraryCatalog`; Room is not wired into Compose yet.

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
- `android/futura-audio/app/src/main/java/com/futura/audio/data/permission/StoragePermissionManager.kt`
- `android/futura-audio/app/src/main/java/com/futura/audio/data/mediastore/MediaStoreAudioQuery.kt`
- `android/futura-audio/app/schemas/` (placeholder; JSON after first successful build)
- `docs/PHASE_2_NOTES.md`

## Assumptions

- Playlist entity is for user playlists; design smart chips (`Never Played Excavator`, etc.) stay filter logic, not Room rows.
- Playlist fields: `id`, `name`, `createdAtEpochMs`; join: `playlistId`, `trackId`, `position`.
- Artist `family` stored as string (`Electronic` | `Chill` | `Indie` | `Soul`).
- MediaStore results stay as DTOs until a later indexer stage.
- Phase 1 alarm packages unchanged; `LibraryCatalog` left in place.

## Approximations

- Relative “days ago” fields in seed stay as design numbers, not derived from wall clock.
- On API &lt; 30, “All files access” is treated as granted when media read permission is granted.
- Unique index on `(playlistId, position)` — renumber on reorder in a later stage if needed.

## Open questions

1. **Never Played Excavator** — kept as written; probable typo (Excavator?). Owner decide before UI port.
2. Rest of truncated Stage A item 26+ — confirm nothing else was required.
3. Should Track grow MediaStore id / content URI / absolute path columns in Stage B?
4. Keep design seed after real library scan, or replace?

## Device tests

1. Fresh install: DB creates; 10 artists, 12 albums, 36 tracks with design ids.
2. Kill/reopen: seed does not duplicate.
3. Set a `valenceOverride` (via temporary debug call or Database Inspector); confirm it survives a simulated `updateIndexedFields`.
4. API 33+: grant/deny `READ_MEDIA_AUDIO`; `hasMediaReadPermission()` matches.
5. Open All files access intent; after return, `hasAllFilesAccess()` matches system state.
6. With permissions: `queryAllAudio()` returns device music without crashing.
7. Phase 1: alarm schedule, camouflage, accessibility service still work.
8. Confirm exported schema JSON under `app/schemas/com.futura.audio.data.db.FuturaDatabase/1.json`.
