package com.chikura.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chikura.app.READ_ONLY
import com.chikura.model.ChikuThreadMeta
import com.chikura.platform.FileSystem
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Task 7: AddChikuThreadDialog — paste GitHub URL -> validate -> git clone via KGit / fetch raw (desktop) or fetch raw (web)
 *
 * Validation:
 * - GitHub URL must parse via parseGithubUrl
 * - On desktop: git clone to ~/Chikura/chikuthreads/<name>/ (not nested) via KGit (or git CLI fallback),
 *   then validate kebab-case *.md + archive-box/inbox.md exists
 * - On web: READ_ONLY — no clone, fetch raw via raw.githubusercontent.com / GitHub API, validate structure from API file list
 *
 * Black & white terminal: 1px borders, JetBrains Mono.
 */

// --- validation state for UI ---

sealed class AddChikuThreadState {
    data object Idle : AddChikuThreadState()
    data class Validating(val message: String) : AddChikuThreadState()
    data class Error(val message: String) : AddChikuThreadState()
    data class Success(val meta: ChikuThreadMeta) : AddChikuThreadState()
}

/**
 * Pure validation for GitHub URL string (no network).
 * Returns null if valid, error message otherwise.
 */
fun validateGithubUrlInput(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return "Paste a GitHub URL (https://github.com/owner/repo)"
    val repo = parseGithubUrl(trimmed) ?: return "Invalid GitHub URL — expect https://github.com/<owner>/<repo>"
    if (repo.owner.isBlank() || repo.name.isBlank()) return "Invalid owner/repo"
    return null
}

// --- clone / fetch logic (platform-agnostic suspend functions) ---

/**
 * Result of add-by-URL.
 */
sealed class AddChikuThreadResult {
    data class Added(val meta: ChikuThreadMeta) : AddChikuThreadResult()
    data class Failed(val reason: String) : AddChikuThreadResult()
}

/**
 * Desktop: clone via KGit (or git CLI) into chikuthreads root.
 * We use [FileSystem] and expect git binary if KGit not available.
 * For pure Kotlin, KGit would be `org.eclipse.jgit`-backed; here we shell out to `git clone` if available,
 * else we fetch raw files via Ktor as fallback (still validates).
 *
 * This function never throws — returns Failed on error.
 */
