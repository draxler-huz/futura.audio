package com.futura.audio.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futura.audio.ui.music.Deck
import com.futura.audio.ui.music.formatTime
import kotlinx.coroutines.launch

/**
 * Library matrix: a browsing layer over the same catalog the view model plays from. A tap calls
 * [onPlay] with the track's playback index, and the caller selects it and returns to the deck.
 *
 * Simplified port of the design 2 library: songs, albums, artists, folders, and moods, with search,
 * smart filters, and an alphabet strip. The node map and 2D mood map are not ported.
 */
private enum class LibraryTab(val label: String) {
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    FOLDERS("Folders"),
    MOODS("Moods"),
}

private enum class SmartFilter(val label: String) {
    ALL("All"),
    NEVER_PLAYED("Never played"),
    MOST_PLAYED("Most played"),
    RECENTLY_ADDED("Recently added"),
    RECENTLY_PLAYED("Recently played"),
    FAVORITES("Favorites"),
}

private val MOOD_ORDER = listOf("Euphoric", "Intense", "Serene", "Melancholy")
private val ALPHABET = listOf("#") + ('A'..'Z').map { it.toString() }

/** Mood from energy and valence, as in the design 2 prototype. */
fun moodOf(track: LibraryTrack): String = when {
    track.energy >= 0.5f && track.valence >= 0.5f -> "Euphoric"
    track.energy >= 0.5f -> "Intense"
    track.valence >= 0.5f -> "Serene"
    else -> "Melancholy"
}

private fun applySmartFilter(list: List<LibraryTrack>, filter: SmartFilter): List<LibraryTrack> = when (filter) {
    SmartFilter.ALL -> list
    SmartFilter.NEVER_PLAYED -> list.filter { it.plays == 0 }.sortedByDescending { it.addedDaysAgo }
    SmartFilter.MOST_PLAYED -> list.filter { it.plays > 0 }.sortedByDescending { it.plays }
    SmartFilter.RECENTLY_ADDED -> list.filter { it.addedDaysAgo <= 14 }.sortedBy { it.addedDaysAgo }
    SmartFilter.RECENTLY_PLAYED -> list.filter { it.lastPlayedDaysAgo != null }
        .sortedBy { it.lastPlayedDaysAgo ?: 0 }
    SmartFilter.FAVORITES -> list.filter { it.favorite }
}

private fun matchesQuery(track: LibraryTrack, query: String): Boolean =
    query.isBlank() || listOf(track.title, track.artist, track.album).any { it.contains(query, ignoreCase = true) }

private fun letterOf(title: String): String {
    val c = title.firstOrNull()?.uppercaseChar()
    return if (c != null && c in 'A'..'Z') c.toString() else "#"
}

private sealed interface ListRow {
    data class Header(val title: String, val subtitle: String?) : ListRow
    data class Song(val track: LibraryTrack, val playIndex: Int) : ListRow
}

private fun buildRows(tab: LibraryTab, tracks: List<LibraryTrack>, playIndexOf: (LibraryTrack) -> Int): List<ListRow> {
    val rows = mutableListOf<ListRow>()
    fun section(title: String, subtitle: String?, items: List<LibraryTrack>) {
        rows += ListRow.Header(title, subtitle)
        items.forEach { rows += ListRow.Song(it, playIndexOf(it)) }
    }
    when (tab) {
        LibraryTab.SONGS -> tracks.sortedBy { it.title.lowercase() }
            .groupBy { letterOf(it.title) }
            .toSortedMap()
            .forEach { (letter, items) -> section(letter, null, items) }
        LibraryTab.ALBUMS -> tracks.groupBy { it.album }.toSortedMap()
            .forEach { (album, items) -> section(album, items.first().artist, items) }
        LibraryTab.ARTISTS -> tracks.groupBy { it.artist }.toSortedMap()
            .forEach { (artist, items) -> section(artist, items.first().genre, items) }
        LibraryTab.FOLDERS -> tracks.groupBy { it.folder }.toSortedMap()
            .forEach { (folder, items) -> section(folder, null, items) }
        LibraryTab.MOODS -> MOOD_ORDER.forEach { mood ->
            val items = tracks.filter { moodOf(it) == mood }
            if (items.isNotEmpty()) section(mood, null, items)
        }
    }
    return rows
}

