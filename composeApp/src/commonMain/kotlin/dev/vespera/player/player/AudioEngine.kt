package dev.vespera.player.player

interface AudioEngine {
    fun load(url: String)
    fun play()
    fun pause()
    fun seek(positionMs: Long)
    fun setVolume(volume: Float)
    fun setRate(rate: Float)
    fun positionMs(): Long
    fun release()
}

expect class PlatformAudioEngine() : AudioEngine