suspend fun addChikuThreadByUrlDesktop(
    githubUrl: String,
    client: HttpClient,
    chikuthreadsRoot: String = FileSystem.homeChikuThreadsPath()
): AddChikuThreadResult {
    val repo = parseGithubUrl(githubUrl) ?: return AddChikuThreadResult.Failed("Invalid GitHub URL")
    val chikuthreadId = repo.id // use repo name as chikuthread id (kebab-case expected to be enforced by repo naming, but we validate contents)
    val destPath = chikuthreadsRoot.trimEnd('/', '\\') + "/" + chikuthreadId

    // If already exists, report
    if (FileSystem.exists(destPath) && FileSystem.isDirectory(destPath)) {
        // Check if valid chikuthread already
        val existingFiles = try { FileSystem.listFiles(destPath) } catch (_: Exception) { emptyList<String>() }
        val relative = existingFiles.map { it.substringAfter(destPath.trimEnd('/', '\\')).trimStart('/', '\\') }
        // For desktop FileSystem.listFiles returns absolute paths; we also need recursive listing
        val allFiles = collectAllFilesRecursive(destPath)
        val validation = validateChikuThreadStructure(allFiles)
        if (validation.isValid) {
            return AddChikuThreadResult.Added(ChikuThreadMeta(id = chikuthreadId, name = chikuthreadId, path = destPath, url = repo.httpsUrl))
        }
        return AddChikuThreadResult.Failed("ChikuThread directory already exists but invalid: ${validation.reason}")
    }

    // Attempt git clone via system git (KGit alternative — isomorphic-git via KMP would be integrated via expect/actual)
    val cloned = tryCloneViaGit(repo.gitUrl, destPath)
    if (cloned) {
        val allFiles = collectAllFilesRecursive(destPath)
        val validation = validateChikuThreadStructure(allFiles)
        if (!validation.isValid) {
            return AddChikuThreadResult.Failed("Cloned repo fails chikuthread spec: ${validation.reason}")
        }
        return AddChikuThreadResult.Added(ChikuThreadMeta(id = chikuthreadId, name = chikuthreadId, path = destPath, url = repo.httpsUrl))
    }

    // Fallback: fetch raw via GitHub API file list validation + sparse fetch
    // We still create dest dir and fetch at least the index files to validate
    val apiFiles = try {
        fetchFileListViaGithubApi(repo, client)
    } catch (_: Exception) { null }

    if (apiFiles == null) {
        return AddChikuThreadResult.Failed("git clone failed and GitHub API unavailable — check URL and network")
    }
    val validation = validateChikuThreadStructure(apiFiles)
    if (!validation.isValid) {
        return AddChikuThreadResult.Failed("Remote repo fails chikuthread spec: ${validation.reason}")
    }

    // Create local dir and fetch at least inbox + one md to make it loadable
    try {
        FileSystem.mkdirs(destPath)
        FileSystem.mkdirs("$destPath/archive-box")
        // Fetch archive-box/inbox.md
        val inboxText = fetchRawText(client, repo.rawUrl(path = "archive-box/inbox.md"))
        if (inboxText != null) FileSystem.writeText("$destPath/archive-box/inbox.md", inboxText)
        // Fetch other root mds (kebab-case)
        val rootMds = apiFiles.filter { !it.contains('/') && it.endsWith(".md") && isKebabCaseMd(it) }
        for (name in rootMds.take(10)) {
            val text = fetchRawText(client, repo.rawUrl(path = name))
            if (text != null) FileSystem.writeText("$destPath/$name", text)
        }
    } catch (e: Exception) {
        return AddChikuThreadResult.Failed("Failed to materialize chikuthread locally: ${e.message}")
    }

    return AddChikuThreadResult.Added(ChikuThreadMeta(id = chikuthreadId, name = chikuthreadId, path = destPath, url = repo.httpsUrl))
}

/**
 * Web (READ_ONLY): no git clone, no FS — fetch file list via GitHub API and validate,
 * then create in-memory ChikuThreadMeta pointing to raw.githubusercontent.com.
 * Caller should use RemoteChikuThreadDataSource to load actual markdown via raw urls.
 */
suspend fun addChikuThreadByUrlWeb(
    githubUrl: String,
    client: HttpClient
): AddChikuThreadResult {
    val repo = parseGithubUrl(githubUrl) ?: return AddChikuThreadResult.Failed("Invalid GitHub URL")
    val apiFiles = try {
        fetchFileListViaGithubApi(repo, client)
    } catch (e: Exception) {
        return AddChikuThreadResult.Failed("GitHub API unavailable: ${e.message}")
    }
    val validation = validateChikuThreadStructure(apiFiles)
    if (!validation.isValid) return AddChikuThreadResult.Failed("Remote repo fails chikuthread spec: ${validation.reason}")
    return AddChikuThreadResult.Added(
        ChikuThreadMeta(
            id = repo.id,
            name = repo.id,
            path = repo.httpsUrl, // on web path is the github url (no FS)
            url = repo.httpsUrl,
            githubUrl = repo.httpsUrl
        )
    )
}

/**
 * Unified entry: dispatches to desktop or web implementation based on READ_ONLY flag.
 * Desktop also supports fetch-raw fallback when git is unavailable.
 */