@Composable
fun LibraryScreen(
    currentIndex: Int,
    onPlay: (Int) -> Unit,
    onClose: () -> Unit,
) {
    val catalog = remember { LibraryCatalog.tracks }
    val playIndexById = remember { catalog.withIndex().associate { it.value.id to it.index } }
    var tab by remember { mutableStateOf(LibraryTab.SONGS) }
    var filter by remember { mutableStateOf(SmartFilter.ALL) }
    var query by remember { mutableStateOf("") }

    val visible = remember(query, filter) {
        applySmartFilter(catalog.filter { matchesQuery(it, query) }, filter)
    }
    val rows = remember(tab, visible) {
        buildRows(tab, visible) { playIndexById.getValue(it.id) }
    }
    val headerIndexByTitle = remember(rows) {
        rows.withIndex()
            .filter { it.value is ListRow.Header }
            .associate { (it.value as ListRow.Header).title to it.index }
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Deck.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, Deck.Border, RoundedCornerShape(50))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back to deck", tint = Deck.Muted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(text = "DECK", color = Deck.Muted, fontFamily = Deck.Mono, fontSize = 11.sp, letterSpacing = 1.sp)
            }
            Spacer(Modifier.weight(1f))
            Text(text = "LIBRARY MATRIX", color = Deck.Primary, fontFamily = Deck.Mono, fontSize = 11.sp, letterSpacing = 2.sp)
            Spacer(Modifier.weight(1f))
            Text(text = "${visible.size}", color = Deck.Muted, fontFamily = Deck.Mono, fontSize = 11.sp)
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text(text = "Search title, artist, album", color = Deck.Muted, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Deck.Muted) },
            modifier = Modifier.fillMaxWidth(),
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            items(SmartFilter.values().toList()) { option ->
                val selected = option == filter
                Text(
                    text = option.label,
                    color = if (selected) Deck.Primary else Deck.Muted,
                    fontFamily = Deck.Mono,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, if (selected) Deck.Primary.copy(alpha = 0.4f) else Deck.Border, RoundedCornerShape(50))
                        .background(if (selected) Deck.Primary.copy(alpha = 0.08f) else Color.Transparent)
                        .clickable { filter = option }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            items(LibraryTab.values().toList()) { option ->
                val selected = option == tab
                Text(
                    text = option.label.uppercase(),
                    color = if (selected) Deck.Primary else Deck.Muted,
                    fontFamily = Deck.Mono,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.clickable { tab = option }.padding(vertical = 4.dp),
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (rows.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No tracks match.", color = Deck.Muted, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(rows) { row ->
                        when (row) {
                            is ListRow.Header -> HeaderRow(row)
                            is ListRow.Song -> SongRow(
                                row = row,
                                playing = row.playIndex == currentIndex,
                                onClick = { onPlay(row.playIndex) },
                            )
                        }
                    }
                }
            }

            if (tab == LibraryTab.SONGS) {
                AlphabetStrip(
                    available = headerIndexByTitle.keys,
                    onLetter = { letter ->
                        headerIndexByTitle[letter]?.let { index ->
                            scope.launch { listState.animateScrollToItem(index) }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun HeaderRow(header: ListRow.Header) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp)) {
        Text(
            text = header.title.uppercase(),
            color = Deck.Signal,
            fontFamily = Deck.Mono,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            maxLines = 1,
        )
        header.subtitle?.let {
            Text(text = it, color = Deck.Muted, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
private fun SongRow(row: ListRow.Song, playing: Boolean, onClick: () -> Unit) {
    val track = row.track
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (playing) Deck.Accent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (playing) Deck.Primary else Deck.Foreground,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
            )
            Text(text = "${track.artist} • ${track.album}", color = Deck.Muted, fontSize = 12.sp, maxLines = 1)
        }
        if (track.favorite) {
            Text(text = "★", color = Deck.Signal, fontSize = 12.sp)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = formatTime(track.durationSeconds.toFloat()),
            color = Deck.Muted,
            fontFamily = Deck.Mono,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun AlphabetStrip(available: Set<String>, onLetter: (String) -> Unit) {
    Column(
        modifier = Modifier
            .padding(start = 6.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        ALPHABET.forEach { letter ->
            val enabled = letter in available
            Text(
                text = letter,
                color = if (enabled) Deck.Primary else Deck.Muted.copy(alpha = 0.35f),
                fontFamily = Deck.Mono,
                fontSize = 9.sp,
                modifier = Modifier
                    .clickable(enabled = enabled) { onLetter(letter) }
                    .padding(vertical = 1.dp),
            )
        }
    }
}
