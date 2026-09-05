package com.chikura.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chikura.app.EditMode
import com.chikura.hydrator.Hydrated
import com.chikura.hydrator.Hydrator
import com.chikura.media.NowPlaying
import com.chikura.media.LinkPreview
import com.chikura.media.PlaybackStore
import com.chikura.media.fetchLinkPreview
import com.chikura.media.VideoComment
import com.chikura.media.VideoComments
import com.chikura.media.VideoDetails
import com.chikura.media.YouTubeEmbed
import com.chikura.media.fetchVideoReplies
import com.chikura.media.VideoProgress
import com.chikura.media.VideoThumbnail
import com.chikura.media.DownloadManager
import com.chikura.media.DownloadFormat
import com.chikura.media.fetchVideoComments
import com.chikura.media.fetchVideoDetails
import com.chikura.media.formatChapterTime
import com.chikura.media.formatDuration
import com.chikura.media.openVideoUrl
import com.chikura.media.youtubeThumbUrl
import com.chikura.media.youtubeWatchUrlAt
import com.chikura.model.Resource
import com.chikura.ui.theme.*
import kotlinx.coroutines.launch

private fun extractYouTubeId(url: String): String? {
    val lower = url.lowercase()
    return when {
        lower.contains("youtu.be/") -> url.substringAfter("youtu.be/").substringBefore("?").substringBefore("&").trim()
        lower.contains("watch?v=") -> url.substringAfter("watch?v=").substringBefore("&").substringBefore("?").trim()
        lower.contains("/shorts/") -> url.substringAfter("/shorts/").substringBefore("?").substringBefore("/").trim()
        lower.contains("/live/") -> url.substringAfter("/live/").substringBefore("?").trim()
        else -> null
    }?.takeIf { it.isNotBlank() && it.length in 6..20 }
}

private fun channelHandle(url: String): String {
    val lower = url.lowercase()
    return when {
        "@" in url -> "@" + url.substringAfter("@").substringBefore("/").substringBefore("?").substringBefore("&")
        "/channel/" in lower -> url.substringAfter("/channel/").substringBefore("/").substringBefore("?").take(14)
        "/c/" in lower -> url.substringAfter("/c/").substringBefore("/").substringBefore("?")
        "/user/" in lower -> url.substringAfter("/user/").substringBefore("/").substringBefore("?")
        else -> url.take(24)
    }.trim().ifBlank { url.take(24) }
}

