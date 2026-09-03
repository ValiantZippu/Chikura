package com.chikura.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.sp

// Black & white terminal theme — #000 / #FFF only, JetBrains Mono, 1px border
private val ChikuraBlack = Color(0xFF000000)
private val ChikuraWhite = Color(0xFFFFFFFF)

/**
 * Root composable — blank by default per Task 1.
 * Shows monospace "Chikura — blank" centered on white bg with black 1px border.
 * Cross-platform: commonMain, delegated to androidMain/desktopMain/wasmJsMain+webMain.
 * Task 7: Web READ_ONLY mode — no Monaco, no drag, show "Edit in App" button, reads via raw.githubusercontent.com
 */
@Composable
fun App() {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = ChikuraWhite,
            surface = ChikuraWhite,
            onBackground = ChikuraBlack,
            onSurface = ChikuraBlack,
            primary = ChikuraBlack
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ChikuraWhite)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .border(1.dp, ChikuraBlack)
                        .background(ChikuraWhite)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Chikura — blank",
                            fontFamily = FontFamily.Monospace,
                            color = ChikuraBlack
                        )
                        Text(
                            text = "${getPlatformName()}${if (READ_ONLY) " • READ_ONLY" else ""}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF888888)
                        )
                    }
                }
                if (READ_ONLY) {
                    // Web: no Monaco, no drag, show "Edit in App" — reads via raw.githubusercontent.com / GitHub API
                    Box(
                        modifier = Modifier
                            .border(1.dp, ChikuraBlack)
                            .background(ChikuraWhite)
                            .clickable { /* chikura://open — handled by desktop app (Chikura 知蔵) */ }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Edit in App",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = ChikuraBlack
                        )
                    }
                    Text(
                        text = "Web is read-only (no Monaco, no drag) — fetches via raw.githubusercontent.com",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = Color(0xFF999999)
                    )
                }
            }
        }
    }
}
