package com.chikura.ui.board

import com.chikura.model.Bunker
import com.chikura.model.Category
import com.chikura.model.Resource
import com.chikura.model.Section
import com.chikura.parser.moveResourceInMarkdown
import com.chikura.parser.parseMarkdown
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Task 6: BoardViewModel — Kanban drag + Whiteboard nodes + bidirectional write-back.
 *
 * Holds bunker state and markdownByDomain sidecar. `move(resourceId, toSection)`
 * rewrites the underlying .md file preserving indent via BunkerParser writer,
 * re-parses to update in-memory Bunker, and optionally persists via [onWrite].
 *
 * Whiteboard support: nodes as boxes (ResourceCard), edges as lines, connect nodes.
 */
data class WhiteboardEdge(val fromId: String, val toId: String)

class BoardViewModel(
    initialBunker: Bunker,
    markdownByDomain: Map<String, String>,
    private val onWrite: ((domainId: String, newText: String) -> Unit)? = null
) {
    private val markdownMap: MutableMap<String, String> = markdownByDomain.toMutableMap()

    private val _bunker = MutableStateFlow(initialBunker)
    val bunker: StateFlow<Bunker> = _bunker.asStateFlow()

    // Synchronous accessor for tests: vm.bunker.value or vm.getBunker()
    fun getBunker(): Bunker = _bunker.value

    // Whiteboard edges state
    private val _edges = MutableStateFlow<List<WhiteboardEdge>>(emptyList())
    val edges: StateFlow<List<WhiteboardEdge>> = _edges.asStateFlow()

    fun find(resourceId: String): Resource? =
        _bunker.value.domains.flatMap { it.resources }.find { it.id == resourceId }

    fun getMarkdown(domainId: String): String = markdownMap[domainId] ?: ""

    fun allMarkdown(): Map<String, String> = markdownMap.toMap()

    /**
     * Move resource between ## Section — preserves indent.
     * Rewrites markdown via [moveResourceInMarkdown], re-parses, updates [bunker] flow,
     * and calls [onWrite] for file persistence (bidirectional write-back).
     *
     * Test spec: vm.move("music-0001", "Music Theory") asserts section changed.
     */
    fun move(resourceId: String, toSection: String) {
        val resource = find(resourceId) ?: return
        val domainId = resource.domain
        val originalText = markdownMap[domainId] ?: return
        val newText = moveResourceInMarkdown(originalText, domainId, resourceId, toSection)
        if (newText == originalText) return
        markdownMap[domainId] = newText
        onWrite?.invoke(domainId, newText)

        // Re-parse domain's markdown to rebuild Resource list and sections
        val newResources = parseMarkdown(newText, domainId)
        val updatedDomains = _bunker.value.domains.map { domain ->
            if (domain.id == domainId) {
                rebuildDomain(domain, newResources)
            } else domain
        }
        // If domain not present in bunker (e.g. new domain file), create it
        val domainExists = _bunker.value.domains.any { it.id == domainId }
        val finalDomains = if (!domainExists) {
            updatedDomains + rebuildDomain(
                com.chikura.model.Domain(id = domainId, name = domainId),
                newResources
            )
        } else updatedDomains

        _bunker.value = _bunker.value.copy(domains = finalDomains)
    }

    private fun rebuildDomain(
        domain: com.chikura.model.Domain,
        resources: List<Resource>
    ): com.chikura.model.Domain {
        val sectionMap = linkedMapOf<String?, MutableMap<String?, MutableList<Resource>>>()
        for (r in resources) {
            sectionMap.getOrPut(r.section) { linkedMapOf() }
                .getOrPut(r.category) { mutableListOf() }.add(r)
        }
        val sections = sectionMap.map { (secName, catMap) ->
            val categories = catMap.map { (catName, list) ->
                Category(name = catName ?: "Uncategorized", resources = list)
            }
            Section(name = secName ?: "General", categories = categories)
        }
        return domain.copy(sections = sections, resources = resources)
    }

    // Whiteboard: connect nodes (edges as lines)
    fun connect(fromId: String, toId: String) {
        if (fromId == toId) return
        if (fromId.isBlank() || toId.isBlank()) return
        val exists = _edges.value.any {
            (it.fromId == fromId && it.toId == toId) || (it.fromId == toId && it.toId == fromId)
        }
        if (exists) return
        // validate both ids exist in bunker
        if (find(fromId) == null || find(toId) == null) return
        _edges.value = _edges.value + WhiteboardEdge(fromId, toId)
    }

    fun disconnect(fromId: String, toId: String) {
        _edges.value = _edges.value.filterNot {
            (it.fromId == fromId && it.toId == toId) || (it.fromId == toId && it.toId == fromId)
        }
    }

    fun clearEdges() {
        _edges.value = emptyList()
    }
}
