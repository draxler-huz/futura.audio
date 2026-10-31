package com.futura.audio

import android.app.Application
import com.futura.audio.data.db.FuturaDatabase
import com.futura.audio.data.mediastore.MediaStoreAudioQuery
import com.futura.audio.data.permission.StoragePermissionManager

class FuturaApplication : Application() {
    lateinit var database: FuturaDatabase
        private set

    lateinit var storagePermissionManager: StoragePermissionManager
        private set

    lateinit var mediaStoreAudioQuery: MediaStoreAudioQuery
        private set

    override fun onCreate() {
        super.onCreate()
        database = FuturaDatabase.getInstance(this)
        storagePermissionManager = StoragePermissionManager(this)
        mediaStoreAudioQuery = MediaStoreAudioQuery(contentResolver)
    }
}
