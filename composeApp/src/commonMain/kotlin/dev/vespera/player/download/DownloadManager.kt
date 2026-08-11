package dev.vespera.player.download

import dev.vespera.player.data.MusicApi
import dev.vespera.player.model.Song
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class DownloadManager(private val api: MusicApi, private val client: HttpClient = HttpClient()) {
    suspend fun download(song: Song, quality: String = "exhigh"): String {
        val url = requireNotNull(api.streamUrl(song.id, quality)) { "当前歌曲没有可用的下载地址" }
        val bytes = client.get(url).body<ByteArray>()
        return PlatformFileStore.save(safeFileName(song), bytes)
    }

    private fun safeFileName(song: Song): String {
        val raw = "${song.artists.joinToString(" & ")} - ${song.name}.mp3"
        return raw.map { if (it in "\\/:*?\"<>|") '_' else it }.joinToString("")
    }
}

expect object PlatformFileStore {
    fun save(fileName: String, bytes: ByteArray): String
}
