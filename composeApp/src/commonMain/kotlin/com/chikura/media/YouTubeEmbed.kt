package com.chikura.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class PlaybackMode { INCOGNITO, SIGNED_IN }

@Composable
expect fun YouTubeEmbed(videoId: String, modifier: Modifier = Modifier, incognito: Boolean = true, autoplay: Boolean = false, startSec: Int = 0)

fun youTubeEmbedUrl(videoId: String, incognito: Boolean, autoplay: Boolean = false, startSec: Int = 0): String {
    val domain = if (incognito) "https://www.youtube-nocookie.com" else "https://www.youtube.com"
    val auto = if (autoplay) "1" else "0"
    val start = if (startSec > 0) "&start=$startSec" else ""
    return "$domain/embed/$videoId?autoplay=$auto&modestbranding=1&rel=0&playsinline=1$start"
}

fun youTubeWatchUrl(videoId: String): String = "https://www.youtube.com/watch?v=$videoId"
