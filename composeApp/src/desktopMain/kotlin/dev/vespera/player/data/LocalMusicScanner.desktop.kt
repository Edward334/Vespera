package dev.vespera.player.data

import dev.vespera.player.model.Song
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

actual object LocalMusicScanner {
    actual fun scan(): List<Song> {
        val root = Paths.get(System.getProperty("user.home"), "Music")
        if (!Files.exists(root)) return emptyList()
        return Files.walk(root, 4).use { paths -> paths.filter { Files.isRegularFile(it) }.filter { it.fileName.toString().substringAfterLast('.', "").lowercase() in setOf("mp3", "m4a", "flac", "wav", "ogg") }.map { file -> Song(file.toAbsolutePath().hashCode().toLong(), file.fileName.toString().substringBeforeLast('.'), listOf("本地音乐"), streamUrl = file.toUri().toString()) }.toList() }
    }
}
