package com.knowledgebunker.platform

import java.io.File

actual object FileSystem {
    actual fun exists(path: String): Boolean = File(path).exists()
    actual fun isDirectory(path: String): Boolean = File(path).isDirectory
    actual fun isFile(path: String): Boolean = File(path).isFile
    actual fun listFiles(path: String): List<String> =
        File(path).listFiles()?.map { it.absolutePath } ?: emptyList()
    actual fun readText(path: String): String = File(path).readText(Charsets.UTF_8)
    actual fun homeBunkersPath(): String =
        File(System.getProperty("user.home"), "KnowledgeBunker/bunkers").absolutePath
}
