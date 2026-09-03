package com.chikura.repo

import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileChikuThreadDataSourceTest {

    private fun sampleRoot(): File {
        // Resolve sample-chikuthread test resources via multiple candidate paths
        val candidates = listOf(
            File("src/commonTest/resources/sample-chikuthread"),
            File("composeApp/src/commonTest/resources/sample-chikuthread"),
            File("app/composeApp/src/commonTest/resources/sample-chikuthread"),
            File(System.getProperty("user.dir"), "src/commonTest/resources/sample-chikuthread"),
            File(System.getProperty("user.dir"), "composeApp/src/commonTest/resources/sample-chikuthread"),
            File(System.getProperty("user.dir"), "app/composeApp/src/commonTest/resources/sample-chikuthread")
        )
        // Also try relative to app root via FileSystem probe
        for (c in candidates) {
            if (c.exists() && c.isDirectory) {
                // Return parent (chikuthreads root parent), so loadChikuThread("sample-chikuthread") works via root
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
        // Default to app/composeApp/src/commonTest/resources/sample-chikuthread's parent
        return File("src/commonTest/resources")
    }

    @Test
    fun loadChikuThreadFromTestResources() = runBlocking {
        val root = sampleRoot()
        // The FileChikuThreadDataSource expects root containing chikuthreads; our sample-chikuthread is directly under root
        val ds = FileChikuThreadDataSource(root)
        val chikuthread = ds.loadChikuThread("sample-chikuthread")
        assertEquals(2, chikuthread.domains.size, "sample-chikuthread must have 2 domains (music, tech) — root=${root.absolutePath}, chikuthread=$chikuthread")
        // Verify domains are music and tech sorted
        val ids = chikuthread.domains.map { it.id }.sorted()
        assertTrue(ids.contains("music"), "domains=$ids")
        assertTrue(ids.contains("tech"), "domains=$ids")
    }

    @Test
    fun loadChikuThreadDirectFilePath() = runBlocking {
        val direct = File("app/composeApp/src/commonTest/resources/sample-chikuthread").let { f ->
            if (f.exists()) f else File("composeApp/src/commonTest/resources/sample-chikuthread")
        }.let { f ->
            if (f.exists()) f else File("src/commonTest/resources/sample-chikuthread")
        }
        // If direct file path exists, test FileChikuThreadDataSource with root = direct.parent
        if (!direct.exists()) return@runBlocking // skip if not found in CI
        val ds = FileChikuThreadDataSource(direct.parentFile)
        val chikuthread = ds.loadChikuThread("sample-chikuthread")
        assertEquals(2, chikuthread.domains.size)
    }

    @Test
    fun listChikuThreadsReturnsSampleChikuThread() = runBlocking {
        val root = sampleRoot()
        val ds = FileChikuThreadDataSource(root)
        val list = ds.listChikuThreads()
        assertTrue(list.any { it.id == "sample-chikuthread" }, "list=${list.map { it.id }} root=${root.absolutePath}")
    }
}
