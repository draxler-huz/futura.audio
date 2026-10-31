# Phase 2 Implementation Plan

## Overview
This document outlines the implementation strategy for Phase 2 of Futura Audio (package com.futura.audio). The goal is to complete all Stage A through Stage E requirements as specified.

## Current Status Assessment

### Stage A: Storage Foundation
✅ **COMPLETED**
- 27: Recursive file scanner (RecursiveFileScanner.kt)
- 48: Format resolver (FormatResolver.kt)
- 39: Duration reader (DurationReader.kt)
- 37: Corrupt file detector (CorruptFileDetector.kt)
- 32: Track hasher (TrackHasher.kt)
- 40: Storage volume mapper (StorageVolumeMapper.kt)
- **❌ MISSING: Write scanned tracks to Room**

### Stage B: Metadata and Artwork
✅ **COMPLETED**
- 29: Metadata extractor (MetadataExtractor.kt)
- 30: Embedded artwork reader (EmbeddedArtworkReader.kt)
- 31: Cover art cache (CoverArtCache.kt)
- 38: Genre normalizer (GenreNormalizer.kt)
- 44: Cue sheet parser (CueSheetParser.kt)
- 45: Lyrics extractor (LyricsExtractor.kt)
- 42: Mood analyzer (MoodAnalyzer.kt) - Includes energy/valence calculation

### Stage C: Library Features
❌ **NOT STARTED**
- 33: Playlist DAO (transactional add, remove, reorder)
- 34: Smart folder engine (directory tree)
- 35: Incremental sync with FileObserver, debounced
- 41: Recently played journal (circular, capped)
- 42: Most played calculator (completion ratio)
- 43: Search with FTS4 over track, artist, and album names
- 49: Orphan cleaner (WorkManager or start-up job)
- 50: M3U and M3U8 import and export (absolute and relative paths)
- Favorites: track.favorite lives in tracks table

### Stage D: Tag Writing
❌ **NOT STARTED**
- 36: Batch metadata writer with jaudiotagger

### Stage E: Front End
❌ **NOT STARTED**
- 1-15: Complete UI port (six tabs, smart filters, node map, mood map, folder tree, etc.)

## Missing Implementation Focus

### 1. Stage A: Writing Scanned Tracks to Room

**Requirement**: Write scanned tracks to Room, keeping valenceOverride and energyOverride on re-index.

**Implementation Needed**:
- Create a TrackIndexer class in `data/index` or similar
- Integrate with existing scanners (RecursiveFileScanner, FormatResolver, etc.)
- Process MediaStore results and scanned files
- Write to Room database using TrackDao
- Preserve overrides during re-indexing

### 2. Stage C: Library Features

Need to implement:
- Playlist DAO with transactions
- Smart folder engine
- Incremental sync with FileObserver
- Recently played journal
- Most played calculator
- FTS4 search
- Orphan cleaner
- M3U/M3U8 import/export
- Favorites smart filter

### 3. Stage D: Tag Writing

Need to implement:
- Batch metadata writer with jaudiotagger
- Backup original files
- Write metadata (title, artist, genre)
- Verify writes
- Restore on failure

### 4. Stage E: Front End

Need to implement:
- Complete UI matching design specifications
- All six tabs with their content
- Smart filter cards
- Search functionality
- Node map and mood map
- Folder tree
- All interactions and behaviors

## Implementation Strategy

### Priority Order:
1. **TrackIndexer** - Write scanned tracks to Room (critical dependency)
2. **Playlist DAO** - Database foundation for Stage C
3. **Smart folder engine** - Core Stage C feature
4. **Incremental sync** - Data consistency
5. **Recently played journal** - Analytics/usage tracking
6. **Most played calculator** - User insights
7. **FTS4 search** - Core discovery feature
8. **Metadata writer** - Stage D core functionality
9. **Orphan cleaner** - Maintenance utility
10. **Import/export** - Data portability
11. **Front end** - User interface

## Technical Considerations

### Architecture:
- Maintain existing Room schema (v2)
- Use existing database migrations
- Follow existing coding patterns
- Adhere to Android best practices

### Dependencies:
- kotlin.code.style=official (enforced)
- AndroidX Compose
- Room 2.7.2 with KSP
- jaudiotagger for tag reading/writing
- Oboe for audio playback

### Performance:
- Use coroutines and flows
- Implement proper caching
- Consider memory usage for large libraries
- Ensure efficient database queries

## Next Steps

1. **Create TrackIndexer service**
   - Integrate all Stage A scanners
   - Write to Room database
   - Handle overrides preservation

2. **Implement Playlist DAO**
   - Transactional operations
   - CRUD operations
   - Smart filter integration

3. **Develop Smart folder engine**
   - Directory tree structure
   - Color coding for folders
   - Collapsible UI integration

4. **Build incremental sync**
   - FileObserver integration
   - Debouncing implementation
   - Cross-platform support

## Verification

After implementation, verify:
- All numbered items (26-50) are implemented
- Design elements match specifications
- Favorite toggle works and saves to Room
- Valence/energy overrides survive re-indexing
- Phase 1 alarm code remains unchanged
- Build passes gradlew.bat :app:assembleDebug
- docs/PHASE_2_NOTES.md is updated

## Open Questions

1. **Build Process**: How to handle gradle build issues?
2. **Testing**: What testing framework should be used?
3. **Dependencies**: Ensure jaudiotagger is available
4. **Permissions**: All files access vs READ_MEDIA_AUDIO
5. **Storage**: How to handle large music libraries?

## Timeline

This is a significant implementation effort requiring careful, methodical progress. Each Stage should be completed incrementally with verification at each step.
