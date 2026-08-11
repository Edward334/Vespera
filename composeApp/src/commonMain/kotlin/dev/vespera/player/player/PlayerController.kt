package dev.vespera.player.player

import dev.vespera.player.model.*
import kotlinx.coroutines.flow.*

class PlayerController(private val audio: AudioEngine) {
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()
    fun play(song: Song) { song.streamUrl?.let { audio.load(it); audio.play() }; _state.update { it.copy(current = song, playing = true, positionMs = 0, queue = (it.queue + song).distinctBy(Song::id)) } }
    fun toggle() { _state.update { val playing = !it.playing; if (playing) audio.play() else audio.pause(); it.copy(playing = playing) } }
    fun seek(positionMs: Long) { val position = positionMs.coerceAtLeast(0); audio.seek(position); _state.update { it.copy(positionMs = position) } }
    fun enqueue(songs: List<Song>) { _state.update { it.copy(queue = (it.queue + songs).distinctBy(Song::id)) } }
    fun setVolume(volume: Float) { val value = volume.coerceIn(0f, 1f); audio.setVolume(value); _state.update { it.copy(volume = value) } }
    fun setRate(rate: Float) { audio.setRate(rate.coerceIn(0.5f, 2f)) }
    fun cycleRepeat() { _state.update { it.copy(repeat = RepeatMode.entries[(it.repeat.ordinal + 1) % RepeatMode.entries.size]) } }
    fun syncPosition() { _state.update { it.copy(positionMs = audio.positionMs().coerceAtLeast(0)) } }
    fun next() { val state = _state.value; val index = state.queue.indexOfFirst { it.id == state.current?.id }; state.queue.getOrNull(index + 1)?.let(::play) ?: if (state.repeat == RepeatMode.ALL) state.queue.firstOrNull()?.let(::play) else Unit }
    fun previous() { val state = _state.value; val index = state.queue.indexOfFirst { it.id == state.current?.id }; state.queue.getOrNull(index - 1)?.let(::play) ?: if (state.repeat == RepeatMode.ALL) state.queue.lastOrNull()?.let(::play) else Unit }
}
