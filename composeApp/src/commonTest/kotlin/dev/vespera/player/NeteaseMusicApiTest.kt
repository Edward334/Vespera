package dev.vespera.player

import dev.vespera.player.data.NeteaseMusicApi
import dev.vespera.player.model.SearchType
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NeteaseMusicApiTest {
    @Test
    fun searchUsesOfficialEndpointAndParsesSongMetadata() = runTest {
        val engine = MockEngine { request ->
            assertEquals("interface.music.163.com", request.url.host)
            assertEquals("/api/cloudsearch/pc", request.url.encodedPath)
            respond(
                content = """{"code":200,"result":{"songs":[{"id":7,"name":"Song","ar":[{"id":8,"name":"Artist"}],"al":{"id":9,"name":"Album","picUrl":"https://cover"},"dt":1234,"mv":10,"fee":0}]}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val song = NeteaseMusicApi(HttpClient(engine), storedCookie = "").search("Song").single()

        assertEquals(7L, song.id)
        assertEquals(listOf("Artist"), song.artists)
        assertEquals(listOf(8L), song.artistIds)
        assertEquals(9L, song.albumId)
        assertEquals(10L, song.mvId)
    }

    @Test
    fun lyricResponseKeepsWordTranslationAndRomanizationTracks() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"code":200,"lrc":{"lyric":"[00:01.00]Line"},"yrc":{"lyric":"[1000,500](1000,500,0)Word"},"tlyric":{"lyric":"[00:01.00]Translation"},"romalrc":{"lyric":"[00:01.00]Romanization"}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val lyrics = NeteaseMusicApi(HttpClient(engine), storedCookie = "").lyrics(7)

        assertTrue(lyrics.word.contains("Word"))
        assertTrue(lyrics.translated.contains("Translation"))
        assertTrue(lyrics.romanized.contains("Romanization"))
    }

    @Test
    fun catalogSearchParsesAllSixResultTypes() = runTest {
        val expectedTypes = ArrayDeque(SearchType.entries.map { it.code.toString() })
        val responses = ArrayDeque(
            listOf(
                """{"code":200,"result":{"songs":[{"id":1,"name":"Song","ar":[{"id":2,"name":"Artist"}],"al":{"id":3,"name":"Album"}}],"hasMore":true}}""",
                """{"code":200,"result":{"playlists":[{"id":4,"name":"Playlist","trackCount":12,"coverImgUrl":"playlist-cover","creator":{"nickname":"Creator"}}]}}""",
                """{"code":200,"result":{"artists":[{"id":5,"name":"Singer","picUrl":"artist-cover","alias":["Alias"],"albumSize":6,"musicSize":7}]}}""",
                """{"code":200,"result":{"albums":[{"id":8,"name":"Record","artist":{"name":"Singer"},"picUrl":"album-cover","size":9,"publishTime":10}]}}""",
                """{"code":200,"result":{"videos":[{"vid":"video-id","title":"Video","durationms":11,"coverUrl":"video-cover","creator":[{"userName":"Director"}]}]}}""",
                """{"code":200,"result":{"djRadios":[{"id":12,"name":"Radio","rcmdtext":"Description","picUrl":"radio-cover"}]}}""",
            ),
        )
        val engine = MockEngine { request ->
            val body = request.body as FormDataContent
            assertEquals(expectedTypes.removeFirst(), body.formData["type"])
            respond(
                content = responses.removeFirst(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = NeteaseMusicApi(HttpClient(engine), storedCookie = "")

        val songPage = api.searchCatalog("query", SearchType.SONG)
        val playlistPage = api.searchCatalog("query", SearchType.PLAYLIST)
        val artistPage = api.searchCatalog("query", SearchType.ARTIST)
        val albumPage = api.searchCatalog("query", SearchType.ALBUM)
        val videoPage = api.searchCatalog("query", SearchType.VIDEO)
        val radioPage = api.searchCatalog("query", SearchType.RADIO)

        assertEquals("Song", songPage.songs.single().name)
        assertTrue(songPage.hasMore)
        assertEquals("Creator", playlistPage.playlists.single().creator)
        assertEquals(listOf("Alias"), artistPage.artists.single().aliases)
        assertEquals("Singer", albumPage.albums.single().artist)
        assertEquals("video-id", videoPage.videos.single().id)
        assertEquals("Director", videoPage.videos.single().creator)
        assertEquals("Description", radioPage.radios.single().description)
        assertTrue(expectedTypes.isEmpty())
    }

    @Test
    fun hotSearchAndSuggestionsUseNativeEndpoints() = runTest {
        val responses = ArrayDeque(
            listOf(
                """{"code":200,"data":[{"searchWord":"Hot one"},{"searchWord":"Hot two"}]}""",
                """{"code":200,"result":{"allMatch":[{"keyword":"Suggestion one"},{"keyword":"Suggestion two"}]}}""",
            ),
        )
        val paths = mutableListOf<String>()
        val engine = MockEngine { request ->
            paths += request.url.encodedPath
            respond(
                content = responses.removeFirst(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = NeteaseMusicApi(HttpClient(engine), storedCookie = "")

        assertEquals(listOf("Hot one", "Hot two"), api.hotSearch())
        assertEquals(listOf("Suggestion one", "Suggestion two"), api.searchSuggestions("Suggestion"))
        assertEquals(listOf("/api/search/hot/detail", "/api/search/suggest/web"), paths)
    }

    @Test
    fun playlistDetailLoadsEveryTrackInPlaylistOrder() = runTest {
        val paths = mutableListOf<String>()
        val engine = MockEngine { request ->
            paths += request.url.encodedPath
            val content = when (request.url.encodedPath) {
                "/api/v6/playlist/detail" -> """{"code":200,"playlist":{"id":42,"name":"Playlist","trackCount":2,"coverImgUrl":"cover","description":"Description","creator":{"nickname":"Creator"},"trackIds":[{"id":1},{"id":2}]}}"""
                "/api/v3/song/detail" -> {
                    val body = request.body as FormDataContent
                    assertTrue(body.formData["c"].orEmpty().contains("\"id\":1"))
                    assertTrue(body.formData["c"].orEmpty().contains("\"id\":2"))
                    """{"code":200,"songs":[{"id":2,"name":"Second","ar":[{"name":"Artist"}],"al":{"name":"Album"}},{"id":1,"name":"First","ar":[{"name":"Artist"}],"al":{"name":"Album"}}]}"""
                }
                else -> error("Unexpected endpoint: ${request.url.encodedPath}")
            }
            respond(
                content = content,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val detail = NeteaseMusicApi(HttpClient(engine), storedCookie = "").playlistDetail(42)

        assertEquals("Playlist", detail.playlist.name)
        assertEquals("Creator", detail.playlist.creator)
        assertEquals("Description", detail.playlist.description)
        assertEquals(listOf(1L, 2L), detail.tracks.map { it.id })
        assertEquals(listOf("/api/v6/playlist/detail", "/api/v3/song/detail"), paths)
    }
}