@Composable
private fun HoverPlayButton(size: Dp, fontSize: TextUnit, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val hov by src.collectIsHoveredAsState()
    val s by animateFloatAsState(if (hov) 1.18f else 1f, tween(160), label = "playHover")
    Box(
        modifier = Modifier.size(size).scale(s).clip(RoundedCornerShape(999.dp))
            .background(if (hov) Color(0xFF00D084) else Color.White)
            .border(1.dp, Color.White, RoundedCornerShape(999.dp))
            .clickable(interactionSource = src, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(">", fontFamily = FontFamily.Monospace, fontSize = fontSize, color = Color.Black, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ResourceCard(
    resource: Resource,
    modifier: Modifier = Modifier,
    hydrated: Hydrated? = null,
    editMode: EditMode = EditMode.VIEW,
    incognito: Boolean = true,
    onExpand: (() -> Unit)? = null
) {
    var hovered by remember { mutableStateOf(false) }
    var hydratedState by remember(resource.url) { mutableStateOf(hydrated) }
    var expanded by remember { mutableStateOf(false) }
    var details by remember(resource.url) { mutableStateOf<VideoDetails?>(null) }
    var comments by remember(resource.url) { mutableStateOf<VideoComments?>(null) }
    var descOpen by remember(resource.url) { mutableStateOf(false) }
    var progress by remember(resource.url) { mutableStateOf(VideoProgress()) }
    var thumbAttempt by remember(resource.url) { mutableStateOf(0) }
    var showDownloadPanel by remember(resource.url) { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    LaunchedEffect(resource.url) {
        if (hydratedState == null && (resource.typeHint == "video" || resource.typeHint == "channel" || resource.typeHint == "website" || resource.typeHint == "shorts")) {
            try {
                val h = Hydrator.hydrate(resource.url)
                if (h.title != h.url) hydratedState = h
            } catch (_: Exception) {}
        }
        val vid = extractYouTubeId(resource.url)
        if (vid != null) {
            try { progress = PlaybackStore.get(vid) } catch (_: Exception) {}
        }
    }
    // Heavy watch-page + comments fetch only when opened: hundreds of cards
    // must not each download pages on scroll.
    LaunchedEffect(resource.url, expanded) {
        if (!expanded || (resource.typeHint != "video" && resource.typeHint != "shorts" && resource.typeHint != "live")) return@LaunchedEffect
        val vid = extractYouTubeId(resource.url) ?: return@LaunchedEffect
        try {
            if (details == null) fetchVideoDetails(vid)?.let { details = it }
            if (comments == null) comments = fetchVideoComments(vid)
            thumbAttempt++
        } catch (_: Exception) {}
    }
    val scale by animateFloatAsState(if (hovered) 1.015f else 1f, tween(240, easing = ChikuraEase), label = "cardScale")
    val borderColor by animateColorAsState(if (hovered) ChikuraWhite else ChikuraBorder, tween(200), label = "border")
    val isVideo = resource.typeHint == "video" || resource.typeHint == "shorts" || resource.typeHint == "live" || extractYouTubeId(resource.url) != null
    val isChannel = resource.typeHint == "channel"
    val isPlaylist = resource.typeHint == "playlist"
    val ytId = remember(resource.url) { extractYouTubeId(resource.url) }
    var linkPreview by remember(resource.url) { mutableStateOf<LinkPreview?>(null) }
    var previewTried by remember(resource.url) { mutableStateOf(false) }
    // Universal preview for all urls, fetched once on expand only.
    LaunchedEffect(resource.url, expanded) {
        if (!expanded || isVideo || isChannel || isPlaylist || previewTried) return@LaunchedEffect
        previewTried = true
        if (hydratedState?.title?.isNotBlank() == true) return@LaunchedEffect
        try {
            fetchLinkPreview(resource.url)?.let { linkPreview = it }
        } catch (_: Exception) {}
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(ChikuraCard)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { hovered = !hovered; expanded = !expanded; onExpand?.invoke() }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            if (isChannel) {
                val hs = hydratedState
                val handle = remember(resource.url) { channelHandle(resource.url) }
                val name = if (hs != null && hs.title.isNotBlank()) hs.title else handle
                Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(999.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                        Text(name.take(1).uppercase().ifBlank { "#" }, fontFamily = FontFamily.Monospace, fontSize = 20.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(name, fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = ChikuraWhite, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("$handle · youtube channel", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color.White).padding(horizontal = 7.dp, vertical = 3.dp)) {
                                Text("CHANNEL", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            if (hydratedState != null) Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF00D084)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                                Text("HYDRA", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color.White).clickable { openVideoUrl(resource.url) }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("OPEN ↗", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (isPlaylist) {
                val hs = hydratedState
                val titleText = if (hs != null && hs.title.isNotBlank() && hs.title != hs.url) hs.title else fallbackTitle(resource.url, resource)
                Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A1A)).border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                repeat(3) { Box(modifier = Modifier.size(width = 14.dp, height = 2.dp).background(Color.White)) }
                            }
                            Text(">", fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(titleText, fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = ChikuraWhite, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 17.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color.White).padding(horizontal = 7.dp, vertical = 3.dp)) {
                                Text("PLAYLIST", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            if (hydratedState != null) Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF00D084)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                                Text("HYDRA", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color.White).clickable { openVideoUrl(resource.url) }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("OPEN", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (isVideo) {
                val hs = hydratedState
                val det = details
                val thumbUrl = hs?.thumb?.takeIf { it.isNotBlank() } ?: ytId?.let { youtubeThumbUrl(it) } ?: ""
                val durText = det?.let { formatDuration(it.durationSec) }
                if (expanded) {
                    // Expanded: true 16:9 player area, circular play control
                    Box(
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)).background(Color(0xFF080808)),
                        contentAlignment = Alignment.Center
                    ) {
                    val isPlaying = NowPlaying.videoId != null && NowPlaying.videoId == ytId
                    if (isPlaying && ytId != null) {
                        key(ytId) {
                            YouTubeEmbed(
                                videoId = ytId,
                                modifier = Modifier.fillMaxSize(),
                                incognito = incognito,
                                autoplay = true,
                                startSec = progress.positionSec
                            )
                        }
                    } else {
                        if (thumbUrl.isNotBlank()) VideoThumbnail(imageUrl = thumbUrl, modifier = Modifier.fillMaxSize(), attempt = thumbAttempt)
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.10f)))
                        HoverPlayButton(size = 64.dp, fontSize = 24.sp) {
                            if (ytId == null) return@HoverPlayButton
                            NowPlaying.videoId = ytId
                            onExpand?.invoke()
                        }
                    }
                        if (isPlaying) {
                            Box(modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).clip(RoundedCornerShape(6.dp)).background(Color.White).clickable {
                                NowPlaying.videoId = null
                            }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text("STOP", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        } else if (progress.positionSec > 0) {
                            Box(modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF00D084)).clickable {
                                if (ytId == null) return@clickable
                                NowPlaying.videoId = ytId
                            }.padding(horizontal = 6.dp, vertical = 3.dp)) {
                                Text("RESUME ${formatDuration(progress.positionSec)}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.85f)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                            Text(if (durText != null) durText else "NOW PLAYING", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color.White).padding(horizontal = 7.dp, vertical = 4.dp)) {
                                Text("VIDEO", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            if (progress.watched) Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color.White).padding(horizontal = 6.dp, vertical = 4.dp)) {
                                Text("SEEN", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            } else if (hydratedState != null) Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF00D084)).padding(horizontal = 6.dp, vertical = 4.dp)) {
                                Text("HYDRA", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (incognito) Color(0xFF00D084) else Color(0xFF4A90E2)).padding(horizontal = 6.dp, vertical = 4.dp)) {
                                Text(if (incognito) "INC" else "ACC", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Collapsed: Discord-style horizontal embed — full 4:3 thumb
                    // beside the text, no crop, no floating square button.
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(width = 132.dp, height = 99.dp).background(Color(0xFF080808)), contentAlignment = Alignment.Center) {
                            if (thumbUrl.isNotBlank()) VideoThumbnail(imageUrl = thumbUrl, modifier = Modifier.fillMaxSize(), attempt = thumbAttempt)
                            HoverPlayButton(size = 36.dp, fontSize = 15.sp) {
                                if (ytId == null) return@HoverPlayButton
                                expanded = true
                                NowPlaying.videoId = ytId
                                onExpand?.invoke()
                            }
                        }
                        Column(modifier = Modifier.weight(1f).padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val hs2 = hydratedState
                            val titleText = if (hs2 != null && hs2.title.isNotBlank()) hs2.title else resource.raw
                            Text(titleText, fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = ChikuraWhite, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 17.sp)
                            val hs3 = hydratedState
                            val subtitle = when { hs3 != null && hs3.author.isNotBlank() -> hs3.author else -> resource.url }
                            Text(subtitle, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ChikuraGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (durText != null) Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color.Black).border(1.dp, Color(0xFF333333), RoundedCornerShape(6.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                                    Text(durText, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                if (progress.watched) Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color.White).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text("SEEN", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                                if (progress.positionSec > 0) Text("RESUME ${formatDuration(progress.positionSec)}", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color(0xFF00D084))
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isVideo && !isChannel && !isPlaylist) {
                    val lp = linkPreview
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(ChikuraSurface2).border(1.dp, ChikuraBorder, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            val hs = hydratedState
                            val img = hs?.thumb?.takeIf { it.isNotBlank() } ?: lp?.imageUrl?.takeIf { it.isNotBlank() }
                            if (img != null) VideoThumbnail(imageUrl = img, modifier = Modifier.fillMaxSize())
                            else Text(resource.typeHint.take(4).uppercase(), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ChikuraGray, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            val hs = hydratedState
                            val titleText = when {
                                hs != null && hs.title.isNotBlank() && hs.title != hs.url -> hs.title
                                lp?.title?.isNotBlank() == true -> lp.title
                                else -> fallbackTitle(resource.url, resource)
                            }
                            Text(titleText, fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = ChikuraWhite, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 17.sp)
                            val hs2 = hydratedState
                            val subtitle = when {
                                hs2 != null && hs2.author.isNotBlank() -> hs2.author
                                lp?.description?.isNotBlank() == true -> lp.description
                                else -> resource.url
                            }
                            Text(subtitle, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(ChikuraWhite).padding(horizontal = 7.dp, vertical = 4.dp)) {
                            Text(resource.typeHint.uppercase().take(4), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ChikuraBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (expanded) {
                    val hs = hydratedState
                    val titleText = if (hs != null && hs.title.isNotBlank() && hs.title != hs.url) hs.title else fallbackTitle(resource.url, resource)
                    Text(titleText, fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = ChikuraWhite, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFF222222)), contentAlignment = Alignment.Center) {
                            Text((hs?.author?.take(1)?.uppercase() ?: ">"), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            val hs2 = hydratedState
                            val author = hs2?.author?.takeIf { it.isNotBlank() } ?: (resource.category ?: "YouTube")
                            Text(author, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(resource.section ?: domainHint(resource), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = ChikuraGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF111111)).border(1.dp, Color(0xFF222222), RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text(if (expanded) "STOP" else "PLAY", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (resource.section != null || resource.category != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (resource.section != null) Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(ChikuraSurface2).border(1.dp, ChikuraBorder, RoundedCornerShape(999.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text(resource.section, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = ChikuraGray)
                            }
                            if (resource.category != null) Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(ChikuraWhite).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text(resource.category, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = ChikuraBlack, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (expanded && ytId != null) {
                        val vid = ytId
                        val det2 = details
                        if (det2 != null && det2.chapters.isNotEmpty()) {
                            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF0A0A0A)).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(10.dp)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("CHAPTERS · ${det2.chapters.size} — tap to play from here", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraGray, fontWeight = FontWeight.Bold)
                                det2.chapters.take(8).forEach { ch ->
                                    val chSrc = remember(ch) { MutableInteractionSource() }
                                    val chHov by chSrc.collectIsHoveredAsState()
                                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (chHov) Color(0xFF1F1F1F) else Color(0xFF111111)).clickable(interactionSource = chSrc, indication = null) {
                                        openVideoUrl(youtubeWatchUrlAt(vid, ch.startSec))
                                        val np = progress.copy(positionSec = ch.startSec)
                                        progress = np
                                        try { PlaybackStore.set(vid, np) } catch (_: Exception) {}
                                    }.padding(horizontal = 8.dp, vertical = 5.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(formatChapterTime(ch.startSec), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = if (chHov) Color(0xFF00D084) else Color.White, fontWeight = FontWeight.Bold)
                                            Text(ch.title, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraGray, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                        val det3 = details
                        if (det3 != null && det3.description.isNotBlank()) {
                            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF0A0A0A)).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(10.dp)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("DESCRIPTION", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraGray, fontWeight = FontWeight.Bold)
                                Text(det3.description, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color(0xFFE0E0E0), lineHeight = 13.sp, maxLines = if (descOpen) 40 else 3, overflow = TextOverflow.Ellipsis)
                                Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF111111)).clickable { descOpen = !descOpen }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text(if (descOpen) "LESS" else "MORE", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color.White)
                                }
                            }
                        }
                        val thread = comments
                        if (thread != null) {
                            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF0A0A0A)).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(10.dp)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("COMMENTS · ${thread.totalText ?: "${thread.comments.size}"}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraGray, fontWeight = FontWeight.Bold)
                                if (thread.comments.isEmpty()) Text("Comments unavailable (login-walled or blocked)", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color(0xFF3A3A3A))
                                thread.comments.take(6).forEach { c -> CommentRow(comment = c) }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (progress.watched) Color.White else Color.Transparent).border(1.dp, if (progress.watched) Color.White else Color(0xFF333333), RoundedCornerShape(999.dp)).clickable {
                                val np = progress.copy(watched = !progress.watched)
                                progress = np
                                try { PlaybackStore.set(vid, np) } catch (_: Exception) {}
                            }.padding(horizontal = 10.dp, vertical = 5.dp)) {
                                Text(if (progress.watched) "WATCHED" else "MARK SEEN", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = if (progress.watched) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                            }
                            Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(AccentGreen).border(1.dp, AccentGreen, RoundedCornerShape(999.dp)).clickable { showDownloadPanel = true }.padding(horizontal = 10.dp, vertical = 5.dp)) {
                                Text("⬇ DL MP4/MP3", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).border(1.dp, Color(0xFF333333), RoundedCornerShape(999.dp)).clickable {
                                clipboard.setText(AnnotatedString(resource.url))
                            }.padding(horizontal = 10.dp, vertical = 5.dp)) {
                                Text("COPY LINK", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraGray)
                            }
                        }
                    }
                }

                if (editMode == EditMode.EDIT) {
                    var editText by remember(resource.id) { mutableStateOf(resource.raw) }
                    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF0A0A0A)).border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(10.dp)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("EDIT", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color.White).padding(horizontal = 6.dp, vertical = 3.dp))
                            Text("Notion-like block - indent is hierarchy", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ChikuraGray)
                            Spacer(Modifier.weight(1f))
                            Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF1A1A1A)).padding(horizontal = 8.dp, vertical = 4.dp)) { Text("Tab indent", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ChikuraGray) }
                        }
                        TextField(value = editText, onValueChange = { editText = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("- https://...", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraGray) }, colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = Color.White, focusedTextColor = Color.White, unfocusedTextColor = Color.White), textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp))
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ChikuraSurface1).border(1.dp, ChikuraDim, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 5.dp)) {
                    Text(resource.url, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).border(1.dp, ChikuraWhite, RoundedCornerShape(999.dp)).background(Color.Transparent).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text(resource.typeHint, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraWhite)
                    }
                    if (hydratedState != null) Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(ChikuraCard).border(1.dp, ChikuraBorder, RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text("hydra cached", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = ChikuraGray)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(">", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ChikuraGray)
                }
                // Quick actions row — open in browser, copy, note
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF0A0A0A)).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF111111)).border(1.dp, Color(0xFF222222), RoundedCornerShape(999.dp)).clickable { openVideoUrl(resource.url) }.padding(horizontal = 8.dp, vertical = 4.dp)) { Text("OPEN ↗", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color.White) }
                    Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF111111)).border(1.dp, Color(0xFF222222), RoundedCornerShape(999.dp)).clickable { clipboard.setText(AnnotatedString(resource.url)) }.padding(horizontal = 8.dp, vertical = 4.dp)) { Text("COPY", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = ChikuraGray) }
                    Spacer(Modifier.weight(1f))
                    if (!isVideo) Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF111111)).border(1.dp, Color(0xFF222222), RoundedCornerShape(999.dp)).clickable { openVideoUrl(resource.url) }.padding(horizontal = 8.dp, vertical = 4.dp)) { Text("VIEW SITE", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = ChikuraGray) }
                }
            }
        }
    }
}

