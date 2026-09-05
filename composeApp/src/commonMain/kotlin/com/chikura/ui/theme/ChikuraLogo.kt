package com.chikura.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Chikura mark — "Vault C".
// A rounded-square vault (the bunker) holding a thick open C (door ajar to the
// right = inbox intake, knowledge flows in) around a solid core dot (the one
// good curated link). Monochrome, geometric, legible 16px -> 512px.
// Deliberately not a kanji glyph: kanji fails at small sizes and across locales.

private val LogoWhite = Color.White
private val LogoBlack = Color.Black
private val LogoGray = Color(0xFF9A9A9A)
private val LogoDim = Color(0xFF1A1A1A)
private val LogoSurface = Color(0xFF111111)

@Composable
fun ChikuraMark(
    boxSize: Dp = 32.dp,
    inverted: Boolean = false,
    withBorder: Boolean = true,
    modifier: Modifier = Modifier
) {
    val bg = if (inverted) LogoBlack else LogoWhite
    val fg = if (inverted) LogoWhite else LogoBlack
    val corner = if (boxSize < 28.dp) 8.dp else 12.dp
    Box(
        modifier = modifier.size(boxSize).clip(RoundedCornerShape(corner)).background(bg)
            .let { if (withBorder) it.border(1.dp, if (inverted) Color(0xFF2A2A2A) else bg, RoundedCornerShape(corner)) else it },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(boxSize * 0.27f)) {
            // C ring leaves an 80deg opening on the right (the intake);
            // the dot is the curated core the vault protects.
            val side = minOf(size.width, size.height)
            val stroke = side * 0.155f
            drawArc(
                color = fg,
                startAngle = 40f,
                sweepAngle = 280f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(stroke / 2f, stroke / 2f),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawCircle(color = fg, radius = side * 0.08f, center = center)
        }
    }
}

@Composable
fun ChikuraWordmark(showJp: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("CHIKURA", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LogoWhite, letterSpacing = 0.8.sp)
        if (showJp) Text("知蔵", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = LogoGray)
    }
}

@Composable
fun ChikuraLogoHorizontal(markSize: Dp = 26.dp, showJp: Boolean = true, invertedMark: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ChikuraMark(boxSize = markSize, inverted = invertedMark)
        ChikuraWordmark(showJp = showJp)
    }
}

@Composable
fun ChikuraLogoBunker(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF0A0A0A)).border(1.dp, LogoDim, RoundedCornerShape(20.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ChikuraMark(boxSize = 52.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("CHIKURA", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LogoWhite, letterSpacing = 1.sp)
            Text("knowledge vault", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = LogoGray)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(LogoSurface).border(1.dp, LogoDim, RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("BUNKER", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = LogoGray, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(LogoWhite).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("AMOLED", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = LogoBlack, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ChikuraLogoSmall(modifier: Modifier = Modifier) {
    ChikuraMark(boxSize = 32.dp, modifier = modifier)
}