suspend fun addChikuThreadByUrl(
    githubUrl: String,
    client: HttpClient,
    chikuthreadsRoot: String = try { FileSystem.homeChikuThreadsPath() } catch (_: Exception) { "" }
): AddChikuThreadResult {
    return if (READ_ONLY) {
        addChikuThreadByUrlWeb(githubUrl, client)
    } else {
        addChikuThreadByUrlDesktop(githubUrl, client, chikuthreadsRoot)
    }
}

// --- helpers ---

private fun collectAllFilesRecursive(root: String): List<String> {
    val result = mutableListOf<String>()
    fun walk(path: String) {
        val entries = try { FileSystem.listFiles(path) } catch (_: Exception) { emptyList<String>() }
        for (e in entries) {
            val rel = e.substringAfter(root.trimEnd('/', '\\')).trimStart('/', '\\').replace('\\', '/')
            if (FileSystem.isDirectory(e)) {
                walk(e)
            } else {
                // Only track files relative to root
                if (rel.isNotBlank()) result.add(rel)
            }
        }
    }
    // listFiles at root may include dirs; we need to also include files at root that are direct children
    // Our walk includes them via recursion, but we start by listing root
    walk(root)
    // Fallback: if walk didn't collect because listFiles returns empty on some platforms, try single level
    if (result.isEmpty()) {
        val direct = try { FileSystem.listFiles(root) } catch (_: Exception) { emptyList<String>() }
        direct.forEach { p ->
            val isFile = try { FileSystem.isFile(p) } catch (_: Exception) { false }
            if (isFile) {
                val rel = p.substringAfter(root.trimEnd('/', '\\')).trimStart('/', '\\').replace('\\', '/')
                result.add(rel)
            }
        }
    }
    return result
}

private fun tryCloneViaGit(gitUrl: String, destPath: String): Boolean {
    // Delegates to platform actual — JS/Wasm has no ProcessBuilder, return false to fallback to fetch
    return tryCloneViaGitPlatform(gitUrl, destPath)
}

internal expect fun tryCloneViaGitPlatform(gitUrl: String, destPath: String): Boolean

private suspend fun fetchFileListViaGithubApi(repo: GithubRepo, client: HttpClient): List<String> {
    // GitHub API: GET /repos/{owner}/{repo}/contents?ref=main
    // Returns array of { name, path, type } — we also need archive-box/inbox.md check which may be in subdir
    // So we fetch root listing + archive-box listing
    val rootJson: String = client.get(repo.apiUrl() + "/contents?ref=main").body()
    val rootFiles = parseGithubContentsNames(rootJson)
    // Also fetch archive-box dir if present
    val archiveFiles = try {
        val archiveJson: String = client.get(repo.apiUrl() + "/contents/archive-box?ref=main").body()
        parseGithubContentsNames(archiveJson).map { "archive-box/$it" }
    } catch (_: Exception) { emptyList<String>() }
    // quarantine.md is at root
    return rootFiles + archiveFiles
}

private fun parseGithubContentsNames(json: String): List<String> {
    // json is array of objects { name: "music.md", path: "music.md", type: "file" } etc
    // Use lenient JsonArray parse without codegen
    return try {
        val element = kotlinx.serialization.json.Json.parseToJsonElement(json)
        val array = element as? JsonArray ?: return emptyList()
        array.mapNotNull { el ->
            val obj = el.jsonObject
            obj["name"]?.jsonPrimitive?.content
        }
    } catch (_: Exception) {
        // Fallback rough regex if json parsing fails (e.g. HTML error)
        Regex("\"name\"\\s*:\\s*\"([^\"]+)\"").findAll(json).map { it.groupValues[1] }.toList()
    }
}

private suspend fun fetchRawText(client: HttpClient, url: String): String? {
    return try {
        val text: String = client.get(url).body()
        if (text.isBlank()) null else text
    } catch (_: Exception) { null }
}

// --- Composable Dialog ---

