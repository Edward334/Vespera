package dev.vespera.player.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun createMusicApi(): MusicApi = NeteaseMusicApi(HttpClient(Darwin))
