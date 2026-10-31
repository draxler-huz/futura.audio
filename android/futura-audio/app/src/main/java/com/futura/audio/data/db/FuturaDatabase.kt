package com.futura.audio.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.futura.audio.data.db.dao.AlbumDao
import com.futura.audio.data.db.dao.ArtistDao
import com.futura.audio.data.db.dao.PlaylistDao
import com.futura.audio.data.db.dao.TrackDao
import com.futura.audio.data.db.entity.AlbumEntity
import com.futura.audio.data.db.entity.ArtistEntity
import com.futura.audio.data.db.entity.PlaylistEntity
import com.futura.audio.data.db.entity.PlaylistTrackEntity
import com.futura.audio.data.db.entity.TrackEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ArtistEntity::class,
        AlbumEntity::class,
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class FuturaDatabase : RoomDatabase() {
    abstract fun artistDao(): ArtistDao
    abstract fun albumDao(): AlbumDao
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        const val NAME = "futura_library.db"

        @Volatile
        private var instance: FuturaDatabase? = null

        fun getInstance(context: Context): FuturaDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { db ->
                    instance = db
                    seedIfEmpty(db)
                }
            }
        }

        private fun build(context: Context): FuturaDatabase {
            return Room.databaseBuilder(context, FuturaDatabase::class.java, NAME)
                .addMigrations(*FuturaMigrations.ALL)
                .build()
        }

        private fun seedIfEmpty(database: FuturaDatabase) {
            CoroutineScope(Dispatchers.IO).launch {
                if (database.artistDao().getAll().isNotEmpty()) return@launch
                database.artistDao().upsertAll(LibrarySeedData.artists)
                database.albumDao().upsertAll(LibrarySeedData.albums)
                database.trackDao().upsertAll(LibrarySeedData.tracks)
            }
        }
    }
}
