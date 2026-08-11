package dev.vespera.player.data

import dev.vespera.player.model.Song
import kotlinx.coroutines.flow.*

class HistoryRepository(private val limit: Int = 100) {
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()
    fun record(song: Song) { _songs.update { (listOf(song) + it.filterNot { old -> old.id == song.id }).take(limit) } }
    fun clear() { _songs.value = emptyList() }
}
