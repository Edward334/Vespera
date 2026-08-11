package dev.vespera.player.lyrics

import dev.vespera.player.model.*

object LyricParser {
    private val line = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?](.*)")
    fun parse(lrc: String): List<LyricLine> {
        val parsed = lrc.lineSequence().mapNotNull { raw ->
        val match = line.matchEntire(raw.trim()) ?: return@mapNotNull null
        val minute = match.groupValues[1].toLong(); val second = match.groupValues[2].toLong()
        val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0
        val start = minute * 60_000 + second * 1_000 + fraction
        LyricLine(start, listOf(LyricWord(match.groupValues[4].trim(), start, Long.MAX_VALUE)))
        }.sortedBy { it.startMs }.toList()
        return parsed.mapIndexed { index, value ->
        val end = parsed.getOrNull(index + 1)?.startMs ?: Long.MAX_VALUE
        value.copy(words = value.words.map { it.copy(endMs = end) })
        }
    }
}

fun List<LyricLine>.activeIndex(positionMs: Long): Int = indexOfLast { it.startMs <= positionMs }.coerceAtLeast(0)
