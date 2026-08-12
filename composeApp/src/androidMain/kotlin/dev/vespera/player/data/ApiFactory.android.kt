package dev.vespera.player.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual fun createMusicApi(): MusicApi = NeteaseMusicApi(HttpClient(OkHttp))
