package com.chikura.app

import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileSystemView

/**
 * Desktop entry point — JVM "desktop" target.
 * Chikura 知蔵: knowledge vault, black & white terminal look.
 *
 * On launch: auto-detect ChikuThread in cwd or ~/KnowledgeBunker/bunkers/.
 * User can click "Open Folder..." to pick any directory with *.md files.
 */
fun main() = application {
    val viewModel = remember { ChikuraViewModel() }

    // Wire desktop file system operations
    viewModel.listFiles = { path ->
        val dir = File(path)
        if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.map { it.absolutePath }?.sorted() ?: emptyList()
        } else emptyList()
    }

    // Auto-detect: try cwd first, then home bunker dir
    val cwd = System.getProperty("user.dir")
    val hasThreadInCwd = File(cwd).listFiles()?.any {
        it.isFile && it.name.endsWith(".md") && it.name != "README.md"
    } == true

    val homeThread = File(System.getProperty("user.home"), "KnowledgeBunker/bunkers")

    if (hasThreadInCwd) {
        viewModel.loadThread(cwd) { File(it).readText(Charsets.UTF_8) }
    } else if (homeThread.exists() && homeThread.isDirectory) {
        val firstBunker = homeThread.listFiles()?.firstOrNull { it.isDirectory }
        if (firstBunker != null) {
            viewModel.loadThread(firstBunker.absolutePath) { File(it).readText(Charsets.UTF_8) }
        }
    }

    // Wire folder picker via JFileChooser (directories only)
    viewModel.requestOpenFolder = {
        val fc = JFileChooser(FileSystemView.getFileSystemView().defaultDirectory)
        fc.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        fc.dialogTitle = "Open ChikuThread Folder"
        val result = fc.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            val selected = fc.selectedFile?.absolutePath
            if (selected != null) {
                viewModel.loadThread(selected) { File(it).readText(Charsets.UTF_8) }
            }
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Chikura 知蔵"
    ) {
        App(viewModel = viewModel)
    }
}
