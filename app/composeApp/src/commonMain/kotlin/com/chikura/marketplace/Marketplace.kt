package com.chikura.marketplace

import com.chikura.model.BunkerMeta
import com.chikura.model.Marketplace
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.json.Json

/**
 * Task 7: Git Marketplace & Add-by-URL + Web Wiki Read-Only Build
 *
 * - data class BunkerMeta etc (see model/Bunker.kt — @Serializable with marketplace fields)
 * - fetchMarketplace(): List<BunkerMeta> from marketplace.json (curated index), Json decode
 * - Validation: kebab-case *.md + archive-box/inbox.md exists
 * - Add by URL: validate GitHub URL, then git clone via KGit / fetch raw (desktop) or fetch raw (web)
 * - Web reads via raw.githubusercontent.com / GitHub API, not FS
 */

// Curated index — not an open store. Hosted as raw json in the KnowledgeBunker org.
// Desktop fallback can load local marketplace.json from classpath/resources if network fails.
const val MARKETPLACE_URL = "https://raw.githubusercontent.com/KnowledgeBunker/marketplace/main/marketplace.json"
const val MARKETPLACE_FALLBACK_URL = "https://raw.githubusercontent.com/KnowledgeBunker/KnowledgeBunker/main/marketplace.json"

// Shared Json for marketplace decode — ignoreUnknownKeys for forward-compat
val marketplaceJson = Json { ignoreUnknownKeys = true; isLenient = true; prettyPrint = false }

// --- kebab-case validation ---

private val KEBAB_MD_RE = Regex("^[a-z0-9]+(-[a-z0-9]+)*\\.md$")
private val GITHUB_URL_RE = Regex("^https://github\\.com/([^/]+)/([^/]+?)(?:\\.git)?/?$")

/**
 * Returns true if [fileName] is kebab-case *.md per bunker spec (e.g. music.md, gadget-electronics.md).
 * Rejects Book.md / TRASH.md / My File.md / archive-box/inbox.md is handled separately.
 */
fun isKebabCaseMd(fileName: String): Boolean {
    // fileName is basename only, no directory
    return KEBAB_MD_RE.matches(fileName)
}

/** Parsed GitHub repo coordinates. */
data class GithubRepo(val owner: String, val name: String) {
    val id: String get() = name.removeSuffix(".git")
    val httpsUrl: String get() = "https://github.com/$owner/$id"
    val gitUrl: String get() = "https://github.com/$owner/$id.git"
    /** raw.githubusercontent.com URL for a given branch/file */
    fun rawUrl(branch: String = "main", path: String = ""): String =
        "https://raw.githubusercontent.com/$owner/$id/$branch/$path"
    /** GitHub API repo URL */
    fun apiUrl(): String = "https://api.github.com/repos/$owner/$id"
}

/**
 * Validate GitHub URL — accepts https://github.com/<owner>/<repo> [+ .git] [+ trailing /].
 * Returns GithubRepo on success, null on failure.
 * Used by AddBunkerDialog to gate clone.
 */
fun parseGithubUrl(url: String): GithubRepo? {
    val trimmed = url.trim()
    if (trimmed.isEmpty()) return null
    // Normalize ssh-style: git@github.com:owner/repo.git -> https://github.com/owner/repo
    val normalized = when {
        trimmed.startsWith("git@github.com:") -> {
            val rest = trimmed.removePrefix("git@github.com:")
            "https://github.com/${rest.removeSuffix(".git")}"
        }
        trimmed.startsWith("github.com/") -> "https://$trimmed"
        else -> trimmed
    }
    val match = GITHUB_URL_RE.matchEntire(normalized.trimEnd('/')) ?: return null
    val owner = match.groupValues[1].trim()
    val repo = match.groupValues[2].trim().removeSuffix(".git")
    if (owner.isEmpty() || repo.isEmpty()) return null
    // owner/repo are loosely validated: no spaces, no empty segments
    if (owner.contains(" ") || repo.contains(" ")) return null
    if (owner.contains("/") || repo.contains("/")) return null
    return GithubRepo(owner = owner, name = repo)
}

/** Convenience: true if URL is a plausible GitHub bunker URL. */
fun isValidGithubBunkerUrl(url: String): Boolean = parseGithubUrl(url) != null

// --- bunker structure validation ---

/**
 * Validates that a set of repo file paths (relative to repo root) satisfies KnowledgeBunker bunker spec:
 * - At least one kebab-case *.md at root (e.g. music.md) OR in allowed subdirs, but spec says root *.md
 * - Must contain archive-box/inbox.md (the inbox) — quarantine.md is optional but should exist for valid curated bunkers
 *
 * [paths] are normalized with '/' separators, e.g. ["music.md", "games.md", "archive-box/inbox.md", "quarantine.md"]
 * Returns ValidationResult with isValid + message.
 *
 * Task spec: "kebab-case *.md + archive-box/inbox.md exists"
 */
data class BunkerValidationResult(val isValid: Boolean, val reason: String = "")

