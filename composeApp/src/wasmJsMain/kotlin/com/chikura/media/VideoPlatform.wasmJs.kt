package com.chikura.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Desktop-first: wasm shows a placeholder until an <img>/fetch bridge lands.
@Composable
actual fun VideoThumbnail(imageUrl: String, modifier: Modifier, attempt: Int) {
    Box(modifier = modifier.background(Color(0xFF0A0A0A)), contentAlignment = Alignment.Center) {
        Text(">", fontFamily = FontFamily.Monospace, fontSize = 18.sp, color = Color(0xFF3A3A3A), fontWeight = FontWeight.Bold)
    }
}

actual suspend fun fetchVideoDetails(videoId: String): VideoDetails? = null

actual suspend fun fetchVideoComments(videoId: String): VideoComments = VideoComments(emptyList(), null)

actual suspend fun fetchVideoReplies(continuation: String): List<VideoComment> = emptyList()



actual fun openVideoUrl(url: String) {
    // No-op on wasm for now; desktop opens the browser.
}
