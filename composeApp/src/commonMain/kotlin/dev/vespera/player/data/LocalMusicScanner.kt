package dev.vespera.player.data

import dev.vespera.player.model.Song

expect object LocalMusicScanner {
    fun scan(): List<Song>
}
