package com.chikura.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.datlag.kcef.KCEF
import java.io.File
import java.time.LocalDateTime

// Inbuilt player engine: real Chromium (KCEF) for desktop.
// Flight recorder writes every stage to kcef-init.log so init failures
// can be diagnosed from disk instead of guessed at.

object PlayerEngine {
    var ready by mutableStateOf(false)
    var progress by mutableStateOf(0f)
    var stage by mutableStateOf("Starting…")
    var failed by mutableStateOf<String?>(null)
    var bundleInfo by mutableStateOf("bundle: ?")
    private var started = false

    fun log(msg: String) {
        try {
            val line = "${LocalDateTime.now()} $msg (bundle=${bundleSizeMb()}MB)\n"
            File("kcef-init.log").appendText(line)
        } catch (_: Exception) {
        }
        refreshBundleInfo()
    }

    fun bundleSizeMb(): Long {
        return try {
            val root = File("kcef-bundle")
            if (!root.exists()) return -1L
            var total = 0L
            var count = 0
            root.walkTopDown().forEach {
                if (it.isFile) {
                    total += it.length()
                    count++
                }
            }
            logFileSafeTotal(total, count)
            total / (1024L * 1024L)
        } catch (_: Exception) {
            -2L
        }
    }

    private var lastCount = 0
    private fun logFileSafeTotal(total: Long, count: Int) {
        lastCount = count
    }

    fun refreshBundleInfo() {
        bundleInfo = try {
            val root = File("kcef-bundle")
            if (!root.exists()) {
                "bundle: missing"
            } else {
                var total = 0L
                var count = 0
                root.walkTopDown().forEach {
                    if (it.isFile) {
                        total += it.length()
                        count++
                    }
                }
                "bundle: ${total / (1024L * 1024L)} MB · $count files"
            }
        } catch (_: Exception) {
            "bundle: unreadable"
        }
    }

    suspend fun ensureStarted() {
        if (started) return
        started = true
        failed = null
        log("ensureStarted: begin")
        refreshBundleInfo()
        try {
            KCEF.init(
                builder = {
                    installDir(File("kcef-bundle"))
                    progress {
                        onLocating { stage = "Locating player engine…"; log("stage=locating") }
                        onDownloading { pct ->
                            stage = "Downloading player engine…"
                            progress = (pct / 100f).coerceIn(0f, 1f)
                            log("stage=downloading pct=$pct")
                        }
                        onExtracting { stage = "Extracting player engine…"; log("stage=extracting") }
                        onInstall { stage = "Installing player engine…"; log("stage=install") }
                        onInitializing { stage = "Initializing player…"; log("stage=initializing") }
                        onInitialized { stage = "Ready"; ready = true; log("stage=initialized READY") }
                    }
                    settings {
                        cachePath = File("kcef-cache").absolutePath
                    }
                },
                onError = {
                    val msg = (it?.message ?: it.toString()).take(300)
                    log("onError: $msg :: ${it?.stackTrace?.take(5)?.joinToString(" <- ")}")
                    failed = msg.take(160)
                },
                onRestartRequired = {
                    log("onRestartRequired FIRED")
                    failed = "First download done — close and run once more, then it plays inline."
                }
            )
            log("init returned normally (ready=$ready)")
        } catch (t: Throwable) {
            log("ensureStarted THREW: ${t.message} :: ${t.stackTrace.take(5).joinToString(" <- ")}")
            failed = (t.message ?: t.toString()).take(160)
        }
    }

    fun retry() {
        started = false
        failed = null
        progress = 0f
        stage = "Starting…"
    }

    fun dispose() {
        try {
            KCEF.disposeBlocking()
        } catch (_: Exception) {
        }
    }
}
