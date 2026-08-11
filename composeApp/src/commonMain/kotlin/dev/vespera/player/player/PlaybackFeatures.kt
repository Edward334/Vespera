package dev.vespera.player.player

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class AbLoopState(val enabled: Boolean = false, val startMs: Long = 0, val endMs: Long = 0)

class AbLoopController {
    private val _state = MutableStateFlow(AbLoopState())
    val state: StateFlow<AbLoopState> = _state.asStateFlow()
    fun setStart(positionMs: Long) { _state.update { it.copy(startMs = positionMs, enabled = it.endMs > positionMs) } }
    fun setEnd(positionMs: Long) { _state.update { it.copy(endMs = positionMs, enabled = positionMs > it.startMs) } }
    fun toggle() { _state.update { it.copy(enabled = !it.enabled && it.endMs > it.startMs) } }
    fun clear() { _state.value = AbLoopState() }
    fun nextPosition(positionMs: Long): Long? = _state.value.takeIf { it.enabled && positionMs >= it.endMs }?.startMs
}

data class EqualizerState(val enabled: Boolean = false, val bands: List<Float> = List(10) { 0f }, val preset: String = "平直")
class PlaybackFeatures(private val scope: CoroutineScope) {
    private var autoCloseJob: Job? = null
    private val _equalizer = MutableStateFlow(EqualizerState())
    val equalizer: StateFlow<EqualizerState> = _equalizer.asStateFlow()
    private val _speed = MutableStateFlow(1f)
    val speed: StateFlow<Float> = _speed.asStateFlow()
    private val _autoCloseRemaining = MutableStateFlow<Long?>(null)
    val autoCloseRemaining: StateFlow<Long?> = _autoCloseRemaining.asStateFlow()
    fun setSpeed(value: Float) { _speed.value = value.coerceIn(0.5f, 2f) }
    fun setBand(index: Int, value: Float) { if (index in 0..9) _equalizer.update { it.copy(bands = it.bands.toMutableList().also { bands -> bands[index] = value.coerceIn(-12f, 12f) }) } }
    fun setEqualizerEnabled(value: Boolean) { _equalizer.update { it.copy(enabled = value) } }
    fun setAutoClose(minutes: Int, onClose: () -> Unit) { autoCloseJob?.cancel(); _autoCloseRemaining.value = minutes * 60_000L; autoCloseJob = scope.launch { while ((_autoCloseRemaining.value ?: 0) > 0) { delay(1000); _autoCloseRemaining.update { (it ?: 0) - 1000 } }; if (_autoCloseRemaining.value == 0L) onClose(); _autoCloseRemaining.value = null } }
    fun cancelAutoClose() { autoCloseJob?.cancel(); autoCloseJob = null; _autoCloseRemaining.value = null }
}
