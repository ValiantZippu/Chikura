package com.chikura.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

/**
 * Desktop entry point — JVM "desktop" target.
 * Black & white terminal window, JetBrains Mono via App.kt
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Chikura"
    ) {
        App()
    }
}
