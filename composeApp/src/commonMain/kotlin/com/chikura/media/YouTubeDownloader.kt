package com.chikura.media

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

// Download format representation for a YouTube video
@Serializable
data class DownloadFormat(
    val itag: Int = 0,
    val quality: String = "",
    val qualityLabel: String = "",
    val mimeType: String = "",
    val container: String = "mp4",
    val isAudioOnly: Boolean = false,
    val bitrate: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val url: String = "",
    val approxSizeMb: Double = 0.0
) {
    val displayName: String get() {
        return if (isAudioOnly) {
            val kbps = bitrate / 1000
            "MP3 ${kbps}kbps"
        } else {
            val codec = if (mimeType.contains("vp9")) "VP9" else if (mimeType.contains("av01")) "AV1" else "H.264"
            "${qualityLabel} ${codec}"
        }
    }

    val formatTag: String get() = if (isAudioOnly) "mp3" else "mp4"
}

data class DownloadInfo(
    val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val durationSec: Int,
    val videoFormats: List<DownloadFormat>,
    val audioFormats: List<DownloadFormat>
)

// Parse adaptive formats from InnerTube player response
fun parseDownloadFormats(playerJson: String): DownloadInfo? {
    try {
        val root = Json.parseToJsonElement(playerJson).jsonObject

        // Video details
        val videoDetails = root["videoDetails"]?.jsonObject ?: return null
        val videoId = videoDetails["videoId"]?.jsonPrimitive?.contentOrNull ?: return null
        val title = videoDetails["title"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
        val lengthSeconds = videoDetails["lengthSeconds"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
        val thumbnailUrl = videoDetails["thumbnail"]?.jsonObject?.get("thumbnails")?.jsonArray
            ?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull ?: youtubeThumbUrl(videoId)

        // Streaming data
        val streamingData = root["streamingData"]?.jsonObject ?: return null
        val adaptiveFormats = streamingData["adaptiveFormats"]?.jsonArray ?: return null

        val videoFormats = mutableListOf<DownloadFormat>()
        val audioFormats = mutableListOf<DownloadFormat>()

        for (formatElement in adaptiveFormats) {
            val fmt = formatElement.jsonObject
            val itag = fmt["itag"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: continue
            val mimeType = fmt["mimeType"]?.jsonPrimitive?.contentOrNull ?: ""
            val url = fmt["url"]?.jsonPrimitive?.contentOrNull ?: ""
            val bitrate = fmt["bitrate"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
            val width = fmt["width"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
            val height = fmt["height"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
            val qualityLabel = fmt["qualityLabel"]?.jsonPrimitive?.contentOrNull ?: ""
            val quality = fmt["quality"]?.jsonPrimitive?.contentOrNull ?: ""

            if (url.isBlank()) continue // Skip encrypted formats

            val isAudio = mimeType.startsWith("audio/")
            val container = when {
                mimeType.contains("video/mp4") -> "mp4"
                mimeType.contains("video/webm") -> "webm"
                mimeType.contains("audio/mp4") -> "m4a"
                mimeType.contains("audio/webm") -> "webm"
                else -> "mp4"
            }

            // Approximate file size from contentLength if available
            val contentLength = fmt["contentLength"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L
            val approxSizeMb = if (contentLength > 0) contentLength / (1024.0 * 1024.0) else 0.0

            val format = DownloadFormat(
                itag = itag,
                quality = quality,
                qualityLabel = qualityLabel.ifBlank { "${height}p" },
                mimeType = mimeType,
                container = container,
                isAudioOnly = isAudio,
                bitrate = bitrate,
                width = width,
                height = height,
                url = url,
                approxSizeMb = approxSizeMb
            )

            if (isAudio) {
                // Keep only best quality audio per bitrate
                val existing = audioFormats.indexOfFirst { it.bitrate == bitrate }
                if (existing < 0) audioFormats.add(format)
            } else {
                // Keep highest quality per resolution
                val existing = videoFormats.indexOfFirst { it.height == height && it.container == format.container }
                if (existing < 0) {
                    videoFormats.add(format)
                } else if (videoFormats[existing].bitrate < bitrate) {
                    videoFormats[existing] = format
                }
            }
        }

        // Sort: video by resolution desc, audio by bitrate desc
        val sortedVideo = videoFormats
            .filter { it.url.isNotBlank() }
            .sortedByDescending { it.height * 10000 + it.bitrate }
            .distinctBy { "${it.height}p" }
            .take(10)

        val sortedAudio = audioFormats
            .filter { it.url.isNotBlank() }
            .sortedByDescending { it.bitrate }
            .distinctBy { "${it.bitrate / 1000}kbps" }
            .take(5)

        return DownloadInfo(
            videoId = videoId,
            title = title,
            thumbnailUrl = thumbnailUrl,
            durationSec = lengthSeconds,
            videoFormats = sortedVideo,
            audioFormats = sortedAudio
        )
    } catch (_: Exception) {
        return null
    }
}
