package dev.vespera.player.player

import android.media.MediaPlayer

actual class PlatformAudioEngine actual constructor() : AudioEngine {
    private var player: MediaPlayer? = null
    override fun load(url: String) { release(); player = MediaPlayer().apply { setDataSource(url); prepareAsync() } }
    override fun play() { player?.setOnPreparedListener { it.start() }; player?.takeIf { it.isPlaying.not() }?.runCatching { start() } }
    override fun pause() { player?.takeIf { it.isPlaying }?.pause() }
    override fun seek(positionMs: Long) { player?.seekTo(positionMs.toInt()) }
    override fun setVolume(volume: Float) { player?.setVolume(volume, volume) }
    override fun setRate(rate: Float) { player?.runCatching { playbackParams = playbackParams.setSpeed(rate) } }
    override fun positionMs(): Long = player?.runCatching { currentPosition.toLong() }?.getOrDefault(0) ?: 0
    override fun release() { player?.release(); player = null }
}
