package com.chikura.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp

// Professional monochrome motion — super smooth, spring + tween, rounded 12/16/20
object ChikuraMotion {
    val Fast = tween<Float>(180, easing = FastOutSlowInEasing)
    val Smooth = tween<Float>(320, easing = FastOutSlowInEasing)
    val Spring = spring<Float>(dampingRatio = 0.86f, stiffness = 420f)
    val BouncySpring = spring<Float>(dampingRatio = 0.68f, stiffness = 380f)
    val SlowSpring = spring<Float>(dampingRatio = 0.9f, stiffness = 280f)
    val Snap = tween<Float>(120, easing = LinearOutSlowInEasing)
    
    val CornerS = RoundedCornerShape(8.dp)
    val CornerM = RoundedCornerShape(12.dp)
    val CornerL = RoundedCornerShape(16.dp)
    val CornerXL = RoundedCornerShape(20.dp)
    val Corner2XL = RoundedCornerShape(24.dp)
    val CornerFull = RoundedCornerShape(999.dp)
    
    val Elevation1 = 2.dp
    val Elevation2 = 8.dp
    val Elevation3 = 16.dp
    val Elevation4 = 24.dp
}

@Composable
fun rememberChikuraInfinitePulse(): Float {
    val infinite = rememberInfiniteTransition(label = "pulse")
    val v by infinite.animateFloat(initialValue = 0.96f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    return v
}

@Composable
fun rememberChikuraHoverScale(hovered: Boolean): Float {
    val anim by animateFloatAsState(targetValue = if (hovered) 1.02f else 1f, animationSpec = ChikuraMotion.Spring, label = "hoverScale")
    return anim
}

@Composable
fun rememberChikuraPressScale(pressed: Boolean): Float {
    val anim by animateFloatAsState(targetValue = if (pressed) 0.98f else 1f, animationSpec = tween(100), label = "pressScale")
    return anim
}

// Staggered entrance — 0..1 fraction, delay per index
fun staggeredDelay(index: Int, base: Int = 45): Int = (index * base).coerceAtMost(420)
fun staggeredEasing(index: Int) = if (index % 2 == 0) FastOutSlowInEasing else LinearOutSlowInEasing

// Monochrome palette — pure black/white with subtle grays for depth, no color except thumbs
object ChikuraMono {
    val White = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
    val OffWhite = androidx.compose.ui.graphics.Color(0xFFFAFAFA)
    val Ghost = androidx.compose.ui.graphics.Color(0xFFF5F5F5)
    val Mist = androidx.compose.ui.graphics.Color(0xFFEEEEEE)
    val Fog = androidx.compose.ui.graphics.Color(0xFFE0E0E0)
    val Ash = androidx.compose.ui.graphics.Color(0xFFBDBDBD)
    val Smoke = androidx.compose.ui.graphics.Color(0xFF9E9E9E)
    val Stone = androidx.compose.ui.graphics.Color(0xFF6B6B6B)
    val Ink = androidx.compose.ui.graphics.Color(0xFF111111)
    val Black = androidx.compose.ui.graphics.Color(0xFF000000)
    // Glass — super smooth blur layers
    val GlassWhite = androidx.compose.ui.graphics.Color(0xEBFFFFFF)
    val GlassBlack = androidx.compose.ui.graphics.Color(0xCC000000)
}

// Smooth shimmer for skeleton — monochrome
@Composable
fun rememberShimmer(): Float {
    val t = rememberInfiniteTransition(label = "shimmer")
    val v by t.animateFloat(initialValue = -1f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), label = "shimmer")
    return v
}
