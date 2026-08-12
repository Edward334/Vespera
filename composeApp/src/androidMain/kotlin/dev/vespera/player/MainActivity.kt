package dev.vespera.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.vespera.player.download.initializeFileStore
import dev.vespera.player.data.initializeLocalMusicScanner
import dev.vespera.player.data.SessionStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionStore.initialize(applicationContext)
        initializeFileStore(this)
        initializeLocalMusicScanner(this)
        setContent { App() }
    }
}
