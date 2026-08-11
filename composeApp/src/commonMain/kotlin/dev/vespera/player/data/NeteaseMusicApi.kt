package dev.vespera.player.data

import dev.vespera.player.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

class NeteaseMusicApi(
    private val baseUrl: String,
    private val userId: Long? = null,
    private val cookie: String? = null,
    private val client: HttpClient = HttpClient { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } },
) : MusicApi {
    override suspend fun search(keyword: String, page: Int): List<Song> = request("cloudsearch", mapOf("keywords" to keyword, "limit" to "30", "offset" to ((page - 1) * 30).toString())).obj("result").array("songs").map(::song)

    override suspend fun playlists(): List<Playlist> {
        val uid = requireNotNull(userId) { "请先在设置中填写网易云用户 ID" }
        return request("user/playlist", mapOf("uid" to uid.toString())).array("playlist").map { value ->
            val item = value.jsonObject
            Playlist(item.long("id"), item.string("name"), item.int("trackCount"), item.optionalString("coverImgUrl"))
        }
    }

    override suspend fun playlistTracks(id: Long): List<Song> = request("playlist/track/all", mapOf("id" to id.toString(), "limit" to "1000")).array("songs").map(::song)

    override suspend fun lyric(songId: Long): String {
        val root = request("lyric/new", mapOf("id" to songId.toString()))
        return root.objOrNull("lrc")?.optionalString("lyric") ?: root.objOrNull("yrc")?.optionalString("lyric").orEmpty()
    }

    override suspend fun comments(songId: Long, page: Int): List<Comment> = request("comment/music", mapOf("id" to songId.toString(), "limit" to "20", "offset" to ((page - 1) * 20).toString())).array("comments").map { value ->
        val item = value.jsonObject; val user = item.obj("user")
        Comment(item.long("commentId"), user.string("nickname"), user.optionalString("avatarUrl"), item.string("content"), item.int("likedCount"), item.optionalString("timeStr").orEmpty())
    }

    override suspend fun streamUrl(songId: Long, quality: String): String? = request("song/url/v1", mapOf("id" to songId.toString(), "level" to quality)).array("data").firstOrNull()?.jsonObject?.optionalString("url")

    override suspend fun createLoginQr(): LoginQr {
        val key = request("login/qr/key", emptyMap()).obj("data").string("unikey")
        val data = request("login/qr/create", mapOf("key" to key, "qrimg" to "true")).obj("data")
        return LoginQr(key, data.optionalString("qrimg").orEmpty())
    }

    override suspend fun checkLoginQr(key: String): LoginResult {
        val root = request("login/qr/check", mapOf("key" to key), validateCode = false)
        val code = root["code"]?.jsonPrimitive?.intOrNull
        val status = when (code) { 800 -> LoginStatus.EXPIRED; 801 -> LoginStatus.WAITING; 802 -> LoginStatus.SCANNED; 803 -> LoginStatus.AUTHORIZED; else -> LoginStatus.ERROR }
        return LoginResult(status, root.optionalString("message").orEmpty(), root.optionalString("cookie"))
    }

    override suspend fun dailySongs(): List<Song> = request("recommend/songs", emptyMap()).obj("data").array("dailySongs").map(::song)
    override suspend fun cloudSongs(): List<Song> = request("cloud", mapOf("limit" to "1000")).array("data").map(::song)
    override suspend fun radios(): List<Radio> = request("personalized/djprogram", mapOf("limit" to "30")).array("result").map { value -> val item = value.jsonObject; Radio(item.long("id"), item.string("name"), item.optionalString("copywriter").orEmpty(), item.optionalString("picUrl")) }
    override suspend fun videos(songId: Long): List<Video> = request("mv/detail", mapOf("mvid" to songId.toString())).objOrNull("data")?.let { item -> listOf(Video(item.long("id"), item.string("name"), item.optionalString("url"), item.long("duration"), item.optionalString("cover"))) } ?: emptyList()
    override suspend fun likeComment(songId: Long, commentId: Long, like: Boolean): Boolean { request("comment/like", mapOf("id" to songId.toString(), "cid" to commentId.toString(), "t" to if (like) "1" else "0", "type" to "0"), validateCode = true); return true }

    private suspend fun request(path: String, parameters: Map<String, String>, validateCode: Boolean = true): JsonObject = client.get(baseUrl.trimEnd('/') + "/" + path) {
        parameters.forEach { (key, value) -> parameter(key, value) }
        cookie?.takeIf(String::isNotBlank)?.let { header(HttpHeaders.Cookie, it) }
    }.body<JsonObject>().also { if (validateCode) check(it["code"]?.jsonPrimitive?.intOrNull in setOf(null, 200)) { "网易云接口错误：${it["code"]}" } }

    private fun song(value: JsonElement): Song { val item = value.jsonObject; val artists = (item["ar"] ?: item["artists"])?.jsonArray.orEmpty().map { it.jsonObject.string("name") }; val album = (item["al"] ?: item["album"])?.jsonObject
        return Song(item.long("id"), item.string("name"), artists, album?.optionalString("name").orEmpty(), item["dt"]?.jsonPrimitive?.longOrNull ?: item["duration"]?.jsonPrimitive?.longOrNull ?: 0, album?.optionalString("picUrl"), mvId = item["mv"]?.jsonPrimitive?.longOrNull ?: 0) }
}

private fun JsonObject.array(key: String) = this[key]?.jsonArray.orEmpty()
private fun JsonObject.obj(key: String) = getValue(key).jsonObject
private fun JsonObject.objOrNull(key: String) = this[key]?.jsonObject
private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content
private fun JsonObject.optionalString(key: String) = this[key]?.jsonPrimitive?.contentOrNull
private fun JsonObject.long(key: String) = getValue(key).jsonPrimitive.long
private fun JsonObject.int(key: String) = this[key]?.jsonPrimitive?.intOrNull ?: 0
