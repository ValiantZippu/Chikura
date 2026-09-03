package com.knowledgebunker.platform

actual object FileSystem {
    actual fun exists(path: String): Boolean = false
    actual fun isDirectory(path: String): Boolean = false
    actual fun isFile(path: String): Boolean = false
    actual fun listFiles(path: String): List<String> = emptyList()
    actual fun readText(path: String): String = ""
    actual fun homeBunkersPath(): String = ""
}
