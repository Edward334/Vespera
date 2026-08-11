package dev.vespera.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.vespera.player.download.initializeFileStore
import dev.vespera.player.data.initializeLocalMusicScanner

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); initializeFileStore(this); initializeLocalMusicScanner(this); setContent { App() } }
}