private val AccentGreen = Color(0xFF00D084)

private fun domainHint(r: Resource): String = r.domain

/** Extract a readable title from a URL when hydration fails */
private fun fallbackTitle(url: String, resource: Resource): String {
    // If there's a category or section, use that
    if (!resource.category.isNullOrBlank()) return resource.category
    if (!resource.section.isNullOrBlank()) return resource.section
    // Try to extract a readable name from the URL
    val lower = url.lowercase()
    return when {
        lower.contains("youtu.be") || lower.contains("youtube.com") -> "YouTube Video"
        lower.contains("soundcloud.com") -> "SoundCloud Track"
        lower.contains("spotify.com") -> "Spotify Track"
        lower.contains("github.com") -> "GitHub Repository"
        lower.contains("twitch.tv") -> "Twitch Stream"
        lower.contains("store.steampowered.com") -> "Steam Store"
        else -> {
            // Extract domain name from URL
            try {
                val host = java.net.URI(url).host ?: url
                host.removePrefix("www.").take(30)
            } catch (_: Exception) {
                url.take(30)
            }
        }
    }
}

// Discord-style message row: avatar, name + time, body, likes + replies.
@Composable
private fun CommentRow(comment: VideoComment) {
    var replies by remember(comment) { mutableStateOf<List<VideoComment>?>(null) }
    var repliesOpen by remember(comment) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFF1A1A1A)), contentAlignment = Alignment.Center) {
            if (comment.avatarUrl.isNotBlank()) {
                VideoThumbnail(imageUrl = comment.avatarUrl, modifier = Modifier.fillMaxSize())
            } else {
                Text(comment.author.take(1).uppercase().ifBlank { "#" }, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(comment.author, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f, fill = false))
                if (comment.time.isNotBlank()) Text(comment.time, fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color(0xFF6B6B6B))
            }
            Text(comment.text, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color(0xFFE0E0E0), lineHeight = 12.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${comment.likes} likes", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color(0xFF6B6B6B))
                if (comment.replyCount > 0 || comment.replyContinuation != null) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (repliesOpen) Color.White else Color(0xFF111111)).border(1.dp, if (repliesOpen) Color.White else Color(0xFF222222), RoundedCornerShape(999.dp)).clickable {
                        repliesOpen = !repliesOpen
                        if (repliesOpen && replies == null) {
                            val cont = comment.replyContinuation
                            if (cont != null) {
                                scope.launch {
                                    try {
                                        replies = fetchVideoReplies(cont)
                                    } catch (_: Exception) {
                                        replies = emptyList()
                                    }
                                }
                            } else {
                                replies = emptyList()
                            }
                        }
                    }.padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text(
                            if (repliesOpen) "HIDE" else "${comment.replyCount} REPLIES",
                            fontFamily = FontFamily.Monospace, fontSize = 8.sp,
                            color = if (repliesOpen) Color.Black else Color.White, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (repliesOpen) {
                val loaded = replies
                if (loaded == null) {
                    Text("loading replies…", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color(0xFF3A3A3A))
                } else {
                    loaded.take(5).forEach { r ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(start = 8.dp)) {
                            Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFF1A1A1A)), contentAlignment = Alignment.Center) {
                                if (r.avatarUrl.isNotBlank()) {
                                    VideoThumbnail(imageUrl = r.avatarUrl, modifier = Modifier.fillMaxSize())
                                } else {
                                    Text(r.author.take(1).uppercase().ifBlank { "#" }, fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(r.author, fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f, fill = false))
                                    if (r.time.isNotBlank()) Text(r.time, fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = Color(0xFF6B6B6B))
                                }
                                Text(r.text, fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color(0xFFE0E0E0), lineHeight = 11.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}
