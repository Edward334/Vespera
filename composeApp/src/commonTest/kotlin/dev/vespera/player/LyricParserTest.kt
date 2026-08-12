package dev.vespera.player

import dev.vespera.player.lyrics.LyricParser
import dev.vespera.player.lyrics.activeIndex
import dev.vespera.player.model.LyricBundle
import dev.vespera.player.model.LyricLine
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

    @Test fun parsesWordLyricsWithTranslationAndRomanization() {
        val result = LyricParser.parse(
            LyricBundle(
                word = "[1000,1000](1000,400,0)你(1400,600,0)好",
                translated = "[00:01.000]Hello",
                romanized = "[00:01.000]Ni hao",
            ),
        )

        assertEquals(listOf("你", "好"), result.single().words.map { it.text })
        assertEquals(1400, result.single().words.first().endMs)
        assertEquals("Hello", result.single().translation)
        assertEquals("Ni hao", result.single().romanization)
    }

    @Test fun emptyLyricsHaveNoActiveLine() {
        assertEquals(-1, emptyList<LyricLine>().activeIndex(1000))
    }
}
