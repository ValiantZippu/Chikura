package com.chikura.marketplace

import com.chikura.platform.FileSystem

actual fun tryCloneViaGitPlatform(gitUrl: String, destPath: String): Boolean {
    return try {
        val pb = ProcessBuilder("git", "clone", "--depth", "1", gitUrl, destPath)
        pb.redirectErrorStream(true)
        val proc = pb.start()
        val exit = proc.waitFor()
        exit == 0 && FileSystem.exists(destPath)
    } catch (_: Exception) {
        false
    } catch (_: Throwable) {
        false
    }
}
