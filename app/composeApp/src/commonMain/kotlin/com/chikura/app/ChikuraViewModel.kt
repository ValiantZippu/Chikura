package com.chikura.app

import com.chikura.model.Bunker
import com.chikura.model.Domain
import com.chikura.parser.buildBunker
import com.chikura.ui.board.BoardViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * View modes for the main content area.
 */
enum class ViewMode { LIST, KANBAN, WHITEBOARD }

/**
 * Core ViewModel — loads a ChikuThread (bunker) from a root directory,
 * manages selected domain and view mode.
 */
class ChikuraViewModel {
    private val _bunker = MutableStateFlow(Bunker(id = "", name = "", domains = emptyList()))
    val bunker: StateFlow<Bunker> = _bunker.asStateFlow()

    private val _selectedDomain = MutableStateFlow<Domain?>(null)
    val selectedDomain: StateFlow<Domain?> = _selectedDomain.asStateFlow()

    private val _currentView = MutableStateFlow(ViewMode.LIST)
    val currentView: StateFlow<ViewMode> = _currentView.asStateFlow()

    private val _threadPath = MutableStateFlow<String?>(null)
    val threadPath: StateFlow<String?> = _threadPath.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val markdownMap = mutableMapOf<String, String>()

    /** Callback set by desktop/web entry point to open a folder picker. */
    var requestOpenFolder: (() -> Unit)? = null

    fun getBoardViewModel(): BoardViewModel {
        val md = _selectedDomain.value?.let { domain ->
            mapOf(domain.id to (markdownMap[domain.id] ?: ""))
        } ?: emptyMap()
        return BoardViewModel(
            initialBunker = _bunker.value,
            markdownByDomain = md
        )
    }

    fun setView(view: ViewMode) { _currentView.value = view }

    fun selectDomain(domain: Domain) { _selectedDomain.value = domain }

    fun clearSelection() { _selectedDomain.value = null }

    /**
     * Load a ChikuThread from the given root path.
     * Reads all *.md files and archive-box/inbox.md.
     */
    fun loadThread(rootPath: String, readText: (String) -> String) {
        _isLoading.value = true
        _error.value = null
        _threadPath.value = rootPath
        markdownMap.clear()

        try {
            val mdFiles = findMarkdownFiles(rootPath, readText)
            if (mdFiles.isEmpty()) {
                _error.value = "No *.md files found in $rootPath"
                _bunker.value = Bunker(id = "empty", name = "Empty Thread", domains = emptyList())
                return
            }

            val markdownByDomain = linkedMapOf<String, String>()
            for ((domainId, text) in mdFiles) {
                markdownByDomain[domainId] = text
                markdownMap[domainId] = text
            }

            val threadName = rootPath.substringAfterLast("/").substringAfterLast("\\")
                .ifEmpty { "ChikuThread" }
            _bunker.value = buildBunker(
                id = threadName.lowercase().replace(" ", "-"),
                name = threadName,
                markdownByDomain = markdownByDomain
            )
            _selectedDomain.value = _bunker.value.domains.firstOrNull()
        } catch (e: Exception) {
            _error.value = "Failed to load: ${e.message}"
            _bunker.value = Bunker(id = "error", name = "Error", domains = emptyList())
        } finally {
            _isLoading.value = false
        }
    }

    private fun findMarkdownFiles(
        rootPath: String,
        readText: (String) -> String
    ): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        val results = listFiles(rootPath)

        for (filePath in results) {
            val name = filePath.substringAfterLast("/").substringAfterLast("\\")
            if (name.endsWith(".md") && name != "README.md") {
                val domainId = name.removeSuffix(".md")
                val text = readText(filePath)
                if (text.isNotBlank()) {
                    result.add(domainId to text)
                }
            }
        }

        // Also check archive-box/inbox.md
        val inboxPath = "$rootPath/archive-box/inbox.md"
        val inboxText = try { readText(inboxPath) } catch (_: Exception) { null }
        if (inboxText != null && inboxText.isNotBlank()) {
            result.add("archive-box-inbox" to inboxText)
        }

        return result
    }

    /** Platform-provided file listing — set by Desktop/Web entry point. */
    var listFiles: (String) -> List<String> = { emptyList() }
}
