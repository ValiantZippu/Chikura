package com.chikura.media

import com.chikura.hydrator.Hydrator
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.withTimeout

// Universal link preview for ALL urls (not just YouTube): OpenGraph /
// twitter-card title + description + image, parsed by hand (no regex brace
// traps). Uses Hydrator's existing Ktor client — zero new dependencies.
// Memory-cached per session; failures return null and the card falls back.

data class LinkPreview(val title: String?, val description: String?, val imageUrl: String?)

private val previewMemory: MutableMap<String, LinkPreview?> = mutableMapOf()

suspend fun fetchLinkPreview(url: String): LinkPreview? {
    val key = url.lowercase()
    if (previewMemory.containsKey(key)) return previewMemory[key]
    val result = try {
        withTimeout(12_000) {
            val target = if (url.startsWith("http")) url else "https://$url"
            val html: String = Hydrator.httpClient.get(target) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36")
            }.bodyAsText()
            if (html.length < 500) null else parseLinkPreview(html.take(300_000))
        }
    } catch (_: Exception) {
        null
    }
    if (previewMemory.size > 500) previewMemory.clear()
    previewMemory[key] = result
    return result
}

private fun metaAttr(tag: String, name: String): String? {
    for (q in listOf('"', '\'')) {
        val needle = name + "=" + q
        var i = tag.indexOf(needle, ignoreCase = true)
        while (i >= 0) {
            val start = i + needle.length
            val end = tag.indexOf(q, start)
            if (end > start && end - start < 2000) return tag.substring(start, end)
            i = tag.indexOf(needle, start, ignoreCase = true)
        }
    }
    return null
}

fun parseLinkPreview(html: String): LinkPreview? {
    var title: String? = null
    var desc: String? = null
    var image: String? = null
    var idx = 0
    while (idx >= 0) {
        val m = html.indexOf("<meta", idx, ignoreCase = true)
        if (m < 0) break
        val e = html.indexOf(">", m)
        if (e < 0 || e - m > 2000) {
            idx = m + 5
            continue
        }
        val tag = html.substring(m, e + 1)
        val prop = (metaAttr(tag, "property") ?: metaAttr(tag, "name"))?.lowercase()
        val content = metaAttr(tag, "content")?.trim()?.takeIf { it.isNotBlank() }
        if (content == null) {
            idx = e + 1
            continue
        }
        when (prop) {
            "og:title", "twitter:title" -> if (title == null) title = content
            "og:description", "twitter:description", "description" -> if (desc == null) desc = content
            "og:image", "twitter:image" -> if (image == null && content.startsWith("http")) image = content
        }
        if (title != null && desc != null && image != null) break
        idx = e + 1
        if (idx > 120_000) break
    }
    if (title == null) {
        val t0 = html.indexOf("<title", ignoreCase = true)
        if (t0 >= 0 && t0 < 60_000) {
            val s = html.indexOf(">", t0)
            val x = html.indexOf("</title", s, ignoreCase = true)
            if (s > 0 && x > s && x - s < 400) title = html.substring(s + 1, x).trim()
        }
    }
    if (title == null && desc == null && image == null) return null
    return LinkPreview(
        title?.let { unescapeHtml(it).trim().take(160) },
        desc?.let { unescapeHtml(it).trim().take(300) },
        image
    )
}

fun unescapeHtml(s: String): String =
    s.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
        .replace("&quot;", "\"").replace("&#39;", "'").replace("&#x27;", "'")
        .replace("&nbsp;", " ")
