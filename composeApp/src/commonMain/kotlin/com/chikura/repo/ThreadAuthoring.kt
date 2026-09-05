package com.chikura.repo

import com.chikura.platform.FileSystem

// Megathread authoring: servers ARE megathreads, so the app creates them.
// Writes real files via FileSystem (desktop), then the caller reloads.
// All names are kebab-case per CHIKUTHREAD-SPEC.md.

fun threadDirCandidates(): List<String> = listOf(
    "ChikuThreads/ValiantZippu/ChikuThread 1",
    "ChikuThreads/ValiantZippu/ChikuThread_1",
    "../ChikuThreads/ValiantZippu/ChikuThread 1",
    "composeApp/src/commonTest/resources/sample-chikuthread"
)

/** First candidate that exists and holds markdown, or null. */
fun resolveThreadDir(): String? {
    for (base in threadDirCandidates()) {
        try {
            if (!FileSystem.exists(base) || !FileSystem.isDirectory(base)) continue
            val hasMd = FileSystem.listFiles(base).any { it.endsWith(".md") }
            if (hasMd) return base
        } catch (_: Exception) {
        }
    }
    return null
}

/** ChikuThreads root derived from a thread dir, or null. */
fun chikuThreadsRoot(threadDir: String): String? {
    val idx = threadDir.replace('\\', '/').indexOf("ChikuThreads/")
    if (idx < 0) return null
    return threadDir.substring(0, idx) + "ChikuThreads"
}

fun kebabCase(raw: String): String =
    raw.trim().lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')

fun isValidDomainId(id: String): Boolean =
    id.isNotEmpty() && id.length <= 48 && Regex("^[a-z0-9]+(-[a-z0-9]+)*$").matches(id)

fun domainTemplate(title: String): String = buildString {
    appendLine("<!-- ChikuThread domain file: kebab-case name, one # title. -->")
    appendLine("<!-- Hierarchy: # domain, ## section (columns), ### category, - url, > note. -->")
    appendLine("<!-- Keep raw URLs verbatim. Never store thumbnails/titles here (sidecar does). -->")
    appendLine()
    appendLine("# $title")
    appendLine()
    appendLine("## Getting Started")
    appendLine()
    appendLine("### Essentials")
    appendLine()
    appendLine("- https://example.com/start-here — replace with your first link")
    appendLine()
    appendLine("> Why this is good: replace with your note, or delete this line.")
    appendLine()
    appendLine("## Inbox Triage")
    appendLine()
    appendLine("- paste bulk links here, then drag each card to its section in Chikura")
    appendLine()
}

fun starterThreadFiles(threadName: String, author: String): Map<String, String> = mapOf(
    "thread.json" to "{\n  \"name\": \"$threadName\",\n  \"version\": \"1.0\",\n  \"description\": \"Curated by $author\",\n  \"author\": \"$author\"\n}\n",
    "archive-box/inbox.md" to "# Inbox\n\n<!-- Bulk intake: paste links below, one per line, no formatting needed. -->\n<!-- Triage in Chikura, then commit. Never delete — quarantine instead. -->\n",
    "archive-box/quarantine.md" to "# Quarantine\n\n<!-- Unsure, dead, or duplicate links go here with a > why note. -->\n",
    "start-here.md" to domainTemplate("Start Here")
)

val authoringGuideSteps: List<Pair<String, String>> = listOf(
    "Expand" to "Paste bulk links into archive-box/inbox.md — no formatting needed.",
    "Triage" to "Inbox renders as cards. Drag each to its domain file and ## section (created on the fly).",
    "Quarantine" to "Unsure, dead, or duplicate? Move to archive-box/quarantine.md with a > why note. Never delete to look cleaner.",
    "Structure" to "# domain title, ## section, ### category, - https://url verbatim, > notes. Indent is hierarchy.",
    "Publish" to "git commit shows one clean line per moved link. Push — followers pull, no re-review needed."
)

sealed interface AuthorResult {
    data class Ok(val path: String) : AuthorResult
    data class Err(val message: String) : AuthorResult
}

fun createDomainFile(threadDir: String, rawName: String): AuthorResult {
    val id = kebabCase(rawName)
    if (!isValidDomainId(id)) return AuthorResult.Err("Use letters, numbers, dashes (e.g. vocal-mixing).")
    val path = "$threadDir/$id.md"
    return try {
        if (FileSystem.exists(path)) return AuthorResult.Err("$id.md already exists — open it instead.")
        val title = id.split("-").joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
        FileSystem.writeText(path, domainTemplate(title))
        AuthorResult.Ok(path)
    } catch (e: Exception) {
        AuthorResult.Err("Write failed: ${(e.message ?: e.toString()).take(120)}")
    }
}

fun createThread(root: String, rawAuthor: String, rawThread: String): AuthorResult {
    val author = kebabCase(rawAuthor)
    val thread = kebabCase(rawThread)
    if (!isValidDomainId(author)) return AuthorResult.Err("Author: letters, numbers, dashes.")
    if (!isValidDomainId(thread)) return AuthorResult.Err("Thread: letters, numbers, dashes.")
    return try {
        val dir = "$root/$author/$thread"
        if (FileSystem.exists("$dir/thread.json") || FileSystem.exists("$dir/start-here.md")) {
            return AuthorResult.Err("Thread already exists — load it instead.")
        }
        FileSystem.mkdirs("$dir/archive-box")
        for ((rel, content) in starterThreadFiles(thread, author)) {
            FileSystem.writeText("$dir/$rel", content)
        }
        AuthorResult.Ok(dir)
    } catch (e: Exception) {
        AuthorResult.Err("Write failed: ${(e.message ?: e.toString()).take(120)}")
    }
}
