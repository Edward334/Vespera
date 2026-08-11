package dev.vespera.player

import dev.vespera.player.lyrics.LyricParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LyricParserTest {
    @Test fun parsesAndSortsTimestampedLines() {
        val result = LyricParser.parse("[00:04.00]second\n[00:01.50]first")
        assertEquals(2, result.size)
        assertEquals("first", result.first().words.single().text)
        assertEquals(1500, result.first().startMs)
        assertEquals(4000, result.first().words.single().endMs)
    }
    @Test fun ignoresMetadataAndMalformedLines() { assertTrue(LyricParser.parse("[ar:artist]\nnot lrc").isEmpty()) }
}
