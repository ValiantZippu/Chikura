package com.chikura.app

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.chikura.media.PlayerEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Desktop entry point — JVM "desktop" target.
 * Native decorated window (snap + maximize intact) with dark titlebar forced
 * via DWM and the painted Vault C icon. Black & white terminal content,
 * JetBrains Mono via App.kt. Player engine (Chromium) boots in background.
 */
fun main() = application {
    val state = rememberWindowState(size = DpSize(1440.dp, 900.dp))
    val icon = rememberChikuraWindowIcon()
    Window(
        onCloseRequest = ::exitApplication,
        state = state,
        title = "Chikura",
        icon = icon
    ) {
        ApplyWindowsDarkChrome(window)
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) { PlayerEngine.ensureStarted() }
        }
        DisposableEffect(Unit) {
            onDispose { PlayerEngine.dispose() }
        }
        App()
    }
}
// Window background is set to AMOLED black via App.kt root Box fillMaxSize + ChikuraTheme; native window white artifact fixed by ChikuraTheme background = #000000 and root Box
