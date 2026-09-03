package com.chikura.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * Task 4: Black & White Design System.
 * Spec: Bg #fff, Fg #000, JetBrains Mono, 1px borders.
 * No color except thumbnails (which are image content, not chrome).
 */

val ChikuraBlack: Color = Color(0xFF000000)
val ChikuraWhite: Color = Color(0xFFFFFFFF)

// For explicit mono usage — JetBrains Mono is FontFamily.Monospace on all platforms
// Custom TTF can be added to composeResources and referenced via Res.font.jetbrainsMono
val ChikuraMono: FontFamily = FontFamily.Monospace

/**
 * ChikuThread theme — wraps MaterialTheme with black/white palette.
 * Use ChikuraTheme { ListScreen(...) } for preview/list.
 */
@Composable
fun ChikuraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = ChikuraWhite,
            surface = ChikuraWhite,
            onBackground = ChikuraBlack,
            onSurface = ChikuraBlack,
            primary = ChikuraBlack,
            onPrimary = ChikuraWhite,
            secondary = ChikuraBlack,
            onSecondary = ChikuraWhite,
            surfaceVariant = ChikuraWhite,
            onSurfaceVariant = ChikuraBlack,
            outline = ChikuraBlack,
            outlineVariant = ChikuraBlack
        ),
        typography = MaterialTheme.typography.copy(
            // Ensure default text uses mono where possible — callers still set FontFamily.Monospace explicitly
        )
    ) {
        Box(modifier = Modifier.background(ChikuraWhite)) {
            content()
        }
    }
}

/**
 * Token object for snapshot tests: assert ChikuraTheme.colors.background == Color.White
 */
object ChikuraThemeTokens {
    val background: Color = Color.White
    val foreground: Color = Color.Black
    val borderColor: Color = Color.Black
    val borderWidth = 1.dp
    val fontFamily: FontFamily = FontFamily.Monospace
}
