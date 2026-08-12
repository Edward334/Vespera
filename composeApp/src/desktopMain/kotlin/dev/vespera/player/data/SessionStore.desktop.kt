package dev.vespera.player.data

import java.util.prefs.Preferences

actual object SessionStore {
    private const val COOKIE = "netease_cookie"
    private val preferences = Preferences.userRoot().node("dev/vespera/player")

    actual fun loadCookie(): String? = preferences.get(COOKIE, null)
    actual fun saveCookie(cookie: String) { preferences.put(COOKIE, cookie) }
    actual fun clear() { preferences.remove(COOKIE) }
}
