package com.chikura.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

// Only one Chromium view at a time: expanded cards render the inline player
// solely for the video recorded here. Everything else shows thumbnail UI.
object NowPlaying {
    var videoId: String? by mutableStateOf(null)
}

// Platform video plumbing: thumbnails, metadata, external playback.
// Thumbnails load over the existing Ktor client (no new image dep).
// Metadata is parsed from the watch page (no scraper lib); everything
// degrades to Hydrator data when parsing fails.

// --- expect/actual ---

@Composable
expect fun VideoThumbnail(imageUrl: String, modifier: Modifier = Modifier, attempt: Int = 0)

expect suspend fun fetchVideoDetails(videoId: String): VideoDetails?

expect suspend fun fetchVideoComments(videoId: String): VideoComments

expect suspend fun fetchVideoReplies(continuation: String): List<VideoComment>

expect fun openVideoUrl(url: String)

// --- model ---

data class VideoChapter(val title: String, val startSec: Int)

data class VideoDetails(
    val videoId: String,
    val title: String?,
    val author: String?,
    val durationSec: Int,
    val chapters: List<VideoChapter>,
    val description: String = ""
)

data class VideoComment(
    val author: String,
    val text: String,
    val likes: Int,
    val time: String = "",
    val avatarUrl: String = "",
    val replyCount: Int = 0,
    val replyContinuation: String? = null
)

data class VideoComments(val comments: List<VideoComment>, val totalText: String?)

// --- helpers ---

fun youtubeThumbUrl(videoId: String): String =
    "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

fun extractVideoId(url: String): String? {
    val lower = url.lowercase()
    return when {
        lower.contains("youtu.be/") -> url.substringAfter("youtu.be/").substringBefore("?").substringBefore("&").trim()
        lower.contains("watch?v=") -> url.substringAfter("watch?v=").substringBefore("&").substringBefore("?").trim()
        lower.contains("/shorts/") -> url.substringAfter("/shorts/").substringBefore("?").substringBefore("/").trim()
        lower.contains("/live/") -> url.substringAfter("/live/").substringBefore("?").trim()
        else -> null
    }?.takeIf { it.isNotBlank() && it.length in 6..20 }
}

fun youtubeWatchUrlAt(videoId: String, tSec: Int): String =
    if (tSec > 0) "https://www.youtube.com/watch?v=$videoId&t=${tSec}s"
    else "https://www.youtube.com/watch?v=$videoId"

fun formatDuration(totalSec: Int): String {
    if (totalSec <= 0) return "--:--"
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    else "$m:${s.toString().padStart(2, '0')}"
}

fun formatChapterTime(totalSec: Int): String = formatDuration(totalSec)

