package dev.vespera.player.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual fun createMusicApi(): MusicApi = NeteaseMusicApi(HttpClient(CIO))
