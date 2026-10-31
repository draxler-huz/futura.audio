package com.futura.audio.data.scan

import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Recursively walks Music directories (and optional extra roots) for audio extensions.
 */
class RecursiveFileScanner(
    private val volumeMapper: StorageVolumeMapper,
) {

    data class ScannedFile(
        val file: File,
        val format: FormatResolver.AudioFormat,
    )

    /**
     * @param extraRoots additional directories to include (e.g. app-specific folders).
     * @param musicOnly when true, only walks `*/Music` trees under each volume; otherwise
     *   walks volume roots but still filters to audio extensions.
     */
    suspend fun scan(
        extraRoots: List<File> = emptyList(),
        musicOnly: Boolean = true,
    ): List<ScannedFile> = withContext(Dispatchers.IO) {
        val roots = linkedSetOf<File>()
        if (musicOnly) {
            for (volumeRoot in volumeMapper.listMusicRoots()) {
                val music = if (volumeRoot.name.equals("Music", ignoreCase = true)) {
                    volumeRoot
                } else {
                    File(volumeRoot, "Music")
                }
                if (music.isDirectory) roots += music
            }
            @Suppress("DEPRECATION")
            val legacyMusic = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            if (legacyMusic.isDirectory) roots += legacyMusic
        } else {
            roots += volumeMapper.listMusicRoots()
        }
        roots += extraRoots.filter { it.isDirectory }

        val results = ArrayList<ScannedFile>(256)
        val seen = HashSet<String>()
        for (root in roots) {
            walk(root, results, seen)
        }
        results
    }

    private fun walk(
        dir: File,
        out: MutableList<ScannedFile>,
        seen: MutableSet<String>,
    ) {
        val children = dir.listFiles() ?: return
        for (child in children) {
            if (child.isDirectory) {
                // Skip hidden / Android system dirs to keep scans bounded.
                if (child.name.startsWith(".")) continue
                if (child.name.equals("Android", ignoreCase = true) &&
                    dir.absolutePath.equals(
                        child.parentFile?.absolutePath,
                        ignoreCase = true,
                    )
                ) {
                    // Still allow Music under Android? Skip top-level Android/ data trees.
                    continue
                }
                walk(child, out, seen)
            } else if (child.isFile) {
                val ext = child.extension
                if (!FormatResolver.isSupportedAudioExtension(ext)) continue
                if (CorruptFileDetector.isCorrupt(child)) continue
                val path = child.absolutePath
                if (!seen.add(path)) continue
                out += ScannedFile(child, FormatResolver.resolve(child))
            }
        }
    }
}
