package com.futura.audio.data.scan

import android.content.Context
import android.os.Build
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Maps absolute paths to [StorageManager] volume UUIDs (primary → "primary" when null).
 */
class StorageVolumeMapper(context: Context) {
    private val appContext = context.applicationContext
    private val storageManager =
        appContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager

    suspend fun volumeUuidFor(file: File): String? = withContext(Dispatchers.IO) {
        volumeUuidForBlocking(file)
    }

    fun volumeUuidForBlocking(file: File): String? {
        val volume: StorageVolume? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                storageManager.getStorageVolume(file)
            } else {
                null
            }
        if (volume != null) {
            val uuid = volume.uuid
            return uuid ?: PRIMARY_UUID
        }
        // Fallback: match path prefixes of known volumes.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = file.absolutePath
            for (v in storageManager.storageVolumes) {
                val dir = volumeDirectory(v) ?: continue
                if (path.startsWith(dir.absolutePath)) {
                    return v.uuid ?: PRIMARY_UUID
                }
            }
        }
        return null
    }

    fun listMusicRoots(): List<File> {
        val roots = mutableListOf<File>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            for (volume in storageManager.storageVolumes) {
                val dir = volumeDirectory(volume) ?: continue
                val music = File(dir, "Music")
                if (music.isDirectory) roots += music
                // Also include the volume root so RecursiveFileScanner can walk other audio trees.
                roots += dir
            }
        } else {
            @Suppress("DEPRECATION")
            val ext = android.os.Environment.getExternalStorageDirectory()
            roots += File(ext, "Music")
            roots += ext
        }
        return roots.distinctBy { it.absolutePath }
    }

    private fun volumeDirectory(volume: StorageVolume): File? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            volume.directory
        } else {
            try {
                @Suppress("DEPRECATION")
                val method = StorageVolume::class.java.getMethod("getPathFile")
                method.invoke(volume) as? File
            } catch (_: Exception) {
                try {
                    @Suppress("DEPRECATION")
                    val method = StorageVolume::class.java.getMethod("getPath")
                    val path = method.invoke(volume) as? String
                    path?.let { File(it) }
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    companion object {
        const val PRIMARY_UUID = "primary"
    }
}
