package com.chikura.media

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// WasmJs stub — file downloads are not supported in the browser.
actual object DownloadManager {
    private val _items = MutableStateFlow<List<DownloadItem>>(emptyList())
    actual val items: StateFlow<List<DownloadItem>> = _items
    actual fun downloadUrl(videoId: String, title: String, format: DownloadFormat) {
        // No-op on wasm
    }
    actual fun removeItem(id: String) {}
    actual fun clearCompleted() {}
}
