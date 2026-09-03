package com.knowledgebunker.repo

import com.knowledgebunker.model.Bunker
import com.knowledgebunker.model.BunkerMeta
import com.knowledgebunker.parser.buildBunker
import java.io.File

/**
 * Task 3: File-system loader (Desktop/Android).
 * Reads bunkers from ~/KnowledgeBunker/bunkers/ or from a supplied root (for tests).
 * Integrates BunkerParser (Task 2) to preserve indent and typeHint.
 *
 * Bunker layout: ~/KnowledgeBunker/bunkers/<bunkerId>/[kebab-case].md
 * Supports kebab-case markdown plus archive-box inbox and quarantine per spec.
 */
class FileBunkerDataSource(
    private val root: File = File(System.getProperty("user.home"), "KnowledgeBunker/bunkers")
) : BunkerDataSource {

    override suspend fun listBunkers(): List<BunkerMeta> {
        if (!root.exists() || !root.isDirectory) return emptyList()
        return root.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.sortedBy { it.name }
            ?.map { BunkerMeta(id = it.name, name = it.name, path = it.absolutePath) }
            ?: emptyList()
    }

    override suspend fun loadBunker(id: String): Bunker {
        val bunkerDir = resolveBunkerDir(id)
        if (bunkerDir == null || !bunkerDir.exists() || !bunkerDir.isDirectory) {
            return Bunker(id = id, name = id, domains = emptyList())
        }

        val mdFiles = bunkerDir.listFiles { f ->
            f.isFile && f.name.endsWith(".md") && f.name != "README.md"
        }?.sortedBy { it.name } ?: emptyList()

        val extraFiles = listOf(
            File(bunkerDir, "archive-box/inbox.md"),
            File(bunkerDir, "quarantine.md")
        ).filter { it.exists() && it.isFile }

        // Deduplicate by absolute path (extraFiles may already be in mdFiles if flat)
        val allFiles = (mdFiles + extraFiles).distinctBy { it.absolutePath }

        // If directory contains no markdown, return empty bunker (blank-by-default)
        if (allFiles.isEmpty()) {
            return Bunker(id = id, name = id, domains = emptyList())
        }

        val markdownByDomain = linkedMapOf<String, String>()
        for (file in allFiles) {
            val effectiveDomainId = when {
                file.absolutePath.contains("archive-box") -> "archive-box-inbox"
                file.name == "quarantine.md" -> "quarantine"
                else -> file.nameWithoutExtension
            }
            val text = try {
                file.readText(Charsets.UTF_8)
            } catch (_: Exception) {
                ""
            }
            // If duplicate domainId (should not happen), merge with newline
            markdownByDomain[effectiveDomainId] = if (markdownByDomain.containsKey(effectiveDomainId)) {
                markdownByDomain[effectiveDomainId] + "\n" + text
            } else {
                text
            }
        }

        return buildBunker(id = id, name = id, markdownByDomain = markdownByDomain)
    }

    private fun resolveBunkerDir(id: String): File? {
        // 1. root/id
        val direct = File(root, id)
        if (direct.exists() && direct.isDirectory) return direct

        // 2. id as absolute or relative path
        val asPath = File(id)
        if (asPath.exists() && asPath.isDirectory) return asPath

        // 3. For tests: try common test-resource locations relative to app/ and repo root
        // Gradle desktop test working dir is app/ or app/composeApp/
        val candidates = listOf(
            File("src/commonTest/resources/$id"),
            File("composeApp/src/commonTest/resources/$id"),
            File("app/composeApp/src/commonTest/resources/$id"),
            File(System.getProperty("user.dir"), "src/commonTest/resources/$id"),
            File(System.getProperty("user.dir"), "composeApp/src/commonTest/resources/$id"),
            File(System.getProperty("user.dir"), "app/composeApp/src/commonTest/resources/$id")
        )
        candidates.forEach { c ->
            if (c.exists() && c.isDirectory) return c
            // Also try resolving against root's parent path for sample-bunker
            val alt = File(root, "../${c.path}").canonicalFile
            if (alt.exists() && alt.isDirectory) return alt
        }

        // 4. Fallback: search upwards from user.dir for composeApp/src/commonTest/resources
        var cur: File? = File(System.getProperty("user.dir"))
        repeat(4) {
            val probe = File(cur, "composeApp/src/commonTest/resources/$id")
            if (probe.exists() && probe.isDirectory) return probe
            val probe2 = File(cur, "app/composeApp/src/commonTest/resources/$id")
            if (probe2.exists() && probe2.isDirectory) return probe2
            cur = cur?.parentFile
        }

        return direct
    }
}
