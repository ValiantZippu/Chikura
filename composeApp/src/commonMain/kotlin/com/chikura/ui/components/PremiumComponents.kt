package com.chikura.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chikura.ui.theme.*

// Premium monochrome components — rounded 12/16/20, super smooth motion, no gradle compile check

private val PremiumEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
private val PremiumSpring = SpringSpec<Float>(dampingRatio = 0.84f, stiffness = 380f)

@Composable
fun PremiumPill(text: String, active: Boolean = false, onClick: (() -> Unit)? = null) {
    val scale by animateFloatAsState(if (active) 1.02f else 1f, animationSpec = PremiumSpring, label = "pillScale")
    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) ChikuraBlack else Color.White)
            .border(1.dp, if (active) ChikuraBlack else ChikuraDim, RoundedCornerShape(999.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, color = if (active) Color.White else ChikuraBlack)
    }
}

@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    rounded: RoundedCornerShape = RoundedCornerShape(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var hovered by remember { mutableStateOf(false) }
    val elev by animateDpAsState(if (hovered) 12.dp else 2.dp, tween(280, easing = PremiumEasing), label = "elev")
    val ty by animateDpAsState(if (hovered) (-2).dp else 0.dp, tween(280, easing = PremiumEasing), label = "ty")
    Column(
        modifier = modifier
            .clip(rounded)
            .background(Color.White)
            .border(1.dp, ChikuraBlack, rounded)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(12.dp),
        content = content
    )
}

@Composable
fun AnimatedDomainHeader(title: String, count: Int, expanded: Boolean, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(if (expanded) 90f else 0f, tween(220, easing = PremiumEasing), label = "rot")
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (expanded) ChikuraWhite else ChikuraLight).border(1.dp, ChikuraDim, RoundedCornerShape(12.dp)).clickable(onClick = onToggle).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(22.dp).clip(RoundedCornerShape(8.dp)).background(ChikuraBlack), contentAlignment = Alignment.Center) {
            Text(if (expanded) "▾" else "▸", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White)
        }
        Spacer(Modifier.width(10.dp))
        Text(title, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ChikuraBlack, modifier = Modifier.weight(1f))
        Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(ChikuraBlack).padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text("$count", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "shimmer")
    val x by infinite.animateFloat(initialValue = -1f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "shimmerX")
    Box(modifier = modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFF0F0F0)))
}

@Composable
fun PremiumBadge(text: String, mono: Boolean = true) {
    Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).border(1.dp, ChikuraBlack, RoundedCornerShape(999.dp)).background(Color.White).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(text, fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default, fontSize = 9.sp, color = ChikuraBlack, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GlassHeader(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xEBFFFFFF)).border(1.dp, ChikuraDim, RoundedCornerShape(16.dp)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

// Staggered premium list — super smooth entrance
@Composable
fun <T> StaggeredColumn(items: List<T>, delayStep: Int = 42, content: @Composable (T, Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { idx, item ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { kotlinx.coroutines.delay((idx * delayStep).toLong()); visible = true }
            AnimatedVisibility(visible = visible, enter = fadeIn(tween(320, easing = PremiumEasing)) + slideInVertically(tween(320, easing = PremiumEasing)) { it / 6 }, exit = fadeOut()) {
                content(item, idx)
            }
        }
    }
}

// Premium monochrome loader — rounded, pulsing
@Composable
fun ChikuraLoader(modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "loaderPulse")
    val scale by pulse.animateFloat(initialValue = 0.92f, targetValue = 1.06f, animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "scale")
    Box(modifier = modifier.size(44.dp).scale(scale).clip(RoundedCornerShape(12.dp)).background(ChikuraBlack), contentAlignment = Alignment.Center) {
        Text("蔵", fontFamily = FontFamily.Monospace, fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PremiumDivider(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(1.dp).background(ChikuraDim))
}
