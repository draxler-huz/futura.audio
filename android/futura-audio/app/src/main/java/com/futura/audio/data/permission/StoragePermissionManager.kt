package com.futura.audio.data.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Storage permission helper (item 47).
 *
 * - API 33+: [Manifest.permission.READ_MEDIA_AUDIO]
 * - API 26–32: [Manifest.permission.READ_EXTERNAL_STORAGE]
 * - File scanner: All files access via [Environment.isExternalStorageManager]
 *   and [Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION]
 */
class StoragePermissionManager(private val context: Context) {

    fun mediaReadPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    fun hasMediaReadPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            mediaReadPermission(),
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            hasMediaReadPermission()
        }
    }

    /** True when MediaStore queries and the file scanner can both run. */
    fun hasLibraryAccess(): Boolean = hasMediaReadPermission() && hasAllFilesAccess()

    /**
     * Opens the system All files access screen for this app.
     * Caller should re-check [hasAllFilesAccess] when the activity resumes.
     */
    fun createManageAllFilesAccessIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }

    /** Fallback when the per-app manage-all-files intent is unavailable. */
    fun createManageAllFilesAccessFallbackIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        } else {
            createManageAllFilesAccessIntent()
        }
    }
}
