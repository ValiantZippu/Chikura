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

// ━━━ Chikura AMOLED Premium — True Black #000000 Terminal ━━━
// AMOLED black bg, white fg, JetBrains Mono, rounded 12/16/20, super smooth motion

val ChikuraBlack = Color(0xFF000000) // AMOLED true black
val ChikuraWhite = Color(0xFFFFFFFF)
val ChikuraGray = Color(0xFF9A9A9A)
val ChikuraDim = Color(0xFF1A1A1A)
val ChikuraBorder = Color(0xFF2A2A2A)
val ChikuraLight = Color(0xFF0A0A0A)
val ChikuraCard = Color(0xFF111111)
val ChikuraHover = Color(0xFF1E1E1E)
val ChikuraMuted = Color(0xFF6B6B6B)
val ChikuraAccent = Color(0xFFFFFFFF)
val ChikuraOverlay = Color(0x99000000)

// AMOLED elevated surfaces — subtle grays on black
val ChikuraSurface1 = Color(0xFF0F0F0F)
val ChikuraSurface2 = Color(0xFF141414)
val ChikuraSurface3 = Color(0xFF1C1C1C)
val ChikuraSurface4 = Color(0xFF222222)

// Rounded — premium soft AMOLED
val ChikuraRadiusSmall = RoundedCornerShape(8.dp)
val ChikuraRadiusMedium = RoundedCornerShape(12.dp)
val ChikuraRadiusLarge = RoundedCornerShape(16.dp)
val ChikuraRadiusXLarge = RoundedCornerShape(20.dp)
val ChikuraRadius2XLarge = RoundedCornerShape(24.dp)
val ChikuraRadiusPill = RoundedCornerShape(50)

// Motion — super smooth AMOLED
val ChikuraEase = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
val ChikuraEaseOut = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
val ChikuraEaseInOut = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
val ChikuraSpring = Spring.DampingRatioMediumBouncy
val ChikuraSpringNoBounce = Spring.DampingRatioNoBouncy

fun chikuraTween(duration: Int = 280) = tween<Float>(duration, easing = ChikuraEase)
fun chikuraSpring() = spring<Float>(dampingRatio = 0.82f, stiffness = 420f)
fun <T> chikuraAnimateSpec() = tween<T>(320, easing = ChikuraEase)

val ChikuraTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = ChikuraWhite, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ChikuraWhite),
    titleMedium = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ChikuraWhite),
    bodyLarge = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = ChikuraWhite),
    bodyMedium = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = ChikuraWhite),
    bodySmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ChikuraGray),
    labelLarge = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ChikuraWhite, letterSpacing = 0.8.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraMuted)
)

@Composable
fun ChikuraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = ChikuraBlack,
            surface = ChikuraBlack,
            surfaceVariant = ChikuraSurface1,
            onBackground = ChikuraWhite,
            onSurface = ChikuraWhite,
            primary = ChikuraWhite,
            onPrimary = ChikuraBlack,
            outline = ChikuraBorder,
            outlineVariant = ChikuraDim
        ),
        typography = ChikuraTypography,
        content = content
    )
}

@Composable
fun rememberChikuraPulse(): Float {
    val infinite = rememberInfiniteTransition(label = "chikura-pulse")
    val pulse by infinite.animateFloat(initialValue = 0.6f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(900, easing = ChikuraEaseInOut), repeatMode = RepeatMode.Reverse), label = "pulse")
    return pulse
}

@Composable
fun rememberChikuraShimmer(): Float {
    val infinite = rememberInfiniteTransition(label = "shimmer")
    val shimmer by infinite.animateFloat(initialValue = -1f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "shimmer")
    return shimmer
}

// AMOLED glow — subtle white glow on black
val ChikuraGlow = Color(0x14FFFFFF)
val ChikuraGlowStrong = Color(0x22FFFFFF)
