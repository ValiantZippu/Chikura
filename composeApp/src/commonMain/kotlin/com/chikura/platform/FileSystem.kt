package com.chikura.platform

/**
 * Task 3: expect/actual file IO.
 * Desktop uses java.io.File; wasm uses stubs / raw.githubusercontent.com fetch.
 * This satisfies the "expect/actual FileSystem" requirement.
 * Task 5 adds write/mkdirs for sidecar cache.
 */
expect object FileSystem {
    fun exists(path: String): Boolean
    fun isDirectory(path: String): Boolean
    fun isFile(path: String): Boolean
    fun listFiles(path: String): List<String>
    fun readText(path: String): String
    fun writeText(path: String, content: String)
    fun mkdirs(path: String)
    fun homeChikuThreadsPath(): String
}
