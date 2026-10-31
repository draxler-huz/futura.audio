package com.futura.audio.data.metadata

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

/**
 * Loads `assets/genre_taxonomy.json` and maps genre variants → canonical family.
 * Families: Electronic | Chill | Indie | Soul.
 *
 * **Fallback:** unmapped / blank genres resolve to [FALLBACK_FAMILY] (`Chill`),
 * as documented in the taxonomy JSON `notes` / `fallbackFamily` fields.
 */
class GenreNormalizer(context: Context) {
    private val appContext = context.applicationContext
    private val cache = AtomicReference<Taxonomy?>(null)

    data class Taxonomy(
        val fallbackFamily: String,
        val families: Set<String>,
        val mappings: Map<String, String>,
    )

    data class Normalized(
        /** Original tag text (trimmed), or null if blank. */
        val genre: String?,
        /** Canonical family: Electronic | Chill | Indie | Soul. */
        val family: String,
        val wasMapped: Boolean,
    )

    suspend fun ensureLoaded(): Taxonomy = withContext(Dispatchers.IO) {
        cache.get() ?: loadBlocking().also { cache.set(it) }
    }

    suspend fun normalize(raw: String?): Normalized = withContext(Dispatchers.IO) {
        normalizeBlocking(raw)
    }

    fun normalizeBlocking(raw: String?): Normalized {
        val taxonomy = cache.get() ?: loadBlocking().also { cache.set(it) }
        val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() }
        if (trimmed == null) {
            return Normalized(genre = null, family = taxonomy.fallbackFamily, wasMapped = false)
        }
        val key = trimmed.lowercase(Locale.US)
        val family = taxonomy.mappings[key]
        return if (family != null) {
            Normalized(genre = trimmed, family = family, wasMapped = true)
        } else {
            // Unknown → Chill fallback (documented in assets/genre_taxonomy.json).
            Normalized(genre = trimmed, family = taxonomy.fallbackFamily, wasMapped = false)
        }
    }

    private fun loadBlocking(): Taxonomy {
        val json = appContext.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val fallback = root.optString("fallbackFamily", FALLBACK_FAMILY).ifBlank { FALLBACK_FAMILY }
        val familiesJson = root.optJSONArray("families")
        val families = linkedSetOf<String>()
        if (familiesJson != null) {
            for (i in 0 until familiesJson.length()) {
                families += familiesJson.getString(i)
            }
        } else {
            families += DEFAULT_FAMILIES
        }
        val mappingsObj = root.getJSONObject("mappings")
        val mappings = HashMap<String, String>(mappingsObj.length())
        val keys = mappingsObj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            mappings[k.lowercase(Locale.US)] = mappingsObj.getString(k)
        }
        return Taxonomy(fallbackFamily = fallback, families = families, mappings = mappings)
    }

    companion object {
        const val ASSET_NAME = "genre_taxonomy.json"
        /** Documented fallback family for unknown genres. */
        const val FALLBACK_FAMILY = "Chill"
        private val DEFAULT_FAMILIES = listOf("Electronic", "Chill", "Indie", "Soul")
    }
}
