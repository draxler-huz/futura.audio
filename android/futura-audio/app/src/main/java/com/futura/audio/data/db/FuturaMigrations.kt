package com.futura.audio.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room migration registry (item 46).
 * Version 1: Phase 1 seed schema.
 * Version 2: Phase 2 scan/index columns, FTS4, play journal.
 */
object FuturaMigrations {

    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Expand tracks with nullable scan/index fields + play-completion accumulators.
            db.execSQL("ALTER TABLE tracks ADD COLUMN absolutePath TEXT")
            db.execSQL("ALTER TABLE tracks ADD COLUMN contentUri TEXT")
            db.execSQL("ALTER TABLE tracks ADD COLUMN mediaStoreId INTEGER")
            db.execSQL("ALTER TABLE tracks ADD COLUMN format TEXT")
            db.execSQL("ALTER TABLE tracks ADD COLUMN md5 TEXT")
            db.execSQL("ALTER TABLE tracks ADD COLUMN sha1 TEXT")
            db.execSQL("ALTER TABLE tracks ADD COLUMN volumeUuid TEXT")
            db.execSQL("ALTER TABLE tracks ADD COLUMN durationMs INTEGER")
            db.execSQL("ALTER TABLE tracks ADD COLUMN genre TEXT")
            db.execSQL("ALTER TABLE tracks ADD COLUMN normalizedGenre TEXT")
            db.execSQL("ALTER TABLE tracks ADD COLUMN lyrics TEXT")
            db.execSQL("ALTER TABLE tracks ADD COLUMN coverCacheKey TEXT")
            db.execSQL("ALTER TABLE tracks ADD COLUMN fileSizeBytes INTEGER")
            db.execSQL("ALTER TABLE tracks ADD COLUMN lastModifiedEpochMs INTEGER")
            db.execSQL("ALTER TABLE tracks ADD COLUMN completionRatioSum REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE tracks ADD COLUMN playEventCount INTEGER NOT NULL DEFAULT 0")

            db.execSQL("CREATE INDEX IF NOT EXISTS index_tracks_absolutePath ON tracks(absolutePath)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_tracks_mediaStoreId ON tracks(mediaStoreId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_tracks_favorite ON tracks(favorite)")

            // FTS4 virtual table (Room @Fts4 without contentEntity).
            db.execSQL(
                """
                CREATE VIRTUAL TABLE IF NOT EXISTS tracks_fts USING FTS4(
                    trackId TEXT,
                    title TEXT,
                    artistName TEXT,
                    albumTitle TEXT
                )
                """.trimIndent(),
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS play_journal (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    trackId TEXT NOT NULL,
                    playedAtEpochMs INTEGER NOT NULL,
                    completionRatio REAL NOT NULL,
                    FOREIGN KEY(trackId) REFERENCES tracks(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_play_journal_trackId ON play_journal(trackId)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_play_journal_playedAtEpochMs ON play_journal(playedAtEpochMs)",
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
