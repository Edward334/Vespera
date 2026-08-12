package dev.vespera.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.vespera.player.data.createMusicApi
import dev.vespera.player.ui.VesperaApp

@Composable
fun App() = VesperaApp(remember { createMusicApi() })
