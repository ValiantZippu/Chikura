package com.chikura.media

import java.net.HttpURLConnection
import java.net.URL

/** JVM implementation: fetches the watch page HTML and extracts player response. */
actual fun platformFetchDownloadInfo(videoId: String): DownloadInfo? {
    return try {
        val html = fetchWatchPage(videoId) ?: return null
        parsePlayerFormatsFromHtml(videoId, html)
    } catch (_: Exception) { null }
}

private val InnerTubeUa = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"

private fun fetchWatchPage(videoId: String): String? {
    return try {
        val conn = URL("https://www.youtube.com/watch?v=$videoId").openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", InnerTubeUa)
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
        conn.connectTimeout = 15000
        conn.readTimeout = 20000
        conn.connect()
        val text = conn.inputStream.bufferedReader().readText()
        conn.disconnect()
        if (text.length < 50000) null else text
    } catch (_: Exception) { null }
}

private fun parsePlayerFormatsFromHtml(videoId: String, html: String): DownloadInfo? {
    val marker = "var ytInitialPlayerResponse = "
    val idx = html.indexOf(marker)
    if (idx >= 0) {
        val jsonStart = idx + marker.length
        val jsonEnd = html.indexOf("};", jsonStart)
        if (jsonEnd < 0) return null
        val json = html.substring(jsonStart, jsonEnd + 1)
        return parseDownloadFormats(json)
    }
    val alt = "ytInitialPlayerResponse = "
    val altIdx = html.indexOf(alt)
    if (altIdx < 0) return null
    val jsonStart = altIdx + alt.length
    val jsonEnd = html.indexOf("};", jsonStart)
    if (jsonEnd < 0) return null
    val json = html.substring(jsonStart, jsonEnd + 1)
    return parseDownloadFormats(json)
}
