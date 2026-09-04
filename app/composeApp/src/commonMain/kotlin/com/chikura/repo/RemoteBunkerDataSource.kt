package com.chikura.repo

import com.chikura.marketplace.GithubRepo
import com.chikura.marketplace.parseGithubUrl
import com.chikura.model.Bunker
import com.chikura.model.BunkerMeta
import com.chikura.parser.buildBunker
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Web (READ_ONLY) data source — reads via raw.githubusercontent.com / GitHub API, not FS.
 * Used when READ_ONLY == true (wasmJs/web). No FileSystem, no Monaco, no drag writes.
 *
 * Each bunker is a GitHub repo (e.g. https://github.com/owner/music-bunker).
 * We list files via GitHub API, then fetch each markdown via raw.githubusercontent.com.
 *
 * On desktop, FileBunkerDataSource is used instead (file-system + KGit clone).
 */
class RemoteBunkerDataSource(
    private val client: HttpClient,
    private val bunkerGithubUrls: Map<String, String> = emptyMap(), // bunkerId -> githubUrl
    private val defaultBranch: String = "main"
) : BunkerDataSource {

    private fun repoFor(id: String): GithubRepo? {
        val url = bunkerGithubUrls[id] ?: return null
        return parseGithubUrl(url)
    }

    override suspend fun listBunkers(): List<BunkerMeta> {
        // Marketplace-provided list is the source of truth for remote; if we have no mapping, return empty
        // Callers (e.g. App) should populate bunkerGithubUrls from marketplace.json or add-by-url
        return bunkerGithubUrls.map { (id, url) ->
            val repo = parseGithubUrl(url)
            BunkerMeta(id = id, name = id, path = url, url = url, githubUrl = repo?.httpsUrl)
        }
    }

    override suspend fun loadBunker(id: String): Bunker {
        val repo = repoFor(id) ?: return Bunker(id = id, name = id, domains = emptyList())

        // 1. List root contents via GitHub API
        val fileNames = try {
            fetchRootFileNames(repo)
        } catch (_: Exception) {
            emptyList()
        }

        if (fileNames.isEmpty()) return Bunker(id = id, name = id, domains = emptyList())

        // 2. Fetch each kebab-case md + archive-box/inbox.md via raw
        val markdownByDomain = linkedMapOf<String, String>()
        // Root kebab-case mds
        val rootMds = fileNames.filter { !it.contains('/') && it.endsWith(".md") && it != "README.md" && it != "CONTRIBUTING.md" }
            .filter { Regex("^[a-z0-9]+(-[a-z0-9]+)*\\.md$").matches(it) }

        for (name in rootMds) {
            val text = tryFetchRaw(repo, name) ?: continue
            val domainId = name.removeSuffix(".md")
            markdownByDomain[domainId] = text
        }

        // archive-box/inbox.md
        if (fileNames.contains("archive-box") || fileNames.contains("archive-box/inbox.md")) {
            val inboxText = tryFetchRaw(repo, "archive-box/inbox.md")
            if (inboxText != null) markdownByDomain["archive-box-inbox"] = inboxText
        } else {
            // Try directly — GitHub API root listing doesn't always include subdir files
            val inboxText = tryFetchRaw(repo, "archive-box/inbox.md")
            if (inboxText != null) markdownByDomain["archive-box-inbox"] = inboxText
        }

        // quarantine.md (optional)
        val quarantineText = tryFetchRaw(repo, "quarantine.md")
        if (quarantineText != null) markdownByDomain["quarantine"] = quarantineText

        if (markdownByDomain.isEmpty()) return Bunker(id = id, name = id, domains = emptyList())
        return buildBunker(id = id, name = id, markdownByDomain = markdownByDomain)
    }

    private suspend fun fetchRootFileNames(repo: GithubRepo): List<String> {
        val json: String = client.get("${repo.apiUrl()}/contents?ref=$defaultBranch").body()
        return parseNames(json)
    }

    private suspend fun tryFetchRaw(repo: GithubRepo, path: String): String? {
        return try {
            val text: String = client.get(repo.rawUrl(defaultBranch, path)).body()
            if (text.isBlank()) null else text
        } catch (_: Exception) { null }
    }

    private fun parseNames(json: String): List<String> {
        return try {
            val element = kotlinx.serialization.json.Json.parseToJsonElement(json)
            val arr = element as? JsonArray ?: return emptyList()
            arr.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content }
        } catch (_: Exception) {
            Regex("\"name\"\\s*:\\s*\"([^\"]+)\"").findAll(json).map { it.groupValues[1] }.toList()
        }
    }
}
