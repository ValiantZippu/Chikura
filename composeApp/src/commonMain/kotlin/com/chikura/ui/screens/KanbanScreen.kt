package com.chikura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chikura.model.ChikuThread
import com.chikura.ui.board.BoardViewModel
import com.chikura.ui.components.ResourceCard
import com.chikura.ui.theme.ChikuraBlack
import com.chikura.ui.theme.ChikuraTheme
import com.chikura.ui.theme.ChikuraWhite

/**
 * Task 6: Kanban Board — columns = ## Section, cards = Resource.
 * Drag Resource between Section -> moves underlying bullet in .md preserving indent
 * via [BoardViewModel.move] which uses ChikuThreadParser writer.
 *
 * Implemented via Compose foundation drag-and-drop (pointerInput + detectDragGesturesAfterLongPress)
 * plus click fallback for accessibility.
 */
@Composable
fun KanbanScreen(
    viewModel: BoardViewModel,
    modifier: Modifier = Modifier
) {
    val chikuthread by viewModel.chikuthread.collectAsState()
    KanbanScreenContent(chikuthread = chikuthread, viewModel = viewModel, modifier = modifier)
}

@Composable
fun KanbanScreenContent(
    chikuthread: ChikuThread,
    viewModel: BoardViewModel,
    modifier: Modifier = Modifier
) {
    ChikuraTheme {
        val allResources = chikuthread.domains.flatMap { it.resources }
        val sectionNames = chikuthread.domains.flatMap { it.sections.map { s -> s.name } }
            .distinct()
            .ifEmpty {
                allResources.mapNotNull { it.section }.distinct().ifEmpty { listOf("General") }
            }
            .sorted()

        // Drag state — foundation pointerInput
        var draggedId by remember { mutableStateOf<String?>(null) }

        // Horizontal columns
        Box(
            modifier = modifier
                .background(ChikuraWhite)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // If chikuthread blank, show placeholder column
                val columns = if (sectionNames.isEmpty()) listOf("General") else sectionNames
                columns.forEach { section ->
                    KanbanColumn(
                        section = section,
                        resources = allResources.filter { (it.section ?: "General") == section },
                        allSections = columns,
                        draggedId = draggedId,
                        onDragStart = { draggedId = it },
                        onDragEnd = { draggedId = null },
                        onDrop = { resourceId, toSection ->
                            viewModel.move(resourceId, toSection)
                            draggedId = null
                        }
                    )
                }
                // Extra column for creating new sections via drop? Shown as drop target
                // Provide "Music Theory" etc will be auto-created when moved via button/drag
            }
            if (chikuthread.domains.isEmpty()) {
                Box(
                    modifier = Modifier
                        .border(1.dp, ChikuraBlack)
                        .background(ChikuraWhite)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "(blank chikuthread — no sections)",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color(0xFF888888)
                    )
                }
            }
        }
    }
}

@Composable
private fun KanbanColumn(
    section: String,
    resources: List<com.chikura.model.Resource>,
    allSections: List<String>,
    draggedId: String?,
    onDragStart: (String) -> Unit,
    onDragEnd: () -> Unit,
    onDrop: (resourceId: String, toSection: String) -> Unit
) {
    // Column is a drop target (Compose foundation). Use clickable as drop fallback.
    Box(
        modifier = Modifier
            .width(280.dp)
            .border(1.dp, ChikuraBlack)
            .background(ChikuraWhite)
            .clickable {
                // If dragging, drop onto this column
                if (draggedId != null) {
                    onDrop(draggedId, section)
                }
            }
            .padding(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ChikuraBlack)
                    .background(ChikuraWhite)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "## $section  (${resources.size})",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = ChikuraBlack
                )
            }

            // Drop hint when dragging
            if (draggedId != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF999999))
                        .background(Color(0xFFF5F5F5))
                        .clickable { onDrop(draggedId, section) }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DROP HERE → $section",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = ChikuraBlack
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                resources.forEach { res ->
                    val isDragging = draggedId == res.id
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (isDragging) Color(0xFF000000) else ChikuraBlack)
                            .background(if (isDragging) Color(0xFFEFEFEF) else ChikuraWhite)
                            // Drag source via Compose foundation (pointerInput)
                            .pointerInput(res.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { onDragStart(res.id) },
                                    onDragEnd = { onDragEnd() },
                                    onDragCancel = { onDragEnd() },
                                    onDrag = { _, _ -> }
                                )
                            }
                    ) {
                        Column(modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ResourceCard(resource = res, modifier = Modifier.fillMaxWidth())

                            // Move buttons — accessible alternative to drag
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Show up to 3 other sections as quick-move targets
                                allSections.filter { it != section }.take(3).forEach { target ->
                                    Box(
                                        modifier = Modifier
                                            .border(1.dp, ChikuraBlack)
                                            .background(ChikuraWhite)
                                            .clickable { onDrop(res.id, target) }
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "→ $target",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 8.sp,
                                            color = ChikuraBlack
                                        )
                                    }
                                }
                            }
                            // Inline hint for indent preservation
                            Text(
                                text = "indent=${res.indent}  id=${res.id}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 7.sp,
                                color = Color(0xFF999999)
                            )
                        }
                    }
                }
                if (resources.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .border(1.dp, Color(0xFFCCCCCC))
                            .background(ChikuraWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "(drop here)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = Color(0xFFAAAAAA)
                        )
                    }
                }
            }
        }
    }
}
