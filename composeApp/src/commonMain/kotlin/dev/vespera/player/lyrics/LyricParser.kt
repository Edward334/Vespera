package dev.vespera.player.lyrics

import dev.vespera.player.model.LyricBundle
import dev.vespera.player.model.LyricLine
import dev.vespera.player.model.LyricWord

object LyricParser {
    private val lrcLine = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?](.*)")
    private val yrcLine = Regex("\\[(\\d+),(\\d+)](.*)")
    private val yrcWord = Regex("\\((\\d+),(\\d+),(?:\\d+)\\)(.*?)(?=\\(\\d+,\\d+,(?:\\d+)\\)|$)")

    fun parse(bundle: LyricBundle): List<LyricLine> {
        val translations = timedText(bundle.translated)
        val romanizations = timedText(bundle.romanized)
        val wordLines = parseWordLyrics(bundle.word, translations, romanizations)
        return if (wordLines.isNotEmpty()) wordLines else parseLineLyrics(bundle.line, translations, romanizations)
    }

    fun parse(lrc: String): List<LyricLine> = parse(LyricBundle(line = lrc))

    private fun parseWordLyrics(
        raw: String,
        translations: Map<Long, String>,
        romanizations: Map<Long, String>,
    ): List<LyricLine> = raw.lineSequence().mapNotNull { source ->
        val match = yrcLine.matchEntire(source.trim()) ?: return@mapNotNull null
        val start = match.groupValues[1].toLong()
        val duration = match.groupValues[2].toLong()
        val words = yrcWord.findAll(match.groupValues[3]).map { word ->
            val wordStart = word.groupValues[1].toLong()
            val wordDuration = word.groupValues[2].toLong()
            LyricWord(word.groupValues[3], wordStart, wordStart + wordDuration)
        }.toList()
        if (words.isEmpty()) return@mapNotNull null
        LyricLine(start, start + duration, words, translations[start], romanizations[start])
    }.sortedBy(LyricLine::startMs).toList()

    private fun parseLineLyrics(
        raw: String,
        translations: Map<Long, String>,
        romanizations: Map<Long, String>,
    ): List<LyricLine> {
        val parsed = timedText(raw).entries.sortedBy(Map.Entry<Long, String>::key)
        return parsed.mapIndexed { index, (start, text) ->
            val end = parsed.getOrNull(index + 1)?.key ?: Long.MAX_VALUE
            LyricLine(start, end, listOf(LyricWord(text, start, end)), translations[start], romanizations[start])
        }
    }

    private fun timedText(raw: String): Map<Long, String> = buildMap {
        raw.lineSequence().forEach { source ->
            val match = lrcLine.matchEntire(source.trim()) ?: return@forEach
            val minute = match.groupValues[1].toLong()
            val second = match.groupValues[2].toLong()
            val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0
            put(minute * 60_000 + second * 1_000 + fraction, match.groupValues[4].trim())
        }
    }
}

fun List<LyricLine>.activeIndex(positionMs: Long): Int = indexOfLast { positionMs >= it.startMs }
