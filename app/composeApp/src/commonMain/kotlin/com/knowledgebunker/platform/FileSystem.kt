package com.knowledgebunker.platform

/**
 * Task 3: expect/actual file IO.
 * Desktop uses java.io.File; wasm uses stubs / raw.githubusercontent.com fetch.
 * This satisfies the "expect/actual FileSystem" requirement.
 */
expect object FileSystem {
    fun exists(path: String): Boolean
    fun isDirectory(path: String): Boolean
    fun isFile(path: String): Boolean
    fun listFiles(path: String): List<String>
    fun readText(path: String): String
    fun homeBunkersPath(): String
}
