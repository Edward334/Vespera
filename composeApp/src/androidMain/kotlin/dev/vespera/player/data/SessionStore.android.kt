package dev.vespera.player.data

import android.content.Context

actual object SessionStore {
    private const val PREFS = "vespera_session"
    private const val COOKIE = "netease_cookie"
    private var context: Context? = null

    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    private fun preferences() = checkNotNull(context) { "SessionStore is not initialized" }
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    actual fun loadCookie(): String? = preferences().getString(COOKIE, null)
    actual fun saveCookie(cookie: String) { preferences().edit().putString(COOKIE, cookie).apply() }
    actual fun clear() { preferences().edit().remove(COOKIE).apply() }
}
