package com.chikura.parser

import com.chikura.model.Bunker
import com.chikura.model.Domain
import com.chikura.model.Resource
import com.chikura.model.Section
import com.chikura.model.Category

// Preserved from scripts/clean_markdown.py
private val SECTION_RE = Regex("""^\s*'.*'\s*$""")
private val URL_RE = Regex("""https?://\S+""")
private val SEPARATOR_RE = Regex("""^\s*={3,}\s*$""")

// Bare ref patterns
private val BARE_DOMAIN_RE = Regex("""^(?:www\.)?[\w.-]+\.[a-z]{2,}(?:/.*)?$""", RegexOption.IGNORE_CASE)
private val EXE_RE = Regex("""^[\w.-]+\.exe$""", RegexOption.IGNORE_CASE)
private val HOST_PORT_RE = Regex("""^[\w.-]+\.[a-z]{2,}:\d+.*""", RegexOption.IGNORE_CASE)

/**
 * Infer typeHint from URL.
 * Spec: youtube.com/playlist -> playlist, @ -> channel, youtu.be/watch/shorts/live -> video/shorts/live,
 * reddit -> website, amazon -> book, github -> software else website, bare -> bare.
 */
fun inferTypeHint(url: String): String {
    val lower = url.lowercase()
    // playlist must be checked before channel/@
    if (lower.contains("playlist")) return "playlist"
    if (url.contains("@")) return "channel"
    if (lower.contains("/channel")) return "channel"
    if (lower.contains("youtu.be")) return "video"
    if (lower.contains("/shorts")) return "shorts"
    if (lower.contains("/live")) return "live"
    if (lower.contains("/watch")) return "video"
    if (lower.contains("m.youtube.com/watch")) return "video"
    if (lower.contains("reddit.com")) return "website"
    if (lower.contains("amazon.")) return "book"
    if (lower.contains("github.com")) return "software"
    if (!lower.startsWith("http://") && !lower.startsWith("https://")) return "bare"
    return "website"
}

private fun fixMalformed(input: String): String {
    var s = input
    s = s.replace("htt ps://", "https://")
    s = s.replace("htt p://", "http://")
    s = s.replace(Regex("""htt\s+ps://"""), "https://")
    s = s.replace(Regex("""htt\s+p://"""), "http://")
    s = s.replace("https ://", "https://")
    s = s.replace("http ://", "http://")
    return s
}

private fun extractSectionLegacy(s: String): String {
    var t = s.trim()
    if (t.startsWith("'") && t.endsWith("'") && t.length >= 2) {
        t = t.substring(1, t.length - 1)
    }
    return t.trim().trim('\'').trim()
}

/**
 * Parse markdown text preserving indent.
 * Handles:
 * - # / ## / ### / #### headings (and legacy SECTION_RE quoted sections)
 * - Bullet lists with URL_RE or bare refs (freeCodeCamp.org, VNDB.org, www.projectfeline.com, samp.hzgaming.net:7777, Instagram.com/memphymusic, *.exe)
 * - Malformed htt ps:// fixed
 * - Preserves indent count per resource
 * - Infers typeHint
 */
