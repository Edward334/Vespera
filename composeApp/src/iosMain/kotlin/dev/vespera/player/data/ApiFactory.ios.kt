package dev.vespera.player.data

import platform.Foundation.NSProcessInfo

actual fun createMusicApi(): MusicApi {
    val environment = NSProcessInfo.processInfo.environment
    val url = environment["VESPERA_API_URL"] as? String ?: return DemoMusicApi()
    val uid = (environment["VESPERA_USER_ID"] as? String)?.toLongOrNull()
    val cookie = environment["VESPERA_COOKIE"] as? String
    return NeteaseMusicApi(url, uid, cookie)
}
