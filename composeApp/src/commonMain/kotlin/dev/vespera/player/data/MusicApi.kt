package dev.vespera.player.data

import dev.vespera.player.model.*

interface MusicApi {
    suspend fun search(keyword: String, page: Int = 1): List<Song>
    suspend fun playlists(): List<Playlist>
    suspend fun playlistTracks(id: Long): List<Song>
    suspend fun lyric(songId: Long): String
    suspend fun comments(songId: Long, page: Int = 1): List<Comment>
    suspend fun streamUrl(songId: Long, quality: String = "exhigh"): String?
    suspend fun createLoginQr(): LoginQr
    suspend fun checkLoginQr(key: String): LoginResult
    suspend fun dailySongs(): List<Song> = search("")
    suspend fun cloudSongs(): List<Song> = emptyList()
    suspend fun history(): List<Song> = emptyList()
    suspend fun videos(songId: Long): List<Video> = emptyList()
    suspend fun radios(): List<Radio> = emptyList()
    suspend fun likeComment(songId: Long, commentId: Long, like: Boolean): Boolean = false
}

class DemoMusicApi : MusicApi {
    private val demo = listOf(Song(1, "夜に駆ける", listOf("YOASOBI"), "夜に駆ける", 261000), Song(2, "アイドル", listOf("YOASOBI"), "THE BOOK 3", 204000), Song(3, "群青", listOf("YOASOBI"), "THE BOOK", 250000))
    override suspend fun search(keyword: String, page: Int) = demo.filter { keyword.isBlank() || it.name.contains(keyword, true) || it.artists.any { a -> a.contains(keyword, true) } }
    override suspend fun playlists() = listOf(Playlist(100, "我喜欢的音乐", demo.size), Playlist(101, "最近播放", demo.size))
    override suspend fun playlistTracks(id: Long) = demo
    override suspend fun lyric(songId: Long) = "[00:00.00]Vespera 演示歌词\n[00:04.00]Apple Music 风格同步歌词"
    override suspend fun comments(songId: Long, page: Int) = listOf(Comment(1, "Vespera", null, "这首歌真不错！", 12, "刚刚"), Comment(2, "音乐爱好者", null, "歌词效果很漂亮。", 5, "1 小时前"))
    override suspend fun streamUrl(songId: Long, quality: String): String? = null
    override suspend fun createLoginQr() = LoginQr("demo", "")
    override suspend fun checkLoginQr(key: String) = LoginResult(LoginStatus.AUTHORIZED, "演示模式")
}
