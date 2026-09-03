package com.chikura.repo

import com.chikura.model.ChikuThread
import com.chikura.model.ChikuThreadMeta
import com.chikura.parser.buildChikuThread
import java.io.File

/**
 * Task 3: File-system loader (Desktop/Android).
 * Reads chikuthreads from ~/Chikura/chikuthreads/ or from a supplied root (for tests).
 * Integrates ChikuThreadParser (Task 2) to preserve indent and typeHint.
 *
 * ChikuThread layout: ~/Chikura/chikuthreads/<chikuthreadId>/[kebab-case].md
 * Supports kebab-case markdown plus archive-box inbox and quarantine per spec.
 */
class FileChikuThreadDataSource(
    private val root: File = File(System.getProperty("user.home"), "Chikura/chikuthreads")
) : ChikuThreadDataSource {

    override suspend fun listChikuThreads(): List<ChikuThreadMeta> {
        if (!root.exists() || !root.isDirectory) return emptyList()
        return root.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.sortedBy { it.name }
            ?.map { ChikuThreadMeta(id = it.name, name = it.name, path = it.absolutePath) }
            ?: emptyList()
    }

    override suspend fun loadChikuThread(id: String): ChikuThread {
        val chikuthreadDir = resolveChikuThreadDir(id)
        if (chikuthreadDir == null || !chikuthreadDir.exists() || !chikuthreadDir.isDirectory) {
            return ChikuThread(id = id, name = id, domains = emptyList())
        }

        val mdFiles = chikuthreadDir.listFiles { f ->
            f.isFile && f.name.endsWith(".md") && f.name != "README.md"
        }?.sortedBy { it.name } ?: emptyList()

        val extraFiles = listOf(
            File(chikuthreadDir, "archive-box/inbox.md"),
            File(chikuthreadDir, "quarantine.md")
        ).filter { it.exists() && it.isFile }

        // Deduplicate by absolute path (extraFiles may already be in mdFiles if flat)
        val allFiles = (mdFiles + extraFiles).distinctBy { it.absolutePath }

        // If directory contains no markdown, return empty chikuthread (blank-by-default)
        if (allFiles.isEmpty()) {
            return ChikuThread(id = id, name = id, domains = emptyList())
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

        return buildChikuThread(id = id, name = id, markdownByDomain = markdownByDomain)
    }

    private fun resolveChikuThreadDir(id: String): File? {
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
            // Also try resolving against root's parent path for sample-chikuthread
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
