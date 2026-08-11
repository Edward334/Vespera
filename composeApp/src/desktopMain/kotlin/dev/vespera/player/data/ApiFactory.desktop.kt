package dev.vespera.player.data

actual fun createMusicApi(): MusicApi {
    val url = System.getenv("VESPERA_API_URL")?.takeIf(String::isNotBlank) ?: return DemoMusicApi()
    val uid = System.getenv("VESPERA_USER_ID")?.toLongOrNull()
    val cookie = System.getenv("VESPERA_COOKIE")
    return NeteaseMusicApi(url, uid, cookie)
}
