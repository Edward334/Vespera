package dev.vespera.player

import dev.vespera.player.data.NeteaseMusicApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
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
}
