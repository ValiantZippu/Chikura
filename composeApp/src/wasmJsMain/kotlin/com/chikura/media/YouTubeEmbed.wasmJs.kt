package com.chikura.media

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
actual fun YouTubeEmbed(videoId: String, modifier: Modifier, incognito: Boolean, autoplay: Boolean, startSec: Int) {
    val url = youTubeEmbedUrl(videoId, incognito, autoplay, startSec)
    Box(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(Color.Black).border(1.dp, Color(0xFF222222), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(12.dp)) {
            Text("INLINE IFRAME", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Text(url, fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = Color(0xFF9A9A9A))
            Text(if (incognito) "INCOGNITO" else "SIGNED IN", fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = if (incognito) Color(0xFF00D084) else Color(0xFF4A90E2))
        }
    }
}
