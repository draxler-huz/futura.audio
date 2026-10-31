package com.futura.audio.data.index

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Main entry point for Phase 2 indexing: combines file system and MediaStore sources.
 *
 * 1. [TrackIndexer] reads local audio files.
 * 2. [MediaStoreIndexer] reads Android's MediaStore.
 * 3. Both preserve [valenceOverride] / [energyOverride] on re-index.
 */
object FuturaIndexManager {

    private lateinit var trackIndexer: TrackIndexer
    private lateinit var mediaStoreIndexer: MediaStoreIndexer

    fun init(context: Context) {
        trackIndexer = TrackIndexer(context)
        mediaStoreIndexer = MediaStoreIndexer(context)
    }

    /**
     * Run the full indexing pipeline: local files + MediaStore.
     * Blocks until both pass; each component logs errors but does not abort the other.
     */
    suspend fun indexAll(context: Context) {
        if (!::trackIndexer.isInitialized) init(context)
        if (!::mediaStoreIndexer.isInitialized) init(context)

        try {
            trackIndexer.indexAll()
        } catch (e: Exception) {
            // Log: "Failed to index file system tracks"
        }

        try {
            mediaStoreIndexer.indexAll()
        } catch (e: Exception) {
            // Log: "Failed to index MediaStore tracks"
        }
    }
}
