package com.chikura.media

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class MediaMetadata(
    val title: String,
    val channel: String,
    val duration: String,
    val thumbnailUrl: String,
    val views: String
)

private val cardShape = RoundedCornerShape(16.dp)
private val cardModifier = Modifier.clip(cardShape)
    .background(Color(0xFF111111))
    .border(1.dp, Color(0xFF1A1A1A), cardShape)
    .padding(10.dp)

@Composable
fun MediaCard(metadata: MediaMetadata): Unit {
    Column(modifier = cardModifier) {
        Text(metadata.title, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White)
        Text(metadata.channel + " · " + metadata.duration, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color(0xFF9A9A9A))
    }
}

@Composable
fun VideoPlayerCard(metadata: MediaMetadata): Unit = MediaCard(metadata)