fun validateBunkerStructure(paths: List<String>): BunkerValidationResult {
    val normalized = paths.map { it.trim().trimStart('/', '\\').replace('\\', '/') }.filter { it.isNotEmpty() }
    val hasInbox = normalized.any { it == "archive-box/inbox.md" }
    if (!hasInbox) {
        return BunkerValidationResult(false, "Missing required archive-box/inbox.md")
    }
    // At least one kebab-case md at root or direct file (not counting archive-box/inbox.md/quarantine.md/bookkeeping)
    // Per spec: "*.md must be kebab-case" — so any non-kebab .md besides inbox/quarantine fails strictly?
    // We enforce: at least one kebab-case root md; any non-kebab md at root is an error hint but not blocking if at least one valid exists?
    // For strict validation we report invalid if any root *.md is not kebab-case (to keep curation clean).
    val rootMdFiles = normalized.filter { !it.contains('/') && it.endsWith(".md") }
    // Exclude README.md which is allowed non-kebab (project docs)
    val curatedRootMd = rootMdFiles.filter { it != "README.md" && it != "CONTRIBUTING.md" && it != "LICENSE.md" }
    if (curatedRootMd.isEmpty()) {
        return BunkerValidationResult(false, "No kebab-case *.md at repo root (e.g. music.md)")
    }
    val invalidKebab = curatedRootMd.filter { !isKebabCaseMd(it) }
    if (invalidKebab.isNotEmpty()) {
        return BunkerValidationResult(false, "Non kebab-case markdown at root: ${invalidKebab.joinToString(", ")} (must be kebab-case)")
    }
    // Also check that any .md under repo (excluding archive-box/quarantine/README) that is at root depth is kebab
    // nested .md under archive-box is allowed with different naming; only root enforced strictly.
    return BunkerValidationResult(true, "Valid bunker (${curatedRootMd.size} domains, inbox present)")
}

/**
 * Shorthand for validating a single file list string from git tree / GitHub API.
 */
fun validateBunkerFileListFromApi(fileNames: List<String>): BunkerValidationResult =
    validateBunkerStructure(fileNames)

// --- marketplace JSON fetch ---

/**
 * Decode marketplace json string (curated index) to List<BunkerMeta>.
 * Ignores unknown keys, lenient.
 *
 * Example marketplace.json:
 * [
 *   {"id":"music-bunker","name":"Music Bunker","url":"https://github.com/KnowledgeBunker/music-bunker","description":"..."},
 *   {"id":"games-bunker","name":"Games Bunker","url":"https://github.com/KnowledgeBunker/games-bunker"}
 * ]
 *
 * Test spec:
 * @Test fun parsesMarketplace() { val list = Json.decodeFromString<Marketplace>(json); assertEquals(1, list.size) }
 * where Marketplace == List<BunkerMeta> (typealias in model/Bunker.kt)
 */
fun decodeMarketplace(json: String): List<BunkerMeta> {
    if (json.isBlank()) return emptyList()
    return try {
        marketplaceJson.decodeFromString<Marketplace>(json)
    } catch (_: Exception) {
        // Also try wrapper object { "bunkers": [...] } for forward compat
        try {
            val wrapper = marketplaceJson.decodeFromString<MarketplaceWrapper>(json)
            wrapper.bunkers
        } catch (_: Exception) {
            emptyList()
        }
    }
}

@kotlinx.serialization.Serializable
private data class MarketplaceWrapper(val bunkers: List<BunkerMeta> = emptyList())

/**
 * Fetch curated marketplace index via Ktor.
 * Tries [MARKETPLACE_URL] then [MARKETPLACE_FALLBACK_URL]; returns empty list on failure (never throws).
 *
 * This is the primary "curated, not open store" index. Direct add-by-URL is alternative for private bunkers.
 */
suspend fun fetchMarketplace(client: HttpClient): List<BunkerMeta> {
    // Try primary curated index
    val tried = listOf(MARKETPLACE_URL, MARKETPLACE_FALLBACK_URL)
    for (url in tried) {
        try {
            val text: String = client.get(url).body()
            val decoded = decodeMarketplace(text)
            if (decoded.isNotEmpty()) return decoded
            // If decoded empty but fetch succeeded, still try next url? but respect empty curated?
            // If curated is intentionally empty, return empty. But we try fallback only if primary failed/empty
            if (text.isNotBlank()) return decoded
        } catch (_: Exception) {
            continue
        }
    }
    return emptyList()
}

/**
 * Overload that creates a default client — for callers that don't have DI.
 * Desktop uses CIO, web uses JS engine via actual HttpClient config.
 * Prefer injecting a configured client with json content negotiation.
 */
suspend fun fetchMarketplace(): List<BunkerMeta> {
    val client = createMarketplaceHttpClient()
    try {
        return fetchMarketplace(client)
    } finally {
        try { client.close() } catch (_: Exception) {}
    }
}

// Expect/actual client creation — commonMain delegates to platform-specific engine.
// We define the expect here and actuals in desktopMain / wasmJsMain.
internal expect fun createMarketplaceHttpClient(): HttpClient

// --- Add-by-URL helpers ---

/**
 * Build raw.githubusercontent.com URL for a bunker file.
 * Useful for web readonly reads (no FS, fetch raw).
 */
fun rawGithubContentUrl(repo: GithubRepo, branch: String = "main", path: String): String =
    repo.rawUrl(branch, path)

/**
 * Build GitHub API contents URL for directory listing (web readonly fallback).
 */
fun githubApiContentsUrl(repo: GithubRepo, branch: String = "main", path: String = ""): String {
    val base = repo.apiUrl()
    return if (path.isBlank()) "$base/contents?ref=$branch" else "$base/contents/$path?ref=$branch"
}
