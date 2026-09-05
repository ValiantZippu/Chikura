package com.chikura.media

import com.chikura.platform.FileSystem
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

// Per-video curation state (sidecar, gitignored): watched flag + resume
// position + note. Backs megathread triage: what you watched, where you
// stopped, what to do next — without touching the .md.

@Serializable
data class VideoProgress(
    val watched: Boolean = false,
    val positionSec: Int = 0,
    val note: String = ""
)

object PlaybackStore {
    private val memory: MutableMap<String, VideoProgress> = mutableMapOf()
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private var loaded = false

    private fun filePath(): String {
        val root = FileSystem.homeChikuThreadsPath().trimEnd('/', '\\')
        return "$root/default/.chikura-cache/playback.json"
    }

    private fun dirPath(): String {
        val root = FileSystem.homeChikuThreadsPath().trimEnd('/', '\\')
        return "$root/default/.chikura-cache"
    }

    fun load() {
        if (loaded) return
        loaded = true
        try {
            val path = filePath()
            if (!FileSystem.exists(path) || !FileSystem.isFile(path)) return
            val text = FileSystem.readText(path)
            if (text.isBlank()) return
            val map: Map<String, VideoProgress> = json.decodeFromString(
                MapSerializer(String.serializer(), VideoProgress.serializer()), text
            )
            memory.putAll(map)
        } catch (_: Exception) {
        }
    }

    fun get(videoId: String): VideoProgress {
        load()
        return memory[videoId] ?: VideoProgress()
    }

    fun set(videoId: String, progress: VideoProgress) {
        load()
        memory[videoId] = progress
        persist()
    }

    private fun persist() {
        try {
            FileSystem.mkdirs(dirPath())
            val text = json.encodeToString(
                MapSerializer(String.serializer(), VideoProgress.serializer()),
                memory.toMap()
            )
            FileSystem.writeText(filePath(), text)
        } catch (_: Exception) {
        }
    }
}
