package dev.vespera.player.data

import platform.Foundation.NSUserDefaults

actual object SessionStore {
    private const val COOKIE = "netease_cookie"
    private val defaults get() = NSUserDefaults.standardUserDefaults

    actual fun loadCookie(): String? = defaults.stringForKey(COOKIE)
    actual fun saveCookie(cookie: String) { defaults.setObject(cookie, COOKIE) }
    actual fun clear() { defaults.removeObjectForKey(COOKIE) }
}
