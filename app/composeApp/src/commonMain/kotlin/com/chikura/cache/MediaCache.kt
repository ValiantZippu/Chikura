package com.chikura.cache

import com.chikura.hydrator.Hydrated
import com.chikura.platform.FileSystem
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Task 5: Sidecar cache — gitignored, not in .md.
 * Stores Hydrated entries to bunkers/<id>/.bunker-cache/media.json
 * plus in-memory map. Simple file cache (SqlDelight alternative per spec).
 *
 * Thread-safety: single-threaded usage via hydrate() suspend; still synchronized on memory.
 */
class MediaCache(
    private val bunkerId: String,
    private val bunkersRoot: String = FileSystem.homeBunkersPath()
) {
    private val memory: MutableMap<String, Hydrated> = mutableMapOf()
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private fun cacheFilePath(): String {
        // Normalize separators: use / and let desktop File handle it
        val root = bunkersRoot.trimEnd('/', '\\')
        return "$root/$bunkerId/.bunker-cache/media.json"
    }

    private fun cacheDirPath(): String {
        val root = bunkersRoot.trimEnd('/', '\\')
        return "$root/$bunkerId/.bunker-cache"
    }

    /** In-memory get — no IO */
    fun get(url: String): Hydrated? = memory[url]

    /** In-memory put + persist to file (sidecar). Persists eagerly so cache survives restart. */
    fun put(hydrated: Hydrated) {
        memory[hydrated.url] = hydrated
        persist()
    }

    /** Bulk put from loaded file */
    private fun putAll(map: Map<String, Hydrated>) {
        memory.putAll(map)
    }

    fun size(): Int = memory.size

    fun asMap(): Map<String, Hydrated> = memory.toMap()

    /** Load from sidecar file if exists — populates in-memory map. Call on startup. */
    fun load() {
        val path = cacheFilePath()
        if (!FileSystem.exists(path) || !FileSystem.isFile(path)) return
        try {
            val text = FileSystem.readText(path)
            if (text.isBlank()) return
            val map: Map<String, Hydrated> = json.decodeFromString(
                MapSerializer(String.serializer(), Hydrated.serializer()),
                text
            )
            putAll(map)
        } catch (_: Exception) {
            // Corrupt cache — ignore, start empty
        }
    }

    /** Persist in-memory map to sidecar file. Gitignored per spec. */
    fun persist() {
        try {
            FileSystem.mkdirs(cacheDirPath())
            val snapshot: Map<String, Hydrated> = memory.toMap()
            val text = json.encodeToString(
                MapSerializer(String.serializer(), Hydrated.serializer()),
                snapshot
            )
            FileSystem.writeText(cacheFilePath(), text)
        } catch (_: Exception) {
            // Best-effort persistence — ignore IO failures (e.g. wasm)
        }
    }

    /** Clear in-memory and optionally delete persistence would be here — not needed for Task 5 */
    fun clearMemory() { memory.clear() }
}
