package dev.vespera.player.download

import java.nio.file.Files
import java.nio.file.Paths

actual object PlatformFileStore {
    actual fun save(fileName: String, bytes: ByteArray): String {
        val directory = Paths.get(System.getProperty("user.home"), "Music", "Vespera")
        Files.createDirectories(directory)
        return Files.write(directory.resolve(fileName), bytes).toAbsolutePath().toString()
    }
}
