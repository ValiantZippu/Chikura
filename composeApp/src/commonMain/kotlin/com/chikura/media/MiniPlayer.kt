package com.chikura.media

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BgBlack = Color(0xFF000000)
private val CardDark = Color(0xFF0A0A0A)
private val BorderDim = Color(0xFF1A1A1A)
private val TextMuted = Color(0xFF9A9A9A)
private val AccentGreen = Color(0xFF00D084)
private val White = Color.White
private val Black = Color.Black

/**
 * Discord-style mini player bar at bottom of screen.
 * Shows: prev, play/pause, next, title, author, play mode, queue button.
 * Pulls state from MediaQueue singleton.
 */
@Composable
fun MiniPlayer(
    isPlaying: Boolean,
    onStop: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onQueueOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val current by remember { derivedStateOf { MediaQueue.current } }
    val playMode by MediaQueue.playMode.collectAsState()

    AnimatedVisibility(
        visible = isPlaying && current != null,
        modifier = modifier,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(CardDark)
                .border(1.dp, BorderDim)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Previous
                Box(
                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF111111)).border(1.dp, BorderDim, RoundedCornerShape(999.dp))
                        .clickable(onClick = onPrevious),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⏮", fontSize = 12.sp, color = White)
                }

                // Play/Pause
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(999.dp))
                        .background(AccentGreen).clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isPlaying) "⏸" else "▶", fontSize = 14.sp, color = Black)
                }

                // Next
                Box(
                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF111111)).border(1.dp, BorderDim, RoundedCornerShape(999.dp))
                        .clickable(onClick = onNext),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⏭", fontSize = 12.sp, color = White)
                }

                // Stop
                Box(
                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF222222)).border(1.dp, BorderDim, RoundedCornerShape(999.dp))
                        .clickable(onClick = onStop),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⏹", fontSize = 10.sp, color = White)
                }

                // Title + author
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val title = current?.title?.take(40) ?: "No track"
                    val author = current?.author ?: ""
                    Text(title, fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                        color = White, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (author.isNotBlank()) {
                        Text(author, fontFamily = FontFamily.Monospace, fontSize = 8.sp,
                            color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                // Play mode indicator
                Box(
                    modifier = Modifier.size(24.dp).clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF111111)).border(1.dp, BorderDim, RoundedCornerShape(999.dp))
                        .clickable { MediaQueue.cyclePlayMode() },
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (playMode) {
                        PlayMode.NORMAL -> "→"
                        PlayMode.REPEAT_ALL -> "↻"
                        PlayMode.REPEAT_ONE -> "1"
                        PlayMode.SHUFFLE -> "⟳"
                    }
                    Text(icon, fontSize = 10.sp, color = if (playMode == PlayMode.NORMAL) TextMuted else AccentGreen)
                }

                // Queue button
                Box(
                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF111111)).border(1.dp, BorderDim, RoundedCornerShape(999.dp))
                        .clickable(onClick = onQueueOpen),
                    contentAlignment = Alignment.Center
                ) {
                    Text("☰", fontSize = 12.sp, color = White)
                }
            }
        }
    }
}