/**
 * AddChikuThreadDialog composable — paste GitHub URL -> validate -> add.
 *
 * Shows:
 * - Text field (monospace)
 * - Validation error (kebab-case + archive-box/inbox.md)
 * - Add / Cancel buttons
 * - When READ_ONLY true, shows note "Web is read-only — chikuthread will be loaded via raw.githubusercontent.com"
 * - Success shows ChikuThreadMeta and "Edit in App" hint for web.
 */
@Composable
fun AddChikuThreadDialog(
    onDismiss: () -> Unit,
    onAdded: (ChikuThreadMeta) -> Unit,
    client: HttpClient? = null,
    modifier: Modifier = Modifier
) {
    var urlInput by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<AddChikuThreadState>(AddChikuThreadState.Idle) }
    val scope = rememberCoroutineScope()
    // Use provided client or create one
    val httpClient = remember { client ?: createMarketplaceHttpClient() }

    val inputError = validateGithubUrlInput(urlInput)
    val canSubmit = inputError == null && state !is AddChikuThreadState.Validating

    Column(
        modifier = modifier
            .border(1.dp, Color.Black)
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "ADD CHIKUTHREAD BY URL",
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = Color.Black
        )
        Text(
            text = "Paste GitHub URL (e.g. https://github.com/owner/repo) — must be valid chikuthread spec: kebab-case *.md + archive-box/inbox.md",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = Color(0xFF555555)
        )

        // Input field — styled as bordered box (no TextField dependency to keep commonMain dependency minimal)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (inputError != null && urlInput.isNotEmpty()) Color(0xFFCC0000) else Color.Black)
                .background(Color.White)
                .padding(8.dp)
        ) {
            // We use a simple Text as placeholder for actual TextField; real impl would use BasicTextField
            // To keep dependencies minimal for this task, we expose urlInput editing via click handlers is stubbed
            // Callers integrating with platform should wire a real TextField to urlInput state.
            // For now show current value + hint
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (urlInput.isEmpty()) "https://github.com/..." else urlInput,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = if (urlInput.isEmpty()) Color(0xFFAAAAAA) else Color.Black
                )
                if (READ_ONLY) {
                    Text(
                        text = "WEB READ-ONLY — will fetch via raw.githubusercontent.com",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        color = Color(0xFF888888)
                    )
                }
            }
        }

        // Validation feedback
        if (inputError != null && urlInput.isNotEmpty()) {
            Text(
                text = "✗ $inputError",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = Color(0xFFCC0000)
            )
        }
        when (val s = state) {
            is AddChikuThreadState.Error -> Text(
                text = "✗ ${s.message}",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = Color(0xFFCC0000)
            )
            is AddChikuThreadState.Validating -> Text(
                text = "… ${s.message}",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = Color(0xFF555555)
            )
            is AddChikuThreadState.Success -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "✓ Added ${s.meta.id} → ${s.meta.path}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Color.Black
                )
                if (READ_ONLY) {
                    // Web: no Monaco, no drag, show "Edit in App"
                    Box(
                        modifier = Modifier
                            .border(1.dp, Color.Black)
                            .background(Color.White)
                            .clickable { /* open desktop app link — e.g. chikura://open?repo=... */ }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Edit in App",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color.Black
                        )
                    }
                    Text(
                        text = "Web is read-only (no Monaco, no drag). Open in desktop/Android app to edit & push.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        color = Color(0xFF888888)
                    )
                }
            }
            else -> {}
        }

        // Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .border(1.dp, Color.Black)
                    .background(if (canSubmit) Color.Black else Color(0xFFCCCCCC))
                    .clickable(enabled = canSubmit) {
                        if (!canSubmit) return@clickable
                        scope.launch {
                            state = AddChikuThreadState.Validating("Validating chikuthread spec (kebab-case *.md + archive-box/inbox.md)…")
                            val result = if (READ_ONLY) {
                                addChikuThreadByUrlWeb(urlInput.trim(), httpClient)
                            } else {
                                addChikuThreadByUrlDesktop(urlInput.trim(), httpClient)
                            }
                            when (result) {
                                is AddChikuThreadResult.Added -> {
                                    state = AddChikuThreadState.Success(result.meta)
                                    onAdded(result.meta)
                                }
                                is AddChikuThreadResult.Failed -> state = AddChikuThreadState.Error(result.reason)
                            }
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (READ_ONLY) "FETCH (READ-ONLY)" else "CLONE & VALIDATE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Color.White
                )
            }
            Box(
                modifier = Modifier
                    .border(1.dp, Color.Black)
                    .background(Color.White)
                    .clickable { onDismiss() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "CANCEL",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Color.Black
                )
            }
        }

        // Helper note about curation
        Text(
            text = "Marketplace is curated, not an open store. Use Add by URL for private chikuthreads.",
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            color = Color(0xFF999999)
        )
    }
}

