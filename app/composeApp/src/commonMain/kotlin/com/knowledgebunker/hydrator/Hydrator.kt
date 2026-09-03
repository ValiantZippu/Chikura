package com.knowledgebunker.hydrator

import com.knowledgebunker.cache.MediaCache
import com.knowledgebunker.platform.FileSystem
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Task 5: Hydrator — YouTube OEmbed → Sidecar Cache
 * data class Hydrated(url, title, thumb, author)
 * suspend fun hydrate(url): Hydrated using Ktor client to https://noembed.com/embed?url= (fallback to OEmbed),
 * cache to bunkers/<id>/.bunker-cache/media.json (gitignored) + in-memory via simple file cache.
 */

@Serializable
data class Hydrated(
    val url: String,
    val title: String,
    val thumb: String = "",
    val author: String = ""
)

@Serializable
private data class NoEmbedResponse(
    val title: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("author_name") val authorName: String? = null,
    @SerialName("provider_name") val providerName: String? = null,
    val url: String? = null
)

/**
 * Singleton hydrator with injectable client/cache for tests.
 * Default cache is per-"default" bunker sidecar — callers with known bunkerId can
 * create their own MediaCache(bunkerId) and call hydrateWithCache().
 */
object Hydrator {
    // In-memory + file sidecar cache. Lazy-load on first use.
    private var _cache: MediaCache = MediaCache("default", FileSystem.homeBunkersPath()).also {
        try { it.load() } catch (_: Exception) { }
    }
    val cache: MediaCache get() = _cache

    // HttpClient configured with JSON content negotiation. Replaced in tests via setter.
    private var _httpClient: HttpClient = createDefaultClient()
    val httpClient: HttpClient get() = _httpClient

    private fun createDefaultClient(): HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    /** For tests: inject a custom HttpClient (e.g. mock engine) */
    fun setClient(client: HttpClient) { _httpClient = client }

    /** For tests: inject a temp MediaCache (e.g. temp dir) */
    fun setCache(mediaCache: MediaCache) { _cache = mediaCache }

    suspend fun hydrate(url: String): Hydrated = hydrateWithCache(url, _cache, _httpClient)

    suspend fun hydrateWithCache(url: String, mediaCache: MediaCache, client: HttpClient = _httpClient): Hydrated {
        val normalized = url.trim()
        // 1. Check in-memory + file cache
        mediaCache.get(normalized)?.let { return it }

        // 2. Try noembed first
        val fetched = fetchViaNoEmbed(normalized, client)
            ?: fetchViaYoutubeOEmbed(normalized, client)

        val result = fetched ?: Hydrated(url = normalized, title = normalized, thumb = "", author = "")
        // Do not cache fallback bare title==url? Still cache to avoid re-fetch? Cache only successful fetches.
        if (result.title.isNotBlank() && result.title != normalized) {
            try { mediaCache.put(result) } catch (_: Exception) { }
        } else if (fetched != null) {
            try { mediaCache.put(result) } catch (_: Exception) { }
        }
        return result
    }

    private suspend fun fetchViaNoEmbed(url: String, client: HttpClient): Hydrated? {
        return try {
            val resp: NoEmbedResponse = client.get("https://noembed.com/embed") {
                parameter("url", url)
            }.body()
            if (resp.title.isNullOrBlank()) return null
            Hydrated(
                url = url,
                title = resp.title,
                thumb = resp.thumbnailUrl ?: "",
                author = resp.authorName ?: resp.providerName ?: ""
            )
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun fetchViaYoutubeOEmbed(url: String, client: HttpClient): Hydrated? {
        return try {
            // OEmbed endpoint requires format=json
            val resp: NoEmbedResponse = client.get("https://www.youtube.com/oembed") {
                parameter("url", url)
                parameter("format", "json")
            }.body()
            if (resp.title.isNullOrBlank()) return null
            Hydrated(
                url = url,
                title = resp.title,
                thumb = resp.thumbnailUrl ?: "",
                author = resp.authorName ?: resp.providerName ?: ""
            )
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Top-level suspend function per spec: suspend fun hydrate(url): Hydrated
 * Delegates to Hydrator singleton.
 */
suspend fun hydrate(url: String): Hydrated = Hydrator.hydrate(url)
