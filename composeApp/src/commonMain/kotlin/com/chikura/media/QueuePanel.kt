package com.chikura.media

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
private val BorderLight = Color(0xFF222222)
private val TextMuted = Color(0xFF9A9A9A)
private val AccentGreen = Color(0xFF00D084)
private val White = Color.White
private val Black = Color.Black

/**
 * Slide-out queue panel from the right side.
 * Shows all queued videos, play modes, reorder, and remove.
 */
@Composable
fun QueuePanel(
    isVisible: Boolean,
    onClose: () -> Unit,
    onPlayItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items by MediaQueue.items.collectAsState()
    val currentIndex by MediaQueue.currentIndex.collectAsState()
    val playMode by MediaQueue.playMode.collectAsState()

    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Black.copy(alpha = 0.6f)).clickable { onClose() },
            contentAlignment = Alignment.CenterEnd
        ) {
            Column(
                modifier = Modifier.fillMaxHeight().width(380.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                    .background(CardDark).border(1.dp, BorderDim, RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                    .clickable(enabled = false) {}
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("QUEUE", fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                            color = White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("${items.size} tracks", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextMuted)
                    }
                    Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(White).clickable { onClose() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("CLOSE", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Black, fontWeight = FontWeight.Bold)
                    }
                }

                // Play mode selector
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF111111))
                        .border(1.dp, BorderDim, RoundedCornerShape(999.dp)).padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    listOf(
                        PlayMode.NORMAL to "NORMAL",
                        PlayMode.REPEAT_ALL to "REPEAT",
                        PlayMode.REPEAT_ONE to "ONE",
                        PlayMode.SHUFFLE to "SHUFFLE"
                    ).forEach { (mode, label) ->
                        val active = playMode == mode
                        val bg = if (active) White else Color.Transparent
                        val fg = if (active) Black else TextMuted
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(999.dp)).background(bg)
                                .clickable { MediaQueue.setPlayMode(mode) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = fg,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                // Now playing
                val current = MediaQueue.current
                if (current != null) {
                    Column(
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF111111))
                            .border(1.dp, AccentGreen, RoundedCornerShape(12.dp)).padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("NOW PLAYING", fontFamily = FontFamily.Monospace, fontSize = 8.sp,
                            color = AccentGreen, fontWeight = FontWeight.Bold)
                        Text(current.title.take(50), fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                            color = White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(current.author, fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = TextMuted)
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderDim))

                // Queue items
                if (items.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("♪", fontSize = 32.sp, color = BorderDim)
                            Text("Queue is empty", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextMuted)
                            Text("Click play on any video to start", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = BorderLight)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Black)
                            .border(1.dp, BorderDim, RoundedCornerShape(12.dp)).padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(items, key = { _, item -> item.videoId }) { index, item ->
                            val isCurrent = index == currentIndex
                            val itemBg = if (isCurrent) Color(0xFF111111) else Color.Transparent
                            val itemBorder = if (isCurrent) AccentGreen else Color.Transparent

                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                    .background(itemBg).border(0.5.dp, itemBorder, RoundedCornerShape(8.dp))
                                    .clickable { onPlayItem(item.videoId) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Index / now playing indicator
                                if (isCurrent) {
                                    Text("▶", fontSize = 10.sp, color = AccentGreen)
                                } else {
                                    Text("${index + 1}", fontFamily = FontFamily.Monospace, fontSize = 8.sp,
                                        color = TextMuted, modifier = Modifier.width(16.dp))
                                }

                                // Title
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(item.title.take(40), fontFamily = FontFamily.Monospace, fontSize = 9.sp,
                                        color = if (isCurrent) AccentGreen else White, fontWeight = FontWeight.Bold,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(item.author, fontFamily = FontFamily.Monospace, fontSize = 7.sp,
                                        color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }

                                // Remove button
                                Box(
                                    modifier = Modifier.size(20.dp).clip(RoundedCornerShape(999.dp))
                                        .background(Color(0xFF1A1A1A)).border(0.5.dp, BorderDim, RoundedCornerShape(999.dp))
                                        .clickable { MediaQueue.removeAt(index) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("×", fontSize = 10.sp, color = TextMuted)
                                }
                            }
                        }
                    }
                }

                // Footer actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF111111)).border(1.dp, BorderDim, RoundedCornerShape(999.dp))
                            .clickable { MediaQueue.cyclePlayMode() }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val modeLabel = when (playMode) {
                            PlayMode.NORMAL -> "→ NORMAL"
                            PlayMode.REPEAT_ALL -> "↻ REPEAT"
                            PlayMode.REPEAT_ONE -> "↻ ONE"
                            PlayMode.SHUFFLE -> "⟳ SHUFFLE"
                        }
                        Text(modeLabel, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = White, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF1A1A1A)).border(1.dp, BorderDim, RoundedCornerShape(999.dp))
                            .clickable { MediaQueue.clear() }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("CLEAR ALL", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
