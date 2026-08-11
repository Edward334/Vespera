package dev.vespera.player

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application { Window(onCloseRequest = ::exitApplication, title = "Vespera") { App() } }
