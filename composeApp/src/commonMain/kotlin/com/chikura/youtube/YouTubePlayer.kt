package com.chikura.youtube

import androidx.compose.runtime.Composable
import com.chikura.media.MediaCard
import com.chikura.media.MediaMetadata

typealias YouTubeMeta = MediaMetadata

@Composable
fun YouTubePlayerCard(meta: MediaMetadata): Unit = MediaCard(meta)
