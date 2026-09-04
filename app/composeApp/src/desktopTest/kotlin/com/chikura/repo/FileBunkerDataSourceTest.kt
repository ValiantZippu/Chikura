package com.chikura.repo

import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileBunkerDataSourceTest {

    private fun sampleRoot(): File {
        // Resolve sample-bunker test resources via multiple candidate paths
        val candidates = listOf(
            File("src/commonTest/resources/sample-bunker"),
            File("composeApp/src/commonTest/resources/sample-bunker"),
            File("app/composeApp/src/commonTest/resources/sample-bunker"),
            File(System.getProperty("user.dir"), "src/commonTest/resources/sample-bunker"),
            File(System.getProperty("user.dir"), "composeApp/src/commonTest/resources/sample-bunker"),
            File(System.getProperty("user.dir"), "app/composeApp/src/commonTest/resources/sample-bunker")
        )
        // Also try relative to app root via FileSystem probe
        for (c in candidates) {
            if (c.exists() && c.isDirectory) {
                // Return parent (bunkers root parent), so loadBunker("sample-bunker") works via root
                return c.parentFile
            }
        }
        // Fallback: search upwards
        var cur: File? = File(System.getProperty("user.dir"))
        repeat(5) {
            val p1 = File(cur, "composeApp/src/commonTest/resources")
            if (p1.exists()) return p1
            val p2 = File(cur, "app/composeApp/src/commonTest/resources")
            if (p2.exists()) return p2
            cur = cur?.parentFile
        }
        // Default to app/composeApp/src/commonTest/resources/sample-bunker's parent
        return File("src/commonTest/resources")
    }

    @Test
    fun loadBunkerFromTestResources() = runBlocking {
        val root = sampleRoot()
        // The FileBunkerDataSource expects root containing bunkers; our sample-bunker is directly under root
        val ds = FileBunkerDataSource(root)
        val bunker = ds.loadBunker("sample-bunker")
        assertEquals(2, bunker.domains.size, "sample-bunker must have 2 domains (music, tech) — root=${root.absolutePath}, bunker=$bunker")
        // Verify domains are music and tech sorted
        val ids = bunker.domains.map { it.id }.sorted()
        assertTrue(ids.contains("music"), "domains=$ids")
        assertTrue(ids.contains("tech"), "domains=$ids")
    }

    @Test
    fun loadBunkerDirectFilePath() = runBlocking {
        val direct = File("app/composeApp/src/commonTest/resources/sample-bunker").let { f ->
            if (f.exists()) f else File("composeApp/src/commonTest/resources/sample-bunker")
        }.let { f ->
            if (f.exists()) f else File("src/commonTest/resources/sample-bunker")
        }
        // If direct file path exists, test FileBunkerDataSource with root = direct.parent
        if (!direct.exists()) return@runBlocking // skip if not found in CI
        val ds = FileBunkerDataSource(direct.parentFile)
        val bunker = ds.loadBunker("sample-bunker")
        assertEquals(2, bunker.domains.size)
    }

    @Test
    fun listBunkersReturnsSampleBunker() = runBlocking {
        val root = sampleRoot()
        val ds = FileBunkerDataSource(root)
        val list = ds.listBunkers()
        assertTrue(list.any { it.id == "sample-bunker" }, "list=${list.map { it.id }} root=${root.absolutePath}")
    }
}
