package dev.vespera.player.model

import kotlinx.serialization.Serializable

@Serializable data class Song(val id: Long, val name: String, val artists: List<String>, val album: String = "", val durationMs: Long = 0, val coverUrl: String? = null, val streamUrl: String? = null, val mvId: Long = 0, val artistIds: List<Long> = emptyList(), val albumId: Long = 0, val aliases: List<String> = emptyList(), val quality: String? = null, val fee: Int = 0)
@Serializable data class Playlist(val id: Long, val name: String, val trackCount: Int, val coverUrl: String? = null, val isCloud: Boolean = false, val description: String = "", val creator: String = "", val subscribed: Boolean = false)
@Serializable data class Comment(val id: Long, val user: String, val avatarUrl: String?, val content: String, val likedCount: Int, val timeLabel: String, val liked: Boolean = false, val replyContent: String? = null)
@Serializable data class Video(val id: Long, val title: String, val url: String?, val durationMs: Long = 0, val coverUrl: String? = null)
@Serializable data class Radio(val id: Long, val name: String, val description: String = "", val coverUrl: String? = null)
@Serializable data class UserProfile(val id: Long, val nickname: String, val avatarUrl: String? = null, val vipType: Int = 0)
data class AccountSession(val profile: UserProfile? = null, val loading: Boolean = false, val error: String? = null) { val loggedIn: Boolean get() = profile != null }
data class LyricBundle(val line: String = "", val word: String = "", val translated: String = "", val romanized: String = "", val ttml: String? = null)
data class LyricWord(val text: String, val startMs: Long, val endMs: Long)
data class LyricLine(val startMs: Long, val endMs: Long, val words: List<LyricWord>, val translation: String? = null, val romanization: String? = null, val isBackground: Boolean = false, val isDuet: Boolean = false)
data class PlayerState(val current: Song? = null, val positionMs: Long = 0, val playing: Boolean = false, val queue: List<Song> = emptyList(), val repeat: RepeatMode = RepeatMode.ALL, val volume: Float = 1f)
enum class RepeatMode { OFF, ALL, ONE }
data class LoginQr(val key: String, val payload: String)
data class LoginResult(val status: LoginStatus, val message: String, val cookie: String? = null)
enum class LoginStatus { WAITING, SCANNED, AUTHORIZED, EXPIRED, ERROR }
enum class AppDestination(val label: String) { HOME("首页"), SEARCH("搜索"), LIBRARY("音乐库"), COMMENTS("评论"), SETTINGS("设置") }
