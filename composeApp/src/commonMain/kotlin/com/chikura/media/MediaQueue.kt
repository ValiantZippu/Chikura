package com.chikura.media

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PlayMode { NORMAL, REPEAT_ALL, REPEAT_ONE, SHUFFLE }

data class QueueItem(
    val videoId: String,
    val title: String = "",
    val author: String = "",
    val thumbnailUrl: String = youtubeThumbUrl(videoId),
    val durationText: String = ""
)

/**
 * Persistent media queue with play modes (normal, repeat all, repeat one, shuffle).
 * Backs the mini player and any future queue panel.
 */
object MediaQueue {
    private val _items = MutableStateFlow<List<QueueItem>>(emptyList())
    val items: StateFlow<List<QueueItem>> = _items.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _playMode = MutableStateFlow(PlayMode.NORMAL)
    val playMode: StateFlow<PlayMode> = _playMode.asStateFlow()

    val current: QueueItem? get() {
        val idx = _currentIndex.value
        val list = _items.value
        return if (idx in list.indices) list[idx] else null
    }

    val hasNext: Boolean get() {
        val list = _items.value
        val idx = _currentIndex.value
        if (list.isEmpty()) return false
        return when (_playMode.value) {
            PlayMode.REPEAT_ALL -> true
            PlayMode.REPEAT_ONE -> true
            PlayMode.SHUFFLE -> list.size > 1
            PlayMode.NORMAL -> idx < list.size - 1
        }
    }

    val hasPrevious: Boolean get() {
        val list = _items.value
        val idx = _currentIndex.value
        if (list.isEmpty()) return false
        return when (_playMode.value) {
            PlayMode.REPEAT_ALL -> true
            PlayMode.REPEAT_ONE -> true
            PlayMode.SHUFFLE -> list.size > 1
            PlayMode.NORMAL -> idx > 0
        }
    }

    /** Add a video to the queue (end). If queue is empty, starts playing. */
    fun add(item: QueueItem) {
        val list = _items.value.toMutableList()
        list.add(item)
        _items.value = list
        if (_currentIndex.value < 0) _currentIndex.value = 0
    }

    /** Add and immediately play a video. */
    fun playNow(item: QueueItem) {
        val list = _items.value.toMutableList()
        // Don't duplicate if already in queue
        val existing = list.indexOfFirst { it.videoId == item.videoId }
        if (existing >= 0) {
            _currentIndex.value = existing
        } else {
            list.add(item)
            _items.value = list
            _currentIndex.value = list.size - 1
        }
    }

    /** Insert after current and play next. */
    fun playNext(item: QueueItem) {
        val list = _items.value.toMutableList()
        val idx = _currentIndex.value
        val insertAt = (idx + 1).coerceAtMost(list.size)
        list.add(insertAt, item)
        _items.value = list
    }

    fun next(): QueueItem? {
        val list = _items.value
        if (list.isEmpty()) return null
        val nextIdx = when (_playMode.value) {
            PlayMode.REPEAT_ONE -> _currentIndex.value
            PlayMode.SHUFFLE -> {
                if (list.size <= 1) 0
                else {
                    var r: Int
                    do { r = list.indices.random() } while (r == _currentIndex.value)
                    r
                }
            }
            PlayMode.REPEAT_ALL -> (_currentIndex.value + 1) % list.size
            PlayMode.NORMAL -> {
                val next = _currentIndex.value + 1
                if (next >= list.size) return null
                next
            }
        }
        _currentIndex.value = nextIdx
        return current
    }

    fun previous(): QueueItem? {
        val list = _items.value
        if (list.isEmpty()) return null
        val prevIdx = when (_playMode.value) {
            PlayMode.REPEAT_ONE -> _currentIndex.value
            PlayMode.SHUFFLE -> {
                if (list.size <= 1) 0
                else {
                    var r: Int
                    do { r = list.indices.random() } while (r == _currentIndex.value)
                    r
                }
            }
            PlayMode.REPEAT_ALL -> {
                val prev = _currentIndex.value - 1
                if (prev < 0) list.size - 1 else prev
            }
            PlayMode.NORMAL -> {
                val prev = _currentIndex.value - 1
                if (prev < 0) return null
                prev
            }
        }
        _currentIndex.value = prevIdx
        return current
    }

    fun setPlayMode(mode: PlayMode) {
        _playMode.value = mode
    }

    fun cyclePlayMode() {
        _playMode.value = when (_playMode.value) {
            PlayMode.NORMAL -> PlayMode.REPEAT_ALL
            PlayMode.REPEAT_ALL -> PlayMode.REPEAT_ONE
            PlayMode.REPEAT_ONE -> PlayMode.SHUFFLE
            PlayMode.SHUFFLE -> PlayMode.NORMAL
        }
    }

    fun removeAt(index: Int) {
        val list = _items.value.toMutableList()
        if (index !in list.indices) return
        list.removeAt(index)
        _items.value = list
        // Adjust current index
        val cur = _currentIndex.value
        if (list.isEmpty()) {
            _currentIndex.value = -1
        } else if (index < cur) {
            _currentIndex.value = cur - 1
        } else if (index == cur && cur >= list.size) {
            _currentIndex.value = list.size - 1
        }
    }

    fun clear() {
        _items.value = emptyList()
        _currentIndex.value = -1
    }

    fun move(fromIndex: Int, toIndex: Int) {
        val list = _items.value.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _items.value = list
        // Adjust current index
        val cur = _currentIndex.value
        _currentIndex.value = when {
            cur == fromIndex -> toIndex
            fromIndex < cur && toIndex >= cur -> cur - 1
            fromIndex > cur && toIndex <= cur -> cur + 1
            else -> cur
        }
    }
}
