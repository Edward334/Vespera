@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.vespera.player.player

import platform.AVFoundation.*
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.CoreMedia.CMTimeGetSeconds
import platform.Foundation.NSURL

actual class PlatformAudioEngine actual constructor() : AudioEngine {
    private var player: AVPlayer? = null
    override fun load(url: String) {
        val mediaUrl = NSURL.URLWithString(url) ?: return
        player?.pause()
        player = AVPlayer(uRL = mediaUrl)
    }
    override fun play() { player?.play() }
    override fun pause() { player?.pause() }
    override fun seek(positionMs: Long) { player?.seekToTime(CMTimeMakeWithSeconds(positionMs.coerceAtLeast(0).toDouble() / 1000.0, 600)) }
    override fun setVolume(volume: Float) { player?.volume = volume }
    override fun setRate(rate: Float) { player?.rate = rate }
    override fun positionMs(): Long = player?.currentTime()?.let { (CMTimeGetSeconds(it) * 1000).toLong() } ?: 0
    override fun release() { player?.pause(); player = null }
}
