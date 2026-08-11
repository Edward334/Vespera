package dev.vespera.player.data

import dev.vespera.player.model.Song
import platform.Foundation.*

actual object LocalMusicScanner {
    actual fun scan(): List<Song> {
        val root = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String
        val files = NSFileManager.defaultManager.contentsOfDirectoryAtPath(root, null).orEmpty()
        return files.filterIsInstance<String>().filter { it.substringAfterLast('.', "").lowercase() in setOf("mp3", "m4a", "wav", "flac") }.map { name -> Song(name.hashCode().toLong(), name.substringBeforeLast('.'), listOf("本地音乐"), streamUrl = NSURL.fileURLWithPath("$root/$name").absoluteString) }
    }
}
