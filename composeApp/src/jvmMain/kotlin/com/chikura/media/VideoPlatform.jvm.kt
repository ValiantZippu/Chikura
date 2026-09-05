package com.chikura.media

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.ByteArrayInputStream
import java.net.URI
import javax.imageio.ImageIO

private val thumbMemory: MutableMap<String, ImageBitmap> =
    java.util.Collections.synchronizedMap(mutableMapOf())

private val netClient: HttpClient by lazy {
    HttpClient(CIO) {
        followRedirects = true
        install(HttpTimeout) { requestTimeoutMillis = 15000 }
    }
}

private suspend fun downloadBitmap(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
    try {
        thumbMemory[url]?.let { return@withContext it }
        val bytes: ByteArray = netClient.get(url).bodyAsBytes()
        if (bytes.isEmpty() || bytes.size > 8 * 1024 * 1024) return@withContext null
        val img = ImageIO.read(ByteArrayInputStream(bytes)) ?: return@withContext null
        val bitmap = img.toComposeImageBitmap()
        synchronized(thumbMemory) {
            if (thumbMemory.size > 400) {
                thumbMemory.keys.take(200).forEach { thumbMemory.remove(it) }
            }
            thumbMemory[url] = bitmap
        }
        bitmap
    } catch (_: Exception) {
        null
    }
}

@Composable
actual fun VideoThumbnail(imageUrl: String, modifier: Modifier, attempt: Int) {
    var bitmap by remember(imageUrl) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(imageUrl, attempt) {
        if (imageUrl.isBlank()) return@LaunchedEffect
        if (bitmap == null) bitmap = downloadBitmap(imageUrl)
    }
    val current = bitmap
    if (current != null) {
        Image(bitmap = current, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    } else {
        val pulse by rememberInfiniteTransition(label = "thumbLoad").animateFloat(
            initialValue = 0.3f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "thumbAlpha"
        )
        Box(modifier = modifier.background(Color(0xFF0A0A0A)), contentAlignment = Alignment.Center) {
            Text(">", fontFamily = FontFamily.Monospace, fontSize = 18.sp, color = Color(0xFF3A3A3A).copy(alpha = pulse), fontWeight = FontWeight.Bold)
        }
    }
}

actual suspend fun fetchVideoDetails(videoId: String): VideoDetails? = withContext(Dispatchers.IO) {
    try {
        val html: String = netClient.get("https://www.youtube.com/watch?v=$videoId") {
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36")
            header("Accept-Language", "en-US,en;q=0.9")
        }.bodyAsText()
        if (html.length < 50_000) return@withContext null
        parseVideoDetails(videoId, html)
    } catch (_: Exception) {
        null
    }
}

private const val InnerTubeKey = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
private const val InnerTubeUa = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"

private fun innerTubeClientJson(): String =
    """{"clientName":"WEB","clientVersion":"2.20241201","hl":"en","gl":"US"}"""

private suspend fun postInnerTube(bodyJson: String): String? = withContext(Dispatchers.IO) {
    try {
        val json: String = netClient.post("https://www.youtube.com/youtubei/v1/next?key=$InnerTubeKey&prettyPrint=false") {
            header("User-Agent", InnerTubeUa)
            contentType(io.ktor.http.ContentType.Application.Json)
            setBody(bodyJson)
        }.bodyAsText()
        if (json.length < 10_000) null else json
    } catch (_: Exception) {
        null
    }
}

actual suspend fun fetchVideoComments(videoId: String): VideoComments {
    val first = postInnerTube("""{"context":{"client":${innerTubeClientJson()}},"videoId":"$videoId"}""")
        ?: return VideoComments(emptyList(), null)
    // Comments sometimes ride along; usually they need the panel continuation.
    val direct = parseVideoComments(first)
    if (direct.isNotEmpty()) return VideoComments(direct, parseCommentTotal(first))
    val token = extractCommentsContinuation(first)
        ?: return VideoComments(emptyList(), parseCommentTotal(first))
    val second = postInnerTube("""{"context":{"client":${innerTubeClientJson()}},"continuation":"$token"}""")
        ?: return VideoComments(emptyList(), parseCommentTotal(first))
    val comments = parseVideoComments(second)
    return VideoComments(comments, parseCommentTotal(first) ?: parseCommentTotal(second))
}

actual suspend fun fetchVideoReplies(continuation: String): List<VideoComment> {
    val json = postInnerTube("""{"context":{"client":${innerTubeClientJson()}},"continuation":"$continuation"}""")
        ?: return emptyList()
    return parseVideoComments(json, max = 10)
}

actual fun openVideoUrl(url: String) {
    try {
        if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI(url))
    } catch (_: Exception) {
    }
}