private val LengthRe = Regex(""""lengthSeconds":"(\d+)"""")
private val TitleRe = Regex(""""title":\{"runs":\[\{"text":"((?:[^"\\]|\\.)*)"""")
private val AuthorRe = Regex(""""author":"((?:[^"\\]|\\.)*)"""")
private val OwnerRe = Regex(""""ownerChannelName":"((?:[^"\\]|\\.)*)"""")
private val ChapterRe = Regex(
    """"macroMarkersListItemRenderer":\{"title":\{"runs":\[\{"text":"((?:[^"\\]|\\.)*)"\}.*?"timeRangeStartMillis":(\d+)""",
    RegexOption.DOT_MATCHES_ALL
)

fun unescapeJsonString(s: String): String {
    val out = StringBuilder(s.length)
    var i = 0
    while (i < s.length) {
        val c = s[i]
        if (c == '\\' && i + 1 < s.length) {
            when (val n = s[i + 1]) {
                'n' -> { out.append('\n'); i += 2 }
                't' -> { out.append('\t'); i += 2 }
                'r' -> { out.append('\r'); i += 2 }
                '"' -> { out.append('"'); i += 2 }
                '\\' -> { out.append('\\'); i += 2 }
                '/' -> { out.append('/'); i += 2 }
                'u' -> {
                    if (i + 5 < s.length) {
                        val hex = s.substring(i + 2, i + 6)
                        val code = hex.toIntOrNull(16)
                        if (code != null) {
                            // Surrogate pairs: combine with following \uXXXX when present
                            if (code in 0xD800..0xDBFF && i + 11 < s.length && s[i + 6] == '\\' && s[i + 7] == 'u') {
                                val lo = s.substring(i + 8, i + 12).toIntOrNull(16)
                                if (lo != null && lo in 0xDC00..0xDFFF) {
                                    val cp = 0x10000 + ((code - 0xD800) shl 10) + (lo - 0xDC00)
                                    val high = (0xD800 + ((cp - 0x10000) shr 10)).toChar()
                                    val low = (0xDC00 + ((cp - 0x10000) and 0x3FF)).toChar()
                                    out.append(high).append(low)
                                    i += 12
                                } else {
                                    out.append(code.toChar()); i += 6
                                }
                            } else {
                                out.append(code.toChar()); i += 6
                            }
                        } else {
                            out.append(n); i += 2
                        }
                    } else {
                        out.append(n); i += 2
                    }
                }
                else -> { out.append(n); i += 2 }
            }
        } else {
            out.append(c); i += 1
        }
    }
    return out.toString()
}

private val DescRe = Regex(""""shortDescription":"((?:[^"\\]|\\.)*)"""")

private val CommentAuthorRe = Regex(""""authorText":\{"simpleText":"((?:[^"\\]|\\.)*)"""")
private val CommentRunsRe = Regex(""""contentText":\{"runs":""")
private val CommentTextRe = Regex(""""text":"((?:[^"\\]|\\.)*)"""")
private val CommentLikesRe = Regex(""""likeCount":(\d+)""")
private val CommentTimeRe = Regex(""""publishedTimeText":\{"runs":\[\{"text":"([^"]+)""")
private val CommentAvatarRe = Regex(""""authorThumbnail":\{""")
private val CommentAvatarUrlRe = Regex(""""url":"((?:[^"\\]|\\.)*)"""")
private val CommentReplyCountRe = Regex(""""replyCount":(\d+)""")
private val CommentContinuationRe = Regex(""""nextContinuationData":\{"continuation":"([^"]+)"""")
private val CommentCommandTokenRe = Regex(""""continuationCommand":\{"token":"([^"]+)"""")
private val CommentsPanelTokenRe = Regex(""""continuationItemRenderer":\{"continuationEndpoint":\{"continuationCommand":\{"token":"([^"]+)"""")
private val CommentTotalRe = Regex(""""countText":\{"runs":\[\{"text":"([^"]+)"""")

/** Best-effort parse of a youtube.com/watch page. Null when blocked/consent-walled. */
fun parseVideoDetails(videoId: String, html: String): VideoDetails? {
    val len = LengthRe.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return null
    if (len <= 0) return null
    val title = TitleRe.find(html)?.groupValues?.getOrNull(1)?.let { unescapeJsonString(it) }?.takeIf { it.isNotBlank() }
    val author = (AuthorRe.find(html)?.groupValues?.getOrNull(1)
        ?: OwnerRe.find(html)?.groupValues?.getOrNull(1))
        ?.let { unescapeJsonString(it) }?.takeIf { it.isNotBlank() }
    val chapters = ChapterRe.findAll(html).mapNotNull { m ->
        val t = unescapeJsonString(m.groupValues[1]).trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val ms = m.groupValues[2].toLongOrNull() ?: return@mapNotNull null
        VideoChapter(t, (ms / 1000).toInt())
    }.distinctBy { it.startSec }.sortedBy { it.startSec }.toList().take(40)
    val desc = DescRe.find(html)?.groupValues?.getOrNull(1)
        ?.let { unescapeJsonString(it) }?.trim()?.take(1500) ?: ""
    return VideoDetails(videoId, title, author, len, chapters, desc)
}

/** Best-effort parse of an InnerTube next-response for top comments. */
fun parseVideoComments(json: String, max: Int = 8): List<VideoComment> {
    val out = mutableListOf<VideoComment>()
    var searchFrom = 0
    while (out.size < max) {
        val idx = json.indexOf("\"commentRenderer\":", searchFrom)
        if (idx < 0) break
        searchFrom = idx + 19
        val window = json.substring(idx, minOf(idx + 8000, json.length))
        val author = CommentAuthorRe.find(window)?.groupValues?.getOrNull(1)
            ?.let { unescapeJsonString(it) }?.trim()?.takeIf { it.isNotBlank() } ?: continue
        val runsStart = CommentRunsRe.find(window)?.range?.last ?: continue
        val runsWindow = window.substring(minOf(runsStart + 1, window.length), minOf(runsStart + 1501, window.length))
        val text = CommentTextRe.findAll(runsWindow).take(8).map { unescapeJsonString(it.groupValues[1]) }
            .joinToString("").trim().take(400)
        if (text.isBlank()) continue
        val likes = CommentLikesRe.find(window)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        val time = CommentTimeRe.find(window)?.groupValues?.getOrNull(1)?.take(24) ?: ""
        val avatar = run {
            val avIdx = CommentAvatarRe.find(window)?.range?.first ?: return@run ""
            val avWindow = window.substring(avIdx, minOf(avIdx + 900, window.length))
            CommentAvatarUrlRe.find(avWindow)?.groupValues?.getOrNull(1)?.takeIf { it.startsWith("http") } ?: ""
        }
        val replyCount = CommentReplyCountRe.find(window)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        val continuation = CommentContinuationRe.find(window)?.groupValues?.getOrNull(1)
        val comment = VideoComment(author.take(40), text, likes, time, avatar, replyCount, continuation)
        if (out.none { it.author == comment.author && it.text == comment.text }) out.add(comment)
    }
    return out
}

/** Total comment count text ("1.2K") from the comments header, if present. */
fun parseCommentTotal(json: String): String? =
    CommentTotalRe.find(json)?.groupValues?.getOrNull(1)?.take(12)

/**
 * Comments live behind a continuation: the first InnerTube response only
 * carries a token inside the engagement panel. Follow it with a second
 * request to get the actual comment threads.
 */
fun extractCommentsContinuation(json: String): String? =
    CommentsPanelTokenRe.find(json)?.groupValues?.getOrNull(1)