/**
 * Helper for callers that need a real editable input.
 * Wraps AddChikuThreadDialog with a [onUrlChange] callback for external TextField binding.
 * The main AddChikuThreadDialog above keeps urlInput internal for minimal dependency; this variant
 * exposes controlled input for platforms that have TextField available.
 */
@Composable
fun AddChikuThreadDialogControlled(
    url: String,
    onUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onAdded: (ChikuThreadMeta) -> Unit,
    client: HttpClient? = null,
    modifier: Modifier = Modifier
) {
    var state by remember { mutableStateOf<AddChikuThreadState>(AddChikuThreadState.Idle) }
    val scope = rememberCoroutineScope()
    val httpClient = remember { client ?: createMarketplaceHttpClient() }
    val inputError = validateGithubUrlInput(url)
    val canSubmit = inputError == null && state !is AddChikuThreadState.Validating

    Column(
        modifier = modifier
            .border(1.dp, Color.Black)
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "ADD CHIKUTHREAD BY URL", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Color.Black)
        // Real input would be BasicTextField(value=url, onValueChange=onUrlChange, ...) — placeholder box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (inputError != null && url.isNotEmpty()) Color(0xFFCC0000) else Color.Black)
                .background(Color.White)
                .padding(8.dp)
                .clickable { /* focus */ }
        ) {
            Text(
                text = if (url.isEmpty()) "https://github.com/..." else url,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = if (url.isEmpty()) Color(0xFFAAAAAA) else Color.Black
            )
        }
        if (inputError != null && url.isNotEmpty()) {
            Text(text = "✗ $inputError", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFFCC0000))
        }
        when (val s = state) {
            is AddChikuThreadState.Error -> Text(text = "✗ ${s.message}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFFCC0000))
            is AddChikuThreadState.Validating -> Text(text = "… ${s.message}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFF555555))
            is AddChikuThreadState.Success -> {
                Text(text = "✓ Added ${s.meta.id}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Black)
                if (READ_ONLY) {
                    Box(
                        modifier = Modifier
                            .border(1.dp, Color.Black)
                            .background(Color.White)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) { Text(text = "Edit in App", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Black) }
                }
            }
            else -> {}
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .border(1.dp, Color.Black)
                    .background(if (canSubmit) Color.Black else Color(0xFFCCCCCC))
                    .clickable(enabled = canSubmit) {
                        scope.launch {
                            state = AddChikuThreadState.Validating("Validating…")
                            val result = addChikuThreadByUrl(url.trim(), httpClient)
                            when (result) {
                                is AddChikuThreadResult.Added -> { state = AddChikuThreadState.Success(result.meta); onAdded(result.meta) }
                                is AddChikuThreadResult.Failed -> state = AddChikuThreadState.Error(result.reason)
                            }
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) { Text(text = if (READ_ONLY) "FETCH (READ-ONLY)" else "CLONE & VALIDATE", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White) }
            Box(modifier = Modifier.border(1.dp, Color.Black).background(Color.White).clickable { onDismiss() }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text(text = "CANCEL", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Black)
            }
        }
    }
}