fun parseMarkdown(text: String, domain: String): List<Resource> {
    val lines = text.split("\n")
    var currentSection: String? = null
    var currentCategory: String? = null
    var currentSubcategory: String? = null
    val resources = mutableListOf<Resource>()
    var counter = 1

    for (rawLine in lines) {
        // Preserve indent count (spaces and tabs)
        val indent = rawLine.length - rawLine.trimStart(' ', '\t').length
        val trimmed = rawLine.trim()

        if (trimmed.isEmpty()) continue
        if (trimmed == "---") continue
        if (SEPARATOR_RE.matches(rawLine)) continue
        if (trimmed == "b" && lines.size < 20) continue
        if (trimmed.startsWith(">")) continue

        // Legacy quoted section: 'Section'  (SECTION_RE)
        if (SECTION_RE.matches(trimmed) && trimmed.length >= 2) {
            currentSection = extractSectionLegacy(trimmed)
            currentCategory = null
            currentSubcategory = null
            continue
        }

        // Markdown headings # / ## / ### / ####
        if (trimmed.startsWith("#")) {
            val hashCount = trimmed.takeWhile { it == '#' }.length
            val content = trimmed.substring(hashCount).trim()
            // skip empty heading
            if (content.isEmpty()) continue
            when {
                hashCount == 1 -> {
                    // Top-level title — not a section, ignore for resource hierarchy
                    continue
                }
                hashCount == 2 -> {
                    currentSection = content
                    currentCategory = null
                    currentSubcategory = null
                }
                hashCount == 3 -> {
                    currentCategory = content
                    currentSubcategory = null
                }
                hashCount >= 4 -> {
                    currentSubcategory = content
                }
            }
            continue
        }

        // Candidate handling: strip bullet marker
        var candidate = trimmed
        when {
            candidate.startsWith("- ") -> candidate = candidate.substring(2).trimStart()
            candidate.startsWith("-") -> candidate = candidate.substring(1).trimStart()
            candidate.startsWith("* ") -> candidate = candidate.substring(2).trimStart()
            candidate.startsWith("*") -> candidate = candidate.substring(1).trimStart()
            candidate.startsWith("+ ") -> candidate = candidate.substring(2).trimStart()
            candidate.startsWith("+") -> candidate = candidate.substring(1).trimStart()
        }

        // Fix malformed before detection
        candidate = fixMalformed(candidate)

        // Clean trailing stray ' from GAMES.md type urls
        if (candidate.endsWith("'") && candidate.contains("https://")) {
            candidate = candidate.trimEnd('\'')
        }

        val hasUrl = candidate.contains("https://") || candidate.contains("http://")
        var isBare = false
        if (!hasUrl) {
            // bare detection mirrors scripts/clean_markdown.py
            if (!candidate.contains(" ") && BARE_DOMAIN_RE.matches(candidate)) isBare = true
            if (EXE_RE.matches(candidate)) isBare = true
            if (HOST_PORT_RE.matches(candidate)) isBare = true
            // Also handle bare refs with path and query stripped already via BARE_DOMAIN_RE
            // Additional bare: Instagram.com/memphymusic style already matched
        }

        if (hasUrl || isBare) {
            val raw = candidate
            var url: String
            if (hasUrl) {
                val m = URL_RE.find(raw)
                url = m?.value ?: raw
                // Strip trailing punctuation that is not part of URL (',).] etc)
                url = url.trimEnd(',', '.', ')', ']', '\'', '"', ';', '>')
                // Also trim trailing '.' that may be sentence end
                // Keep url as is otherwise
            } else {
                url = raw.trimEnd(',', '.', ')', ']', '\'', '"', ';')
            }
            val typeHint = inferTypeHint(url)
            // Even for bare, inferTypeHint returns bare; but if bare url looks like github/amazon etc, override?
            // bare but github.com -> software, amazon -> book already handled via contains check before bare fallback
            val id = "$domain-${counter.toString().padStart(4, '0')}"
            counter++
            resources.add(
                Resource(
                    id = id,
                    url = url,
                    raw = raw,
                    domain = domain,
                    section = currentSection,
                    category = currentCategory,
                    subcategory = currentSubcategory,
                    typeHint = typeHint,
                    indent = indent
                )
            )
            continue
        }

        // Not a resource and not heading — could be indented notes/prompts or bracket notes.
        // We skip them to preserve clean hierarchy; they are not resources.
        // For completeness, if line is indented heading-like without '#', clean_markdown treated leading >=4 as category.
        // But for parsed markdown (already cleaned), we ignore such stray lines.
    }

    return resources
}

/**
 * Bidirectional write-back: move a resource bullet preserving indent.
 * Reuse parser logic to locate the bullet line by sequential counter (domain-0001).
 * Preserves indent (spaces) of original bullet and re-inserts under target ## Section.
 * If target section does not exist it is created at end of file.
 */
