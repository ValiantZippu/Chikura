package com.chikura.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chikura.ui.board.BoardViewModel
import com.chikura.ui.screens.KanbanScreen
import com.chikura.ui.screens.ListScreen
import com.chikura.ui.screens.WhiteboardScreen
import com.chikura.ui.theme.BunkerBlack
import com.chikura.ui.theme.BunkerTheme
import com.chikura.ui.theme.BunkerWhite

/**
 * Chikura (知蔵) — knowledge vault.
 * Root composable: sidebar + view switcher + content area.
 * Desktop: loads ChikuThread from a folder path.
 * Web (READ_ONLY): fetches via raw.githubusercontent.com, no drag, no edit.
 */
@Composable
fun App(viewModel: ChikuraViewModel) {
    val bunker by viewModel.bunker.collectAsState()
    val selectedDomain by viewModel.selectedDomain.collectAsState()
    val currentView by viewModel.currentView.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val threadPath by viewModel.threadPath.collectAsState()

    BunkerTheme {
        Row(modifier = Modifier.fillMaxSize().background(BunkerWhite)) {
            // ─── Sidebar ───
            Sidebar(
                viewModel = viewModel,
                modifier = Modifier
                    .width(260.dp)
                    .fillMaxHeight()
                    .background(BunkerWhite)
            )

            // ─── Main Content ───
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(BunkerWhite)
            ) {
                // Top bar: title + view switcher + thread path
                TopBar(
                    currentView = currentView,
                    onViewChange = { viewModel.setView(it) },
                    threadPath = threadPath,
                    domainCount = bunker.domains.size,
                    resourceCount = bunker.domains.sumOf { it.resources.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BunkerWhite)
                )

                // Content area
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BunkerWhite)
                ) {
                    when {
                        isLoading -> CenteredMessage("Loading thread...")
                        error != null -> CenteredMessage("⚠ $error")
                        bunker.domains.isEmpty() -> EmptyState(onOpen = { viewModel.requestOpenFolder?.let { it() } })
                        else -> {
                            // If no domain selected, show all
                            val activeBunker = if (selectedDomain != null) {
                                bunker.copy(domains = listOf(selectedDomain!!))
                            } else bunker

                            when (currentView) {
                                ViewMode.LIST -> ListScreen(bunker = activeBunker)
                                ViewMode.KANBAN -> {
                                    val boardVm = viewModel.getBoardViewModel()
                                    KanbanScreen(viewModel = boardVm)
                                }
                                ViewMode.WHITEBOARD -> {
                                    val boardVm = viewModel.getBoardViewModel()
                                    WhiteboardScreen(viewModel = boardVm)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top bar — CHIKURA 知蔵 title, view mode tabs, thread info.
 */
@Composable
private fun TopBar(
    currentView: ViewMode,
    onViewChange: (ViewMode) -> Unit,
    threadPath: String?,
    domainCount: Int,
    resourceCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier                    .border(1.dp, BunkerBlack)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: title
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "CHIKURA",
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                color = BunkerBlack
            )
            Text(
                text = "知蔵",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Color(0xFF888888)
            )
            if (threadPath != null) {
                Text(
                    text = "• ${threadPath.substringAfterLast("/").substringAfterLast("\\")}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Color(0xFF555555)
                )
            }
        }

        // Center: view tabs
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            ViewMode.entries.forEach { mode ->
                val label = when (mode) {
                    ViewMode.LIST -> "LIST"
                    ViewMode.KANBAN -> "KANBAN"
                    ViewMode.WHITEBOARD -> "BOARD"
                }
                val isActive = currentView == mode
                Box(
                    modifier = Modifier
                        .border(1.dp, BunkerBlack)
                        .background(if (isActive) BunkerBlack else BunkerWhite)
                        .clickable { onViewChange(mode) }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = if (isActive) BunkerWhite else BunkerBlack
                    )
                }
            }
        }

        // Right: stats
        Text(
            text = "$domainCount domains  $resourceCount links",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = Color(0xFF888888)
        )
    }
}

/**
 * Sidebar — list of domains with resource counts, clickable to select.
 */
@Composable
private fun Sidebar(
    viewModel: ChikuraViewModel,
    modifier: Modifier = Modifier
) {
    val bunker by viewModel.bunker.collectAsState()
    val selectedDomain by viewModel.selectedDomain.collectAsState()

    Column(
        modifier = modifier                    .border(1.dp, BunkerBlack)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BunkerBlack)
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "CHIKUTHREADS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = BunkerBlack
                )
                Text(
                    text = "${bunker.domains.size} files",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    color = Color(0xFF888888)
                )
            }
        }

        // Domain list
        LazyColumn(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            // "All" item
            item {
                val isSelected = selectedDomain == null
                SidebarItem(
                    label = "ALL DOMAINS",
                    count = bunker.domains.sumOf { it.resources.size },
                    isSelected = isSelected,
                    onClick = { viewModel.clearSelection() }
                )
            }

            items(bunker.domains) { domain ->
                val isSelected = selectedDomain?.id == domain.id
                SidebarItem(
                    label = domain.id,
                    count = domain.resources.size,
                    isSelected = isSelected,
                    onClick = { viewModel.selectDomain(domain) }
                )
            }
        }

        // "Open folder" button at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()                    .border(1.dp, BunkerBlack)
                .clickable { viewModel.requestOpenFolder?.let { it() } }
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "[ OPEN FOLDER... ]",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = BunkerBlack
            )
        }
    }
}

@Composable
private fun SidebarItem(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isSelected) BunkerBlack else Color(0xFFCCCCCC))
            .background(if (isSelected) BunkerBlack else BunkerWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = if (isSelected) BunkerWhite else BunkerBlack,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .border(1.dp, if (isSelected) BunkerWhite else BunkerBlack)
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = "$count",
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                color = if (isSelected) BunkerWhite else BunkerBlack
            )
        }
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(BunkerWhite).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = BunkerBlack
        )
    }
}

@Composable
private fun EmptyState(onOpen: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(BunkerWhite).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.border(1.dp, BunkerBlack).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Chikura — 知蔵",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        color = BunkerBlack
                    )
                    Text(
                        text = "knowledge vault",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color(0xFF888888)
                    )
                Text(
                    text = getPlatformName(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        color = Color(0xFFAAAAAA)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .border(1.dp, BunkerBlack)
                    .clickable(onClick = onOpen)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "[ OPEN CHIKUTHREAD FOLDER ]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = BunkerBlack
                )
            }
            Text(
                text = "Open a folder containing *.md files + archive-box/",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = Color(0xFF999999)
            )
        }
    }
}
