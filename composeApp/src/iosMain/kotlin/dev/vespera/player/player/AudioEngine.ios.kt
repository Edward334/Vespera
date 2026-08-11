package dev.vespera.player.player

import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeGetSeconds
import platform.Foundation.NSURL

actual class PlatformAudioEngine actual constructor() : AudioEngine {
    private val player = AVPlayer()
    override fun load(url: String) { NSURL.URLWithString(url)?.let { player.replaceCurrentItemWithPlayerItem(AVPlayerItem.playerItemWithURL(it)) } }
    override fun play() { player.play() }
    override fun pause() { player.pause() }
    override fun seek(positionMs: Long) { player.seekToTime(CMTimeMake(positionMs, 1000)) }
    override fun setVolume(volume: Float) { player.volume = volume }
    override fun setRate(rate: Float) { player.rate = rate }
    override fun positionMs(): Long = (CMTimeGetSeconds(player.currentTime()) * 1000).toLong()
    override fun release() { player.pause(); player.replaceCurrentItemWithPlayerItem(null) }
}
