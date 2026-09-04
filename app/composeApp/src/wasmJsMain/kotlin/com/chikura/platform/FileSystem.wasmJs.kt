package com.chikura.platform

actual object FileSystem {
    actual fun exists(path: String): Boolean = false
    actual fun isDirectory(path: String): Boolean = false
    actual fun isFile(path: String): Boolean = false
    actual fun listFiles(path: String): List<String> = emptyList()
    actual fun readText(path: String): String = ""
    actual fun writeText(path: String, content: String) { /* no-op on web — read-only */ }
    actual fun mkdirs(path: String) { /* no-op */ }
    actual fun homeBunkersPath(): String = ""
}
