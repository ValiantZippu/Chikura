package com.knowledgebunker.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

// Black & white terminal theme — #000 / #FFF only, JetBrains Mono, 1px border
private val BunkerBlack = Color(0xFF000000)
private val BunkerWhite = Color(0xFFFFFFFF)

/**
 * Root composable — blank by default per Task 1.
 * Shows monospace "KnowledgeBunker — blank" centered on white bg with black 1px border.
 * Cross-platform: commonMain, delegated to androidMain/desktopMain/wasmJsMain+webMain.
 */
@Composable
fun App() {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = BunkerWhite,
            surface = BunkerWhite,
            onBackground = BunkerBlack,
            onSurface = BunkerBlack,
            primary = BunkerBlack
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BunkerWhite)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .border(1.dp, BunkerBlack)
                    .background(BunkerWhite)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "KnowledgeBunker — blank",
                    fontFamily = FontFamily.Monospace,
                    color = BunkerBlack
                )
            }
        }
    }
}
