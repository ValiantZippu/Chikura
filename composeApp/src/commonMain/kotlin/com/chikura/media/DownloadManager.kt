package com.chikura.media

import kotlinx.coroutines.flow.StateFlow

/**
 * Common download state and item types.
 * Implementation is JVM-specific (file I/O + HTTP).
 */
sealed class DownloadState {
    data object Idle : DownloadState()
    data object Fetching : DownloadState()
    data object Downloading : DownloadState()
    data class Converting(val message: String = "Converting to MP3...") : DownloadState()
    data class Done(val filePath: String, val sizeBytes: Long = 0) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

data class DownloadItem(
    val id: String,
    val videoId: String,
    val title: String,
    val format: DownloadFormat,
    val state: DownloadState = DownloadState.Idle,
    val progress: Float = 0f,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0
)

expect object DownloadManager {
    val items: StateFlow<List<DownloadItem>>
    fun downloadUrl(videoId: String, title: String, format: DownloadFormat)
    fun removeItem(id: String)
    fun clearCompleted()
}

/** Platform-specific: fetch the YouTube watch page HTML and parse download formats. */
expect fun platformFetchDownloadInfo(videoId: String): DownloadInfo?