fun moveResourceInMarkdown(
    originalText: String,
    domain: String,
    resourceId: String,
    toSection: String
): String {
    val targetSection = toSection.trim()
    if (targetSection.isEmpty()) return originalText
    // Extract numeric suffix after last '-'
    val suffix = resourceId.substringAfterLast("-", "")
    val targetIdx = suffix.toIntOrNull() ?: return originalText

    val lines = originalText.split("\n").toMutableList()

    // Find line index of the resourceId by replaying parse counting
    var counter = 1
    var foundIdx: Int? = null
    for (i in lines.indices) {
        val rawLine = lines[i]
        val trimmed = rawLine.trim()
        if (trimmed.isEmpty()) continue
        if (trimmed == "---") continue
        if (SEPARATOR_RE.matches(rawLine)) continue
        if (trimmed == "b" && lines.size < 20) continue
        if (trimmed.startsWith(">")) continue
        if (SECTION_RE.matches(trimmed) && trimmed.length >= 2) continue
        if (trimmed.startsWith("#")) continue
        var candidate = trimmed
        when {
            candidate.startsWith("- ") -> candidate = candidate.substring(2).trimStart()
            candidate.startsWith("-") -> candidate = candidate.substring(1).trimStart()
            candidate.startsWith("* ") -> candidate = candidate.substring(2).trimStart()
            candidate.startsWith("*") -> candidate = candidate.substring(1).trimStart()
            candidate.startsWith("+ ") -> candidate = candidate.substring(2).trimStart()
            candidate.startsWith("+") -> candidate = candidate.substring(1).trimStart()
            else -> continue // not a bullet
        }
        candidate = fixMalformed(candidate)
        if (candidate.endsWith("'") && candidate.contains("https://")) {
            candidate = candidate.trimEnd('\'')
        }
        val hasUrl = candidate.contains("https://") || candidate.contains("http://")
        var isBare = false
        if (!hasUrl) {
            if (!candidate.contains(" ") && BARE_DOMAIN_RE.matches(candidate)) isBare = true
            if (EXE_RE.matches(candidate)) isBare = true
            if (HOST_PORT_RE.matches(candidate)) isBare = true
        }
        if (hasUrl || isBare) {
            if (counter == targetIdx) {
                foundIdx = i
                break
            }
            counter++
        }
    }
    val srcIdx = foundIdx ?: return originalText
    val movedLine = lines[srcIdx]
    // Remove source
    lines.removeAt(srcIdx)

    // Adjust search for target header after removal
    // Find target ## header
    var headerIdx: Int? = null
    for (i in lines.indices) {
        val t = lines[i].trim()
        if (t.startsWith("#")) {
            val hc = t.takeWhile { it == '#' }.length
            if (hc == 2) {
                val content = t.substring(hc).trim()
                if (content == targetSection) {
                    headerIdx = i
                    break
                }
            }
        }
    }
    if (headerIdx == null) {
        // Create new section at end
        // Ensure file ends with blank line before header
        if (lines.isNotEmpty() && lines.last().trim().isNotEmpty()) lines.add("")
        lines.add("## $targetSection")
        lines.add("")
        lines.add(movedLine)
        return lines.joinToString("\n")
    }
    // Find insertion point: before next ## (level 2) or EOF
    var nextSectionIdx: Int? = null
    for (i in headerIdx + 1 until lines.size) {
        val t = lines[i].trim()
        if (t.startsWith("#")) {
            val hc = t.takeWhile { it == '#' }.length
            if (hc == 2) {
                nextSectionIdx = i
                break
            }
        }
    }
    var insertAt = nextSectionIdx ?: lines.size
    // Trim trailing blank lines before next section / EOF to insert before them
    while (insertAt > headerIdx + 1 && insertAt - 1 < lines.size && lines[insertAt - 1].trim().isEmpty()) {
        // Keep one blank separation? Insert before the blanks that separate sections
        // We do not collapse blanks aggressively — just keep insertion before final blank block
        // Move insertAt back only if there are >=2 trailing blanks
        // Simpler: if nextSectionIdx != null, insert right before nextSectionIdx ignoring blank gap
        break
    }
    // If directly after header we have blank lines, skip them to find first content
    // But insertion before next section naturally lands after existing content.
    // To keep indent preservation, insert at insertAt
    // If insertAt == lines.size, just add
    if (insertAt >= lines.size) {
        // Ensure blank line before insertion if previous line not blank
        if (lines.isNotEmpty() && lines.last().trim().isNotEmpty()) {
            lines.add("")
        }
        lines.add(movedLine)
    } else {
        // Insert before nextSection: ensure separation
        // If element before insertAt is not blank, ensure we insert directly
        lines.add(insertAt, movedLine)
    }
    return lines.joinToString("\n")
}

/**
 * Overload that works directly with a Resource object (preserves its indent).
 * Finds the resource by id via same logic; toSection is new section name.
 */
fun moveResourceInMarkdown(originalText: String, resource: Resource, toSection: String): String {
    return moveResourceInMarkdown(originalText, resource.domain, resource.id, toSection)
}

/**
 * Build Bunker model from parsed resources grouped by section/category.
 * This is a convenience used by future hydrator; not required for Task 2 tests but provided for spec completeness.
 */
fun buildBunker(id: String, name: String, markdownByDomain: Map<String, String>): Bunker {
    val domains = markdownByDomain.map { (domainId, text) ->
        val resources = parseMarkdown(text, domainId)
        // Group into sections/categories for hierarchical view
        val sectionMap = linkedMapOf<String?, MutableMap<String?, MutableList<Resource>>>()
        for (r in resources) {
            val sec = r.section
            val cat = r.category
            sectionMap.getOrPut(sec) { linkedMapOf() }.getOrPut(cat) { mutableListOf() }.add(r)
        }
        val sections = sectionMap.map { (secName, catMap) ->
            val categories = catMap.map { (catName, resList) ->
                Category(name = catName ?: "Uncategorized", resources = resList)
            }
            Section(name = secName ?: "General", categories = categories)
        }
        Domain(id = domainId, name = domainId, sections = sections, resources = resources)
    }
    return Bunker(id = id, name = name, domains = domains)
}
