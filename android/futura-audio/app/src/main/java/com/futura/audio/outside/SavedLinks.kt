package com.futura.audio.outside

import android.content.SharedPreferences
import org.json.JSONArray

/**
 * Saved links kept in device-protected storage, so they survive a restart.
 * Only the link text is stored. Covers are YouTube's public thumbnails, loaded on demand.
 */
object SavedLinks {
    private const val KEY = "links"

    fun load(prefs: SharedPreferences): List<String> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            List(array.length()) { array.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(prefs: SharedPreferences, links: List<String>) {
        prefs.edit().putString(KEY, JSONArray(links).toString()).apply()
    }

    /** Video ID from youtu.be, watch, shorts, or embed links. Null if none is found. */
    fun videoId(url: String): String? {
        val match = Regex("""(?:youtu\.be/|[?&]v=|/shorts/|/embed/)([A-Za-z0-9_-]{11})""").find(url)
        return match?.groupValues?.get(1)
    }
}
