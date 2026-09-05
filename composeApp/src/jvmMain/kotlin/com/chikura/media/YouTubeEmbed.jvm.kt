package com.chikura.media

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.awt.Desktop
import java.net.URI

// YouTube embed: shows thumbnail with play button that opens in system browser.
// Reliable fallback — no KCEF dependency.
@Composable
actual fun YouTubeEmbed(videoId: String, modifier: Modifier, incognito: Boolean, autoplay: Boolean, startSec: Int) {
    val thumbUrl = youtubeThumbUrl(videoId)
    val watch = youTubeWatchUrl(videoId)

    Box(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF080808))
            .border(1.dp, Color(0xFF222222), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Thumbnail background
        key(videoId) {
            VideoThumbnail(imageUrl = thumbUrl, modifier = Modifier.fillMaxSize())
        }
        // Dark overlay
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))

        // Play button + info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Play button
            Box(
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.95f))
                    .clickable {
                        try { Desktop.getDesktop().browse(URI(watch)) } catch (_: Exception) {}
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("▶", fontSize = 24.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
            Text(
                "Click to play on YouTube",
                fontFamily = FontFamily.Monospace, fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold
            )
        }
    }
}
