package com.chikura.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ━━━ Chikura Monochrome Premium Design System ━━━
// Black & white only, JetBrains Mono, rounded corners, super smooth motion

val ChikuraBlack = Color(0xFF0A0A0A)
val ChikuraWhite = Color(0xFFFFFFFF)
val ChikuraGray = Color(0xFF6B6B6B)
val ChikuraDim = Color(0xFFE8E8E8)
val ChikuraLight = Color(0xFFF8F8F7)
val ChikuraHover = Color(0xFFF0F0F0)
val ChikuraBorder = Color(0xFFE0E0E0)
val ChikuraMuted = Color(0xFF9A9A9A)
val ChikuraCard = Color(0xFFFAFAFA)
val ChikuraOverlay = Color(0x0A000000)

// Rounded — premium soft
val ChikuraRadiusSmall = RoundedCornerShape(8.dp)
val ChikuraRadiusMedium = RoundedCornerShape(12.dp)
val ChikuraRadiusLarge = RoundedCornerShape(16.dp)
val ChikuraRadiusXLarge = RoundedCornerShape(20.dp)
val ChikuraRadiusPill = RoundedCornerShape(50)

// Motion — super smooth
val ChikuraEase = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
val ChikuraEaseOut = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
val ChikuraEaseInOut = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
val ChikuraSpring = Spring.DampingRatioMediumBouncy
val ChikuraSpringNoBounce = Spring.DampingRatioNoBouncy

fun chikuraTween(duration: Int = 280) = tween<Float>(duration, easing = ChikuraEase)
fun chikuraSpring() = spring<Float>(dampingRatio = 0.82f, stiffness = 420f)
fun <T> chikuraAnimateSpec() = tween<T>(320, easing = ChikuraEase)

val ChikuraTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = ChikuraBlack, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ChikuraBlack),
    titleMedium = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ChikuraBlack),
    bodyLarge = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = ChikuraBlack),
    bodyMedium = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = ChikuraBlack),
    bodySmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ChikuraGray),
    labelLarge = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ChikuraBlack, letterSpacing = 0.8.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraMuted)
)

@Composable
fun ChikuraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = ChikuraWhite,
            surface = ChikuraWhite,
            surfaceVariant = ChikuraLight,
            onBackground = ChikuraBlack,
            onSurface = ChikuraBlack,
            primary = ChikuraBlack,
            onPrimary = ChikuraWhite,
            outline = ChikuraDim
        ),
        typography = ChikuraTypography,
        content = content
    )
}

// Smooth shimmer / pulse for loading states
@Composable
fun rememberChikuraPulse(): Float {
    val infinite = rememberInfiniteTransition(label = "chikura-pulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(900, easing = ChikuraEaseInOut), repeatMode = RepeatMode.Reverse),
        label = "pulse"
    )
    return pulse
}

@Composable
fun rememberChikuraShimmer(): Float {
    val infinite = rememberInfiniteTransition(label = "shimmer")
    val shimmer by infinite.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "shimmer"
    )
    return shimmer
}
