package dev.vespera.player.data

import dev.vespera.player.model.*
import kotlinx.coroutines.flow.StateFlow

interface MusicApi {
    val session: StateFlow<AccountSession>
    suspend fun search(keyword: String, page: Int = 1): List<Song>
    suspend fun searchCatalog(keyword: String, type: SearchType, page: Int = 1): SearchPage
    suspend fun hotSearch(): List<String>
    suspend fun searchSuggestions(keyword: String): List<String>
    suspend fun playlists(): List<Playlist>
    suspend fun playlistDetail(id: Long): PlaylistDetail
    suspend fun playlistTracks(id: Long): List<Song>
    suspend fun lyrics(songId: Long): LyricBundle
    suspend fun comments(songId: Long, page: Int = 1): List<Comment>
    suspend fun commentPage(songId: Long, page: Int = 1, sort: CommentSort = CommentSort.RECOMMENDED, cursor: Long? = null): CommentPage
    suspend fun hotComments(songId: Long, page: Int = 1): CommentPage
    suspend fun commentReplies(songId: Long, parentCommentId: Long, cursor: Long? = null): CommentPage
    suspend fun postComment(songId: Long, content: String, replyToCommentId: Long? = null): Boolean
    suspend fun hugComment(songId: Long, commentId: Long, targetUserId: Long): Boolean
    suspend fun streamUrl(songId: Long, quality: String = "exhigh"): String?
    suspend fun createLoginQr(): LoginQr
    suspend fun checkLoginQr(key: String): LoginResult
    suspend fun refreshAccount(): UserProfile?
    suspend fun sendPhoneCaptcha(phone: String, countryCode: String = "86"): Boolean
    suspend fun loginWithPhone(phone: String, captcha: String, countryCode: String = "86"): UserProfile?
    suspend fun logout()
    suspend fun dailySongs(): List<Song> = search("")
    suspend fun cloudSongs(): List<Song> = emptyList()
    suspend fun likedSongs(): List<Song> = emptyList()
    suspend fun likeSong(songId: Long, like: Boolean): Boolean = false
    suspend fun subscribePlaylist(playlistId: Long, subscribe: Boolean): Boolean = false
    suspend fun subscribedAlbums(): List<Album> = emptyList()
    suspend fun subscribeAlbum(albumId: Long, subscribe: Boolean): Boolean = false
    suspend fun subscribedArtists(): List<Artist> = emptyList()
    suspend fun subscribeArtist(artistId: Long, subscribe: Boolean): Boolean = false
    suspend fun subscribedVideos(): List<Video> = emptyList()
    suspend fun subscribeVideo(videoId: String, subscribe: Boolean): Boolean = false
    suspend fun subscribedRadios(): List<Radio> = emptyList()
    suspend fun subscribeRadio(radioId: Long, subscribe: Boolean): Boolean = false
    suspend fun history(): List<Song> = emptyList()
    suspend fun videos(songId: Long): List<Video> = emptyList()
    suspend fun radios(): List<Radio> = emptyList()
    suspend fun likeComment(songId: Long, commentId: Long, like: Boolean): Boolean = false
}
