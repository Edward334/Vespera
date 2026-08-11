package dev.vespera.player.model

import kotlinx.serialization.Serializable

@Serializable data class Song(val id: Long, val name: String, val artists: List<String>, val album: String = "", val durationMs: Long = 0, val coverUrl: String? = null, val streamUrl: String? = null, val mvId: Long = 0)
@Serializable data class Playlist(val id: Long, val name: String, val trackCount: Int, val coverUrl: String? = null, val isCloud: Boolean = false)
@Serializable data class Comment(val id: Long, val user: String, val avatarUrl: String?, val content: String, val likedCount: Int, val timeLabel: String)
@Serializable data class Video(val id: Long, val title: String, val url: String?, val durationMs: Long = 0, val coverUrl: String? = null)
@Serializable data class Radio(val id: Long, val name: String, val description: String = "", val coverUrl: String? = null)
data class LyricWord(val text: String, val startMs: Long, val endMs: Long)
data class LyricLine(val startMs: Long, val words: List<LyricWord>, val translation: String? = null, val romanization: String? = null)
data class PlayerState(val current: Song? = null, val positionMs: Long = 0, val playing: Boolean = false, val queue: List<Song> = emptyList(), val repeat: RepeatMode = RepeatMode.ALL, val volume: Float = 1f)
enum class RepeatMode { OFF, ALL, ONE }
data class LoginQr(val key: String, val imageData: String)
data class LoginResult(val status: LoginStatus, val message: String, val cookie: String? = null)
enum class LoginStatus { WAITING, SCANNED, AUTHORIZED, EXPIRED, ERROR }
enum class AppDestination(val label: String) { HOME("首页"), SEARCH("搜索"), LIBRARY("音乐库"), COMMENTS("评论"), SETTINGS("设置") }
