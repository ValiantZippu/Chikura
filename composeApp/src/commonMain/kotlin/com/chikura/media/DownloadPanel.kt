package com.chikura.media

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// AMOLED colors
private val BgBlack = Color(0xFF000000)
private val CardDark = Color(0xFF111111)
private val BorderDim = Color(0xFF1A1A1A)
private val TextMuted = Color(0xFF9A9A9A)
private val AccentGreen = Color(0xFF00D084)
private val White = Color.White
private val Black = Color.Black

/**
 * Full download panel: paste URL, pick format, see progress.
 * AMOLED dark theme matching the rest of the app.
 * Platform-specific fetch logic is delegated to platformFetchDownloadInfo().
 */
@Composable
fun DownloadPanel(
    isVisible: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var urlInput by remember { mutableStateOf("") }
    var downloadInfo by remember { mutableStateOf<DownloadInfo?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var formatTab by remember { mutableStateOf("video") } // "video" | "audio"
    var selectedFormat by remember { mutableStateOf<DownloadFormat?>(null) }

    // Listen to download manager state
    val downloads by DownloadManager.items.collectAsState()

    // Periodic refresh for progress
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(isVisible) {
        while (isVisible) {
            delay(500)
            tick++
        }
    }

    val scope = rememberCoroutineScope()

    AnimatedVisibility(visible = isVisible, enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })) {
        Box(modifier = modifier.fillMaxSize().background(Black.copy(alpha = 0.7f)).clickable { onClose() },
            contentAlignment = Alignment.CenterEnd) {
            Column(
                modifier = Modifier.fillMaxHeight().width(440.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                    .background(Color(0xFF0A0A0A)).border(1.dp, BorderDim, RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                    .clickable(enabled = false) {}.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("DOWNLOAD", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("YT → MP4 / MP3", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextMuted)
                    }
                    Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(White).clickable { onClose() }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("CLOSE X", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Black, fontWeight = FontWeight.Bold)
                    }
                }

                // URL input
                Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(CardDark).border(1.dp, BorderDim, RoundedCornerShape(16.dp)).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("YouTube URL", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = White, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Black).border(1.dp, BorderDim, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 8.dp)) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it; errorMessage = null },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = White),
                            singleLine = true,
                            decorationBox = { inner ->
                                if (urlInput.isEmpty()) Text("https://youtube.com/watch?v=...", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color(0xFF3A3A3A))
                                inner()
                            }
                        )
                    }
                    val fetchBg by animateColorAsState(if (isLoading) CardDark else White, tween(200))
                    val fetchFg by animateColorAsState(if (isLoading) TextMuted else Black, tween(200))
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(999.dp)).background(fetchBg).border(1.dp, if (isLoading) BorderDim else White, RoundedCornerShape(999.dp))
                        .clickable(enabled = !isLoading) {
                            val vid = extractVideoId(urlInput)
                            if (vid != null) {
                                isLoading = true
                                errorMessage = null
                                downloadInfo = null
                                selectedFormat = null
                                scope.launch {
                                    try {
                                        val info = platformFetchDownloadInfo(vid)
                                        downloadInfo = info
                                        if (info != null && info.videoFormats.isNotEmpty()) {
                                            selectedFormat = info.videoFormats.first()
                                            formatTab = "video"
                                        } else {
                                            errorMessage = "Could not fetch video info"
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = e.message ?: "Failed to fetch"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            } else {
                                errorMessage = "Invalid YouTube URL"
                            }
                        }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(if (isLoading) "FETCHING FORMATS..." else "FETCH FORMATS", fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp, color = fetchFg, fontWeight = FontWeight.Bold)
                    }
                }

                // Error
                if (errorMessage != null) {
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF2A0A0A)).border(1.dp, Color(0xFF5A2222), RoundedCornerShape(10.dp)).padding(10.dp)) {
                        Text(errorMessage!!, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color(0xFFFF6B6B))
                    }
                }

                // Format picker
                val info = downloadInfo
                if (info != null) {
                    // Video info header
                    Column(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(CardDark).border(1.dp, BorderDim, RoundedCornerShape(12.dp)).padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(info.title.take(60), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = White, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        val durMin = info.durationSec / 60
                        val durSec = info.durationSec % 60
                        Text("${durMin}m ${durSec}s · ${info.videoFormats.size} video · ${info.audioFormats.size} audio formats",
                            fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = TextMuted)
                    }

                    // Tab: video / audio
                    Row(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Black).border(1.dp, BorderDim, RoundedCornerShape(999.dp)).padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        listOf("video" to "VIDEO (MP4)", "audio" to "AUDIO (MP3)").forEach { (tab, label) ->
                            val active = formatTab == tab
                            val bg by animateColorAsState(if (active) White else Color.Transparent, tween(200))
                            val fg by animateColorAsState(if (active) Black else TextMuted, tween(200))
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(999.dp)).background(bg).clickable {
                                formatTab = tab
                                selectedFormat = if (tab == "video") info.videoFormats.firstOrNull() else info.audioFormats.firstOrNull()
                            }.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                                Text(label, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = fg, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }

                    // Format list
                    val formats = if (formatTab == "video") info.videoFormats else info.audioFormats
                    LazyColumn(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Black).border(1.dp, BorderDim, RoundedCornerShape(12.dp)).padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(formats) { fmt ->
                            val isSelected = selectedFormat?.itag == fmt.itag
                            val itemBg by animateColorAsState(if (isSelected) Color(0xFF1A1A1A) else Color.Transparent, tween(200))
                            val itemBorder by animateColorAsState(if (isSelected) White else Color.Transparent, tween(200))
                            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(itemBg)
                                .border(1.dp, itemBorder, RoundedCornerShape(10.dp)).clickable { selectedFormat = fmt }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(fmt.displayName, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = White, fontWeight = FontWeight.Bold)
                                    val sizeText = if (fmt.approxSizeMb > 0) " · ~${fmt.approxSizeMb.toInt()} MB" else ""
                                    Text("${fmt.container.uppercase()}$sizeText",
                                        fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = TextMuted)
                                }
                                if (isSelected) Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(White).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                    Text("\u2713", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Download button
                    val sel = selectedFormat
                    if (sel != null) {
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(999.dp)).background(AccentGreen).clickable {
                            DownloadManager.downloadUrl(info.videoId, info.title, sel)
                        }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text("\u2B07 DOWNLOAD ${sel.displayName}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Active downloads
                val activeDl = downloads.filter { it.state !is DownloadState.Idle }
                if (activeDl.isNotEmpty()) {
                    Text("ACTIVE DOWNLOADS", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    LazyColumn(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(CardDark).border(1.dp, BorderDim, RoundedCornerShape(12.dp)).padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(activeDl, key = { it.id }) { item ->
                            DownloadRow(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadRow(item: DownloadItem) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF0A0A0A)).border(1.dp, BorderDim, RoundedCornerShape(10.dp)).padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.title.take(40), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                val stateColor = when (item.state) {
                    is DownloadState.Done -> AccentGreen
                    is DownloadState.Error -> Color(0xFFFF6B6B)
                    is DownloadState.Converting -> Color(0xFFFFAA00)
                    else -> White
                }
                val stateLabel = when (item.state) {
                    is DownloadState.Fetching -> "FETCHING"
                    is DownloadState.Downloading -> "DOWNLOADING"
                    is DownloadState.Converting -> (item.state as DownloadState.Converting).message
                    is DownloadState.Done -> "DONE \u2713"
                    is DownloadState.Error -> "ERROR"
                    is DownloadState.Idle -> "QUEUED"
                }
                Text(stateLabel, fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = stateColor, fontWeight = FontWeight.Bold)
                Text(item.format.displayName, fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = TextMuted)
                if (item.state is DownloadState.Done) {
                    val sizeKb = (item.state as DownloadState.Done).sizeBytes / 1024
                    val sizeStr = if (sizeKb > 1024) "${sizeKb / 1024}MB" else "${sizeKb}KB"
                    Text(sizeStr, fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = TextMuted)
                }
            }
            // Progress bar
            if (item.state is DownloadState.Downloading && item.totalBytes > 0) {
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFF1A1A1A))) {
                    Box(modifier = Modifier.fillMaxWidth(item.progress.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(999.dp)).background(AccentGreen))
                }
                val pct = (item.progress * 100).toInt()
                Text("${pct}%", fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = TextMuted)
            } else if (item.state is DownloadState.Downloading) {
                // Indeterminate animation
                val infiniteTransition = rememberInfiniteTransition(label = "indeterminate")
                val offset by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse), label = "offset")
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFF1A1A1A))) {
                    Box(modifier = Modifier.fillMaxWidth(0.3f).fillMaxHeight().clip(RoundedCornerShape(999.dp)).background(AccentGreen)
                        .offset(x = (offset * 0.7f * 400).dp))
                }
            }
        }
        // Remove button
        Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFF1A1A1A)).border(1.dp, BorderDim, RoundedCornerShape(999.dp))
            .clickable { DownloadManager.removeItem(item.id) }, contentAlignment = Alignment.Center) {
            Text("X", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = TextMuted)
        }
    }
}
