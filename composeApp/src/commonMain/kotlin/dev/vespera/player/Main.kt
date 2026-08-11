package dev.vespera.player

import androidx.compose.runtime.Composable
import dev.vespera.player.ui.VesperaApp
import dev.vespera.player.data.createMusicApi

@Composable fun App() = VesperaApp(createMusicApi())
