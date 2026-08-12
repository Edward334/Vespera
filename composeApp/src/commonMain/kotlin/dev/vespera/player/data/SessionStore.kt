package dev.vespera.player.data

expect object SessionStore {
    fun loadCookie(): String?
    fun saveCookie(cookie: String)
    fun clear()
}
