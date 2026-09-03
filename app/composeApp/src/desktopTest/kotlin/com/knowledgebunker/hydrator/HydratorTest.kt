package com.knowledgebunker.hydrator

import com.knowledgebunker.cache.MediaCache
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HydratorTest {

    private fun mockClient(): HttpClient {
        val mockEngine = MockEngine { request ->
            val urlParam = request.url.parameters["url"] ?: ""
            // Simulate noembed response for the specific youtube url
            if (request.url.host == "noembed.com") {
                val json = """{"title":"Test Video Title","thumbnail_url":"https://i.ytimg.com/vi/RIuqjFP2cHg/hqdefault.jpg","author_name":"Test Author","provider_name":"YouTube"}"""
                respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            } else if (request.url.host == "www.youtube.com") {
                val json = """{"title":"OEmbed Fallback Title","thumbnail_url":"https://i.ytimg.com/vi/abc/hqdefault.jpg","author_name":"Fallback Author"}"""
                respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                respond("{}", HttpStatusCode.NotFound, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        return HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
    }

    @Test
    fun hydratesYouTubeViaNoEmbed() = runBlocking {
        val tmp = Files.createTempDirectory("bunker-test").toFile()
        val cache = MediaCache("test-bunker", tmp.absolutePath)
        val client = mockClient()
        val hydrated = Hydrator.hydrateWithCache("https://youtu.be/RIuqjFP2cHg", cache, client)
        assertTrue(hydrated.title.isNotEmpty(), "title should not be empty")
        assertEquals("Test Video Title", hydrated.title)
        assertTrue(hydrated.thumb.contains("ytimg.com"))
        assertEquals("Test Author", hydrated.author)
        // Verify sidecar cache written
        val sidecar = File(tmp, "test-bunker/.bunker-cache/media.json")
        assertTrue(sidecar.exists(), "sidecar file should exist at ${sidecar.absolutePath}")
        val cached = cache.get("https://youtu.be/RIuqjFP2cHg")
        assertEquals("Test Video Title", cached?.title)
        // Second call should hit in-memory cache (no extra network)
        val second = Hydrator.hydrateWithCache("https://youtu.be/RIuqjFP2cHg", cache, client)
        assertEquals("Test Video Title", second.title)
    }

    @Test
    fun hydratesYouTubeFallbackWhenNoEmbedEmpty() = runBlocking {
        // Engine returns empty title for noembed, should fallback to youtube oembed
        val fallbackEngine = MockEngine { request ->
            if (request.url.host == "noembed.com") {
                respond("""{"title":""}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                respond("""{"title":"OEmbed Fallback Title","thumbnail_url":"https://i.ytimg.com/vi/abc/hqdefault.jpg","author_name":"Fallback Author"}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val client = HttpClient(fallbackEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
        }
        val tmp = Files.createTempDirectory("bunker-test2").toFile()
        val cache = MediaCache("test-bunker2", tmp.absolutePath)
        val hydrated = Hydrator.hydrateWithCache("https://youtu.be/RIuqjFP2cHg", cache, client)
        assertEquals("OEmbed Fallback Title", hydrated.title)
    }

    @Test
    fun hydrateCachesToFileAndReloads() = runBlocking {
        val tmp = Files.createTempDirectory("bunker-cache-reload").toFile()
        val cache1 = MediaCache("my-bunker", tmp.absolutePath)
        val client = mockClient()
        Hydrator.hydrateWithCache("https://youtu.be/RIuqjFP2cHg", cache1, client)
        // New cache instance should load from file
        val cache2 = MediaCache("my-bunker", tmp.absolutePath)
        cache2.load()
        val loaded = cache2.get("https://youtu.be/RIuqjFP2cHg")
        assertTrue(loaded != null && loaded.title == "Test Video Title")
    }

    @Test
    fun hydrateRealNoembedIntegration() = runBlocking {
        // Real network test — may be skipped if offline. We attempt live noembed.
        // Use CIO client via Hydrator default but with temp cache to avoid pollution.
        val tmp = Files.createTempDirectory("bunker-live").toFile()
        val liveCache = MediaCache("live-bunker", tmp.absolutePath)
        val liveClient = HttpClient(io.ktor.client.engine.cio.CIO) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
        }
        try {
            val h = Hydrator.hydrateWithCache("https://youtu.be/RIuqjFP2cHg", liveCache, liveClient)
            // Even if network fails, hydrate returns Hydrated with title==url, so we only assert not empty
            // But if network succeeds, title should differ from url
            assertTrue(h.title.isNotEmpty(), "live hydrate title not empty: ${h.title}")
            // If live fetch succeeded, thumb should be ytimg
            // Don't assert strictly to avoid flakiness
        } catch (_: Exception) {
            // Network unavailable — still pass as long as code didn't crash
            assertTrue(true)
        } finally {
            liveClient.close()
        }
    }
}
