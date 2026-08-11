package dev.vespera.player.player

import javafx.application.Platform
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration

actual class PlatformAudioEngine actual constructor() : AudioEngine {
    private var player: MediaPlayer? = null
    init { runCatching { Platform.startup {} } }
    override fun load(url: String) { Platform.runLater { player?.dispose(); player = MediaPlayer(Media(url)) } }
    override fun play() { Platform.runLater { player?.play() } }
    override fun pause() { Platform.runLater { player?.pause() } }
    override fun seek(positionMs: Long) { Platform.runLater { player?.seek(Duration.millis(positionMs.toDouble())) } }
    override fun setVolume(volume: Float) { Platform.runLater { player?.volume = volume.toDouble() } }
    override fun setRate(rate: Float) { Platform.runLater { player?.rate = rate.toDouble() } }
    override fun positionMs(): Long = player?.currentTime?.toMillis()?.toLong() ?: 0
    override fun release() { Platform.runLater { player?.dispose(); player = null } }
}
