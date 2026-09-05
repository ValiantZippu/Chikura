package com.chikura.media

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * JVM implementation of DownloadManager.
 * Streams to ~/Downloads/Chikura/ with progress tracking.
 * For MP3: downloads best audio stream and converts via ffmpeg if available.
 */
actual object DownloadManager {
    private val _items = MutableStateFlow<List<DownloadItem>>(emptyList())
    actual val items: StateFlow<List<DownloadItem>> = _items

    private var nextId = 0

    private fun downloadsDir(): File {
        val home = System.getProperty("user.home")
        val dir = File(home, "Downloads/Chikura")
        dir.mkdirs()
        return dir
    }

    actual fun downloadUrl(videoId: String, title: String, format: DownloadFormat) {
        val id = "dl-${nextId++}"
        val item = DownloadItem(id = id, videoId = videoId, title = title, format = format)
        _items.value = _items.value + item
        processDownload(item)
    }

    private fun processDownload(item: DownloadItem) {
        Thread {
            runCatching {
                updateItem(item.id, state = DownloadState.Fetching, progress = 0f)

                val safeName = item.title.replace(Regex("[^a-zA-Z0-9_\\- .]"), "_").take(80)
                val dir = downloadsDir()

                if (item.format.isAudioOnly) {
                    downloadAudio(item, dir, safeName)
                } else {
                    downloadVideo(item, dir, safeName)
                }
            }.onFailure { e ->
                updateItem(item.id, state = DownloadState.Error(e.message ?: "Unknown error"))
            }
        }.start()
    }

    private fun downloadVideo(item: DownloadItem, dir: File, safeName: String) {
        val ext = item.format.container
        val outFile = File(dir, "$safeName.$ext")
        updateItem(item.id, state = DownloadState.Downloading, progress = 0f)

        downloadToFile(item.format.url, outFile) { downloaded, total ->
            val prog = if (total > 0) downloaded.toFloat() / total else 0f
            updateItem(item.id, progress = prog, bytesDownloaded = downloaded, totalBytes = total)
        }

        val size = outFile.length()
        updateItem(item.id, state = DownloadState.Done(outFile.absolutePath, size))
    }

    private fun downloadAudio(item: DownloadItem, dir: File, safeName: String) {
        val rawFile = File(dir, "$safeName.raw_audio")
        updateItem(item.id, state = DownloadState.Downloading, progress = 0f)

        downloadToFile(item.format.url, rawFile) { downloaded, total ->
            val prog = if (total > 0) downloaded.toFloat() / total else 0f
            updateItem(item.id, progress = prog, bytesDownloaded = downloaded, totalBytes = total)
        }

        // Try to convert to MP3 with ffmpeg
        val ffmpegPath = findFfmpeg()
        if (ffmpegPath != null) {
            updateItem(item.id, state = DownloadState.Converting("Converting to MP3..."))
            val mp3File = File(dir, "$safeName.mp3")
            val success = convertToMp3(ffmpegPath, rawFile, mp3File)
            rawFile.delete()
            if (success) {
                val size = mp3File.length()
                updateItem(item.id, state = DownloadState.Done(mp3File.absolutePath, size))
                return
            }
        }

        // Fallback: rename raw to m4a
        val m4aFile = File(dir, "$safeName.m4a")
        rawFile.renameTo(m4aFile)
        val size = m4aFile.length()
        updateItem(item.id, state = DownloadState.Done(m4aFile.absolutePath, size))
    }

    private fun downloadToFile(url: String, outFile: File, onProgress: (Long, Long) -> Unit) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/126.0 Safari/537.36")
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        conn.connect()

        val total = conn.contentLength.toLong()
        val input = conn.inputStream
        val output = FileOutputStream(outFile)

        val buffer = ByteArray(64 * 1024)
        var downloaded = 0L
        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
            output.write(buffer, 0, read)
            downloaded += read
            onProgress(downloaded, total)
        }
        output.flush()
        output.close()
        input.close()
        conn.disconnect()
    }

    private fun findFfmpeg(): String? {
        val candidates = listOf("ffmpeg", "/usr/bin/ffmpeg", "/usr/local/bin/ffmpeg",
            "C:\\ffmpeg\\bin\\ffmpeg.exe", "/opt/homebrew/bin/ffmpeg")
        for (cmd in candidates) {
            try {
                val proc = ProcessBuilder(cmd, "-version")
                    .redirectErrorStream(true)
                    .start()
                val exited = proc.waitFor()
                if (exited == 0) return cmd
            } catch (_: Exception) { }
        }
        return null
    }

    private fun convertToMp3(ffmpeg: String, input: File, output: File): Boolean {
        return try {
            val proc = ProcessBuilder(
                ffmpeg, "-y", "-i", input.absolutePath,
                "-vn", "-acodec", "libmp3lame", "-q:a", "0",
                output.absolutePath
            ).redirectErrorStream(true).start()
            proc.waitFor() == 0 && output.exists() && output.length() > 0
        } catch (_: Exception) {
            false
        }
    }

    private fun updateItem(
        id: String,
        state: DownloadState? = null,
        progress: Float? = null,
        bytesDownloaded: Long? = null,
        totalBytes: Long? = null
    ) {
        _items.value = _items.value.map {
            if (it.id == id) it.copy(
                state = state ?: it.state,
                progress = progress ?: it.progress,
                bytesDownloaded = bytesDownloaded ?: it.bytesDownloaded,
                totalBytes = totalBytes ?: it.totalBytes
            ) else it
        }
    }

    actual fun removeItem(id: String) {
        _items.value = _items.value.filter { it.id != id }
    }

    actual fun clearCompleted() {
        _items.value = _items.value.filter { it.state !is DownloadState.Done && it.state !is DownloadState.Error }
    }
}
