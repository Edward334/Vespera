package dev.vespera.player.data

import dev.vespera.player.model.*
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

class NeteaseMusicApi(
    private val client: HttpClient,
    storedCookie: String? = SessionStore.loadCookie(),
) : MusicApi {
    private val domain = "https://interface.music.163.com"
    private val cookies = linkedMapOf<String, String>()
    private val cookieMutex = Mutex()
    private val _session = MutableStateFlow(AccountSession())
    override val session = _session.asStateFlow()

    init {
        storedCookie.orEmpty().split(';').forEach { part ->
            val separator = part.indexOf('=')
            if (separator > 0) cookies[part.substring(0, separator).trim()] = part.substring(separator + 1).trim()
        }
    }

    override suspend fun search(keyword: String, page: Int): List<Song> = request(
        "/api/cloudsearch/pc",
        mapOf("s" to keyword, "type" to "1", "limit" to "30", "offset" to ((page - 1) * 30).toString(), "total" to "true"),
    ).obj("result").array("songs").map(::song)

    override suspend fun searchCatalog(keyword: String, type: SearchType, page: Int): SearchPage {
        val result = request(
            "/api/cloudsearch/pc",
            mapOf(
                "s" to keyword,
                "type" to type.code.toString(),
                "limit" to "30",
                "offset" to ((page - 1) * 30).toString(),
                "total" to "true",
            ),
        ).obj("result")
        return SearchPage(
            type = type,
            songs = if (type == SearchType.SONG) result.array("songs").map(::song) else emptyList(),
            playlists = if (type == SearchType.PLAYLIST) result.array("playlists").map(::playlist) else emptyList(),
            artists = if (type == SearchType.ARTIST) result.array("artists").map(::artist) else emptyList(),
            albums = if (type == SearchType.ALBUM) result.array("albums").map(::album) else emptyList(),
            videos = if (type == SearchType.VIDEO) result.array("videos").map(::video) else emptyList(),
            radios = if (type == SearchType.RADIO) result.array("djRadios").map(::radio) else emptyList(),
            hasMore = result.boolean("hasMore"),
        )
    }

    override suspend fun hotSearch(): List<String> = request("/api/search/hot/detail", emptyMap())
        .array("data").mapNotNull { it.jsonObject.optionalString("searchWord") }

    override suspend fun searchSuggestions(keyword: String): List<String> = request(
        "/api/search/suggest/web",
        mapOf("s" to keyword),
    ).objOrNull("result")?.array("allMatch").orEmpty().mapNotNull { it.jsonObject.optionalString("keyword") }

    override suspend fun playlists(): List<Playlist> {
        val uid = session.value.profile?.id ?: refreshAccount()?.id ?: error("请先登录网易云账号")
        return request("/api/user/playlist", mapOf("uid" to uid.toString(), "limit" to "1000", "offset" to "0", "includeVideo" to "true"))
            .array("playlist").map { value ->
                val item = value.jsonObject
                playlist(item)
            }
    }

    override suspend fun playlistDetail(id: Long): PlaylistDetail {
        val root = request("/api/v6/playlist/detail", mapOf("id" to id.toString(), "n" to "100000", "s" to "8"))
        val playlistObject = root.obj("playlist")
        val metadata = playlist(playlistObject)
        val ids = playlistObject.array("trackIds").mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.longOrNull }
        val tracks = if (ids.isEmpty()) {
            playlistObject.array("tracks").map(::song)
        } else {
            songDetails(ids)
        }
        return PlaylistDetail(metadata.copy(trackCount = maxOf(metadata.trackCount, tracks.size)), tracks)
    }

    override suspend fun playlistTracks(id: Long): List<Song> = playlistDetail(id).tracks

    override suspend fun lyrics(songId: Long): LyricBundle {
        val root = request(
            "/api/song/lyric/v1",
            mapOf("id" to songId.toString(), "cp" to "false", "tv" to "0", "lv" to "0", "rv" to "0", "kv" to "0", "yv" to "0", "ytv" to "0", "yrv" to "0"),
        )
        return LyricBundle(
            line = root.objOrNull("lrc")?.optionalString("lyric").orEmpty(),
            word = root.objOrNull("yrc")?.optionalString("lyric").orEmpty(),
            translated = root.objOrNull("tlyric")?.optionalString("lyric").orEmpty(),
            romanized = root.objOrNull("romalrc")?.optionalString("lyric").orEmpty(),
        )
    }

    override suspend fun comments(songId: Long, page: Int): List<Comment> = commentPage(songId, page).comments

    override suspend fun commentPage(songId: Long, page: Int, sort: CommentSort, cursor: Long?): CommentPage {
        val requestCursor = when (sort) {
            CommentSort.RECOMMENDED -> ((page - 1) * 20).toString()
            CommentSort.HOT -> "normalHot#${(page - 1) * 20}"
            CommentSort.NEWEST -> cursor?.toString() ?: "0"
        }
        val data = request(
            "/api/v2/resource/comments",
            mapOf(
                "threadId" to "R_SO_4_$songId",
                "pageNo" to page.toString(),
                "showInner" to "true",
                "pageSize" to "20",
                "cursor" to requestCursor,
                "sortType" to sort.code.toString(),
            ),
        ).obj("data")
        val comments = data.array("comments").map(::comment)
        return CommentPage(
            comments = comments,
            totalCount = data.int("totalCount"),
            hasMore = data.boolean("hasMore"),
            nextCursor = data.optionalLong("cursor") ?: comments.lastOrNull()?.timeMs,
        )
    }

    override suspend fun hotComments(songId: Long, page: Int): CommentPage {
        val root = request(
            "/api/v1/resource/hotcomments/R_SO_4_$songId",
            mapOf("rid" to songId.toString(), "limit" to "20", "offset" to ((page - 1) * 20).toString(), "beforeTime" to "0"),
        )
        val comments = root.array("hotComments").map(::comment)
        return CommentPage(comments, hasMore = root.boolean("hasMore"), nextCursor = comments.lastOrNull()?.timeMs)
    }

    override suspend fun commentReplies(songId: Long, parentCommentId: Long, cursor: Long?): CommentPage {
        val data = request(
            "/api/resource/comment/floor/get",
            mapOf(
                "parentCommentId" to parentCommentId.toString(),
                "threadId" to "R_SO_4_$songId",
                "time" to (cursor ?: -1).toString(),
                "limit" to "20",
            ),
        ).obj("data")
        val comments = data.array("comments").map(::comment)
        return CommentPage(comments, hasMore = data.boolean("hasMore"), nextCursor = data.optionalLong("time") ?: comments.lastOrNull()?.timeMs)
    }

    override suspend fun postComment(songId: Long, content: String, replyToCommentId: Long?): Boolean {
        require(content.isNotBlank()) { "评论内容不能为空" }
        val common = mutableMapOf(
            "threadId" to "R_SO_4_$songId",
            "content" to content.trim(),
            "resourceType" to "0",
        )
        val path = if (replyToCommentId == null) {
            common["expressionPicId"] = "-1"
            common["bubbleId"] = "-1"
            "/api/resource/comments/add"
        } else {
            common["commentId"] = replyToCommentId.toString()
            "/api/v1/resource/comments/reply"
        }
        request(path, common)
        return true
    }

    override suspend fun hugComment(songId: Long, commentId: Long, targetUserId: Long): Boolean {
        request(
            "/api/v2/resource/comments/hug/listener",
            mapOf("targetUserId" to targetUserId.toString(), "commentId" to commentId.toString(), "threadId" to "R_SO_4_$songId"),
        )
        return true
    }

    private fun comment(value: JsonElement): Comment {
        val item = value.jsonObject
        val user = item.obj("user")
        val replied = item.array("beReplied").firstOrNull()?.jsonObject
        return Comment(
            id = item.long("commentId"),
            user = user.string("nickname"),
            avatarUrl = user.optionalString("avatarUrl"),
            content = item.string("content"),
            likedCount = item.int("likedCount"),
            timeLabel = item.optionalString("timeStr").orEmpty(),
            liked = item.boolean("liked"),
            replyContent = replied?.optionalString("content"),
            userId = user.optionalLong("userId") ?: 0,
            timeMs = item.optionalLong("time") ?: 0,
            ipLocation = item.objOrNull("ipLocation")?.optionalString("location").orEmpty(),
            replyUser = replied?.objOrNull("user")?.optionalString("nickname"),
            replyCount = item.objOrNull("showFloorComment")?.int("replyCount") ?: 0,
        )
    }

    override suspend fun streamUrl(songId: Long, quality: String): String? = request(
        "/api/song/enhance/player/url/v1",
        mapOf("ids" to "[$songId]", "level" to quality, "encodeType" to "flac"),
    ).array("data").firstOrNull()?.jsonObject?.optionalString("url")

    override suspend fun createLoginQr(): LoginQr {
        val key = request("/api/login/qrcode/unikey", mapOf("type" to "3")).string("unikey")
        return LoginQr(key, "https://music.163.com/login?codekey=$key")
    }

    override suspend fun checkLoginQr(key: String): LoginResult {
        val root = request("/api/login/qrcode/client/login", mapOf("key" to key, "type" to "3"), validateCode = false)
        val code = root["code"]?.jsonPrimitive?.intOrNull
        val status = when (code) {
            800 -> LoginStatus.EXPIRED
            801 -> LoginStatus.WAITING
            802 -> LoginStatus.SCANNED
            803 -> LoginStatus.AUTHORIZED
            else -> LoginStatus.ERROR
        }
        if (status == LoginStatus.AUTHORIZED) refreshAccount()
        return LoginResult(status, root.optionalString("message").orEmpty(), cookieHeader().ifBlank { null })
    }

    override suspend fun refreshAccount(): UserProfile? {
        _session.value = _session.value.copy(loading = true, error = null)
        return runCatching {
            val root = request("/api/nuser/account/get", emptyMap(), validateCode = false)
            val profile = root.objOrNull("profile")?.let {
                UserProfile(it.long("userId"), it.string("nickname"), it.optionalString("avatarUrl"), it.int("vipType"))
            }
            _session.value = AccountSession(profile = profile)
            profile
        }.getOrElse {
            _session.value = AccountSession(error = it.message)
            null
        }
    }

    override suspend fun sendPhoneCaptcha(phone: String, countryCode: String): Boolean {
        val root = request(
            "/api/sms/captcha/sent",
            mapOf("ctcode" to countryCode, "secrete" to "music_middleuser_pclogin", "cellphone" to phone),
        )
        return root["code"]?.jsonPrimitive?.intOrNull == 200
    }

    override suspend fun loginWithPhone(phone: String, captcha: String, countryCode: String): UserProfile? {
        request(
            "/api/w/login/cellphone",
            mapOf("type" to "1", "https" to "true", "phone" to phone, "countrycode" to countryCode, "captcha" to captcha, "remember" to "true"),
        )
        return refreshAccount()
    }

    override suspend fun logout() {
        runCatching { request("/api/logout", emptyMap(), validateCode = false) }
        cookieMutex.withLock { cookies.clear() }
        SessionStore.clear()
        _session.value = AccountSession()
    }

    override suspend fun dailySongs(): List<Song> {
        if (session.value.loggedIn) {
            runCatching { request("/api/v3/discovery/recommend/songs", emptyMap()).obj("data").array("dailySongs").map(::song) }
                .getOrNull()?.takeIf(List<Song>::isNotEmpty)?.let { return it }
        }
        return request("/api/personalized/newsong", emptyMap()).array("result").map { value ->
            value.jsonObject["song"]?.let(::song) ?: song(value)
        }
    }

    override suspend fun cloudSongs(): List<Song> = request("/api/v1/cloud/get", mapOf("limit" to "1000", "offset" to "0"))
        .array("data").mapNotNull { value ->
            val item = value.jsonObject
            (item["simpleSong"] ?: value).let(::song)
        }

    override suspend fun likedSongs(): List<Song> {
        val uid = session.value.profile?.id ?: refreshAccount()?.id ?: error("请先登录网易云账号")
        val ids = request("/api/song/like/get", mapOf("uid" to uid.toString()))
            .array("ids").mapNotNull { it.jsonPrimitive.longOrNull }
        return songDetails(ids)
    }

    override suspend fun likeSong(songId: Long, like: Boolean): Boolean {
        request(
            "/api/radio/like",
            mapOf("alg" to "itembased", "trackId" to songId.toString(), "like" to like.toString(), "time" to "3"),
        )
        return true
    }

    override suspend fun subscribePlaylist(playlistId: Long, subscribe: Boolean): Boolean {
        request("/api/playlist/${if (subscribe) "subscribe" else "unsubscribe"}", mapOf("id" to playlistId.toString()))
        return true
    }

    override suspend fun subscribedAlbums(): List<Album> = request(
        "/api/album/sublist",
        mapOf("limit" to "1000", "offset" to "0", "total" to "true"),
    ).array("data").map(::album)

    override suspend fun subscribeAlbum(albumId: Long, subscribe: Boolean): Boolean {
        request("/api/album/${if (subscribe) "sub" else "unsub"}", mapOf("id" to albumId.toString()))
        return true
    }

    override suspend fun subscribedArtists(): List<Artist> = request(
        "/api/artist/sublist",
        mapOf("limit" to "1000", "offset" to "0", "total" to "true"),
    ).array("data").map(::artist)

    override suspend fun subscribeArtist(artistId: Long, subscribe: Boolean): Boolean {
        request(
            "/api/artist/${if (subscribe) "sub" else "unsub"}",
            mapOf("artistId" to artistId.toString(), "artistIds" to "[$artistId]"),
        )
        return true
    }

    override suspend fun subscribedVideos(): List<Video> = request(
        "/api/cloudvideo/allvideo/sublist",
        mapOf("limit" to "1000", "offset" to "0", "total" to "true"),
    ).array("data").map(::video)

    override suspend fun subscribeVideo(videoId: String, subscribe: Boolean): Boolean {
        request(
            "/api/mv/${if (subscribe) "sub" else "unsub"}",
            mapOf("mvId" to videoId, "mvIds" to "[\"$videoId\"]"),
        )
        return true
    }

    override suspend fun subscribedRadios(): List<Radio> = request(
        "/api/djradio/get/subed",
        mapOf("limit" to "1000", "offset" to "0", "total" to "true"),
    ).array("djRadios").map(::radio)

    override suspend fun subscribeRadio(radioId: Long, subscribe: Boolean): Boolean {
        request("/api/djradio/${if (subscribe) "sub" else "unsub"}", mapOf("id" to radioId.toString()))
        return true
    }

    override suspend fun radios(): List<Radio> = request("/api/personalized/djprogram", emptyMap()).array("result").map { value ->
        val item = value.jsonObject
        Radio(item.long("id"), item.string("name"), item.optionalString("copywriter").orEmpty(), item.optionalString("picUrl"))
    }

    override suspend fun videos(songId: Long): List<Video> {
        val item = request("/api/v1/mv/detail", mapOf("id" to songId.toString())).objOrNull("data") ?: return emptyList()
        return listOf(Video(item.long("id").toString(), item.string("name"), null, item.long("duration"), item.optionalString("cover")))
    }

    override suspend fun likeComment(songId: Long, commentId: Long, like: Boolean): Boolean {
        request(
            "/api/v1/comment/${if (like) "like" else "unlike"}",
            mapOf("threadId" to "R_SO_4_$songId", "commentId" to commentId.toString()),
        )
        return true
    }

    private suspend fun request(path: String, data: Map<String, String>, validateCode: Boolean = true): JsonObject {
        val cookie = cookieHeader()
        val response = client.submitForm(
            url = domain + path,
            formParameters = Parameters.build { data.forEach { (key, value) -> append(key, value) } },
        ) {
            header(HttpHeaders.UserAgent, "NeteaseMusic 9.0.90/5038 (iPhone; iOS 16.2; zh_CN)")
            cookie.takeIf(String::isNotBlank)?.let { header(HttpHeaders.Cookie, it) }
        }
        captureCookies(response.headers.getAll(HttpHeaders.SetCookie).orEmpty())
        val root = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val code = root["code"]?.jsonPrimitive?.intOrNull
        if (validateCode && code !in setOf(null, 200)) error(root.optionalString("message") ?: root.optionalString("msg") ?: "网易云接口错误：$code")
        return root
    }

    private suspend fun cookieHeader(): String = cookieMutex.withLock {
        cookies.entries.joinToString("; ") { (name, value) -> "$name=$value" }
    }

    private suspend fun captureCookies(headers: List<String>) {
        if (headers.isEmpty()) return
        val value = cookieMutex.withLock {
            headers.forEach { header ->
                val pair = header.substringBefore(';')
                val separator = pair.indexOf('=')
                if (separator > 0) cookies[pair.substring(0, separator).trim()] = pair.substring(separator + 1).trim()
            }
            cookies.entries.joinToString("; ") { (name, cookie) -> "$name=$cookie" }
        }
        if (value.isNotBlank()) SessionStore.saveCookie(value)
    }

    private fun song(value: JsonElement): Song {
        val item = value.jsonObject
        val artists = (item["ar"] ?: item["artists"])?.jsonArray.orEmpty().map { it.jsonObject }
        val album = (item["al"] ?: item["album"])?.jsonObject
        return Song(
            id = item.long("id"),
            name = item.string("name"),
            artists = artists.map { it.string("name") },
            album = album?.optionalString("name").orEmpty(),
            durationMs = item["dt"]?.jsonPrimitive?.longOrNull ?: item["duration"]?.jsonPrimitive?.longOrNull ?: 0,
            coverUrl = album?.optionalString("picUrl"),
            mvId = item["mv"]?.jsonPrimitive?.longOrNull ?: item["mvid"]?.jsonPrimitive?.longOrNull ?: 0,
            artistIds = artists.mapNotNull { it["id"]?.jsonPrimitive?.longOrNull },
            albumId = album?.get("id")?.jsonPrimitive?.longOrNull ?: 0,
            aliases = item.array("alia").mapNotNull { it.jsonPrimitive.contentOrNull },
            fee = item.int("fee"),
        )
    }

    private suspend fun songDetails(ids: List<Long>): List<Song> {
        if (ids.isEmpty()) return emptyList()
        val byId = ids.chunked(500).flatMap { chunk ->
            val payload = buildJsonArray { chunk.forEach { add(buildJsonObject { put("id", it) }) } }.toString()
            request("/api/v3/song/detail", mapOf("c" to payload)).array("songs").map(::song)
        }.associateBy(Song::id)
        return ids.mapNotNull(byId::get)
    }

    private fun playlist(value: JsonElement): Playlist = playlist(value.jsonObject)

    private fun playlist(item: JsonObject): Playlist = Playlist(
        id = item.long("id"),
        name = item.string("name"),
        trackCount = item.int("trackCount"),
        coverUrl = item.optionalString("coverImgUrl"),
        description = item.optionalString("description").orEmpty(),
        creator = item.objOrNull("creator")?.optionalString("nickname").orEmpty(),
        subscribed = item.boolean("subscribed"),
    )

    private fun artist(value: JsonElement): Artist {
        val item = value.jsonObject
        return Artist(
            id = item.long("id"),
            name = item.string("name"),
            coverUrl = item.optionalString("picUrl") ?: item.optionalString("img1v1Url"),
            aliases = (item["alias"] ?: item["alia"])?.jsonArray.orEmpty().mapNotNull { it.jsonPrimitive.contentOrNull },
            albumCount = item.int("albumSize"),
            songCount = item.int("musicSize"),
        )
    }

    private fun album(value: JsonElement): Album {
        val item = value.jsonObject
        return Album(
            id = item.long("id"),
            name = item.string("name"),
            artist = item.objOrNull("artist")?.optionalString("name")
                ?: item.array("artists").joinToString(" · ") { it.jsonObject.string("name") },
            coverUrl = item.optionalString("picUrl"),
            songCount = item.int("size"),
            publishTime = item["publishTime"]?.jsonPrimitive?.longOrNull ?: 0,
        )
    }

    private fun video(value: JsonElement): Video {
        val item = value.jsonObject
        return Video(
            id = item.optionalString("vid") ?: item["id"]?.jsonPrimitive?.content.orEmpty(),
            title = item.optionalString("title") ?: item.optionalString("name").orEmpty(),
            url = null,
            durationMs = item["durationms"]?.jsonPrimitive?.longOrNull ?: item["duration"]?.jsonPrimitive?.longOrNull ?: 0,
            coverUrl = item.optionalString("coverUrl") ?: item.optionalString("cover"),
            creator = item.array("creator").joinToString(" · ") { it.jsonObject.optionalString("userName").orEmpty() },
        )
    }

    private fun radio(value: JsonElement): Radio {
        val item = value.jsonObject
        return Radio(
            id = item.long("id"),
            name = item.string("name"),
            description = item.optionalString("rcmdtext") ?: item.optionalString("desc").orEmpty(),
            coverUrl = item.optionalString("picUrl"),
        )
    }
}

private fun JsonObject.array(key: String) = (this[key] as? JsonArray).orEmpty()
private fun JsonObject.obj(key: String) = getValue(key).jsonObject
private fun JsonObject.objOrNull(key: String): JsonObject? = this[key]?.let { if (it is JsonNull) null else it.jsonObject }
private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content
private fun JsonObject.optionalString(key: String) = this[key]?.let { if (it is JsonNull) null else it.jsonPrimitive.contentOrNull }
private fun JsonObject.long(key: String) = getValue(key).jsonPrimitive.long
private fun JsonObject.optionalLong(key: String) = this[key]?.jsonPrimitive?.longOrNull
private fun JsonObject.int(key: String) = this[key]?.jsonPrimitive?.intOrNull ?: 0
private fun JsonObject.boolean(key: String) = this[key]?.jsonPrimitive?.booleanOrNull ?: false
