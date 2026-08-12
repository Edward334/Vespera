package dev.vespera.player.data

import dev.vespera.player.model.*
import kotlinx.coroutines.flow.StateFlow

interface MusicApi {
    val session: StateFlow<AccountSession>
    suspend fun search(keyword: String, page: Int = 1): List<Song>
    suspend fun playlists(): List<Playlist>
    suspend fun playlistTracks(id: Long): List<Song>
    suspend fun lyrics(songId: Long): LyricBundle
    suspend fun comments(songId: Long, page: Int = 1): List<Comment>
    suspend fun streamUrl(songId: Long, quality: String = "exhigh"): String?
    suspend fun createLoginQr(): LoginQr
    suspend fun checkLoginQr(key: String): LoginResult
    suspend fun refreshAccount(): UserProfile?
    suspend fun sendPhoneCaptcha(phone: String, countryCode: String = "86"): Boolean
    suspend fun loginWithPhone(phone: String, captcha: String, countryCode: String = "86"): UserProfile?
    suspend fun logout()
    suspend fun dailySongs(): List<Song> = search("")
    suspend fun cloudSongs(): List<Song> = emptyList()
    suspend fun history(): List<Song> = emptyList()
    suspend fun videos(songId: Long): List<Video> = emptyList()
    suspend fun radios(): List<Radio> = emptyList()
    suspend fun likeComment(songId: Long, commentId: Long, like: Boolean): Boolean = false
}
