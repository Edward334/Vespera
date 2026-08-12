package dev.vespera.player

import dev.vespera.player.model.*
import dev.vespera.player.player.PlayerController
import dev.vespera.player.player.AudioEngine
import dev.vespera.player.player.AbLoopController
import kotlin.test.*

class PlayerControllerTest {
    private class FakeAudio : AudioEngine { override fun load(url: String) = Unit; override fun play() = Unit; override fun pause() = Unit; override fun seek(positionMs: Long) = Unit; override fun setVolume(volume: Float) = Unit; override fun setRate(rate: Float) = Unit; override fun positionMs() = 0L; override fun release() = Unit }
    private val song = Song(1, "Song", listOf("Artist"), durationMs = 10_000, streamUrl = "https://audio")
    @Test fun playUpdatesCurrentTrackAndQueue() { val player = PlayerController(FakeAudio()); player.play(song); assertEquals(song, player.state.value.current); assertTrue(player.state.value.playing); assertEquals(listOf(song), player.state.value.queue) }
    @Test fun volumeAndSeekAreClamped() { val player = PlayerController(FakeAudio()); player.setVolume(2f); player.seek(-10); assertEquals(1f, player.state.value.volume); assertEquals(0, player.state.value.positionMs) }
    @Test fun repeatCyclesThroughAllModes() { val player = PlayerController(FakeAudio()); player.cycleRepeat(); assertEquals(RepeatMode.ONE, player.state.value.repeat); player.cycleRepeat(); assertEquals(RepeatMode.OFF, player.state.value.repeat) }
    @Test fun replacingQueueKeepsOrderAndProvidesLazyNextCandidate() { val player = PlayerController(FakeAudio()); val second = Song(2, "Second", listOf("Artist")); player.replaceQueue(listOf(song, second)); player.play(song); assertEquals(listOf(1L, 2L), player.state.value.queue.map { it.id }); assertEquals(second, player.nextSong()) }
    @Test fun unresolvedSongDoesNotClaimToBePlaying() { val player = PlayerController(FakeAudio()); player.play(song.copy(streamUrl = null)); assertFalse(player.state.value.playing) }
    @Test fun abLoopJumpsBackAfterEnd() { val loop = AbLoopController(); loop.setStart(1000); loop.setEnd(5000); assertEquals(1000, loop.nextPosition(5000)); assertNull(loop.nextPosition(4999)) }
}
