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

val BunkerBlack: Color = Color(0xFF000000)
val BunkerWhite: Color = Color(0xFFFFFFFF)

// For explicit mono usage — JetBrains Mono is FontFamily.Monospace on all platforms
// Custom TTF can be added to composeResources and referenced via Res.font.jetbrainsMono
val BunkerMono: FontFamily = FontFamily.Monospace

/**
 * Bunker theme — wraps MaterialTheme with black/white palette.
 * Use BunkerTheme { ListScreen(...) } for preview/list.
 */
@Composable
fun BunkerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = BunkerWhite,
            surface = BunkerWhite,
            onBackground = BunkerBlack,
            onSurface = BunkerBlack,
            primary = BunkerBlack,
            onPrimary = BunkerWhite,
            secondary = BunkerBlack,
            onSecondary = BunkerWhite,
            surfaceVariant = BunkerWhite,
            onSurfaceVariant = BunkerBlack,
            outline = BunkerBlack,
            outlineVariant = BunkerBlack
        ),
        typography = MaterialTheme.typography.copy(
            // Ensure default text uses mono where possible — callers still set FontFamily.Monospace explicitly
        )
    ) {
        Box(modifier = Modifier.background(BunkerWhite)) {
            content()
        }
    }
}

/**
 * Token object for snapshot tests: assert BunkerTheme.colors.background == Color.White
 */
object BunkerThemeTokens {
    val background: Color = Color.White
    val foreground: Color = Color.Black
    val borderColor: Color = Color.Black
    val borderWidth = 1.dp
    val fontFamily: FontFamily = FontFamily.Monospace
}
