package com.knowledgebunker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knowledgebunker.model.Resource
import com.knowledgebunker.ui.board.BoardViewModel
import com.knowledgebunker.ui.board.WhiteboardEdge
import com.knowledgebunker.ui.components.ResourceCard
import com.knowledgebunker.ui.theme.BunkerBlack
import com.knowledgebunker.ui.theme.BunkerTheme
import com.knowledgebunker.ui.theme.BunkerWhite
import kotlin.math.roundToInt

/**
 * Task 6: Whiteboard via Compose Canvas — nodes as boxes (ResourceCard), edges as lines, can connect nodes.
 * Uses [BoardViewModel.edges] for edge persistence; node positions are local UI state
 * but preserved via remember. Connect by clicking two nodes sequentially.
 */
@Composable
fun WhiteboardScreen(
    viewModel: BoardViewModel,
    modifier: Modifier = Modifier
) {
    val bunker by viewModel.bunker.collectAsState()
    val edges by viewModel.edges.collectAsState()
    WhiteboardScreenContent(
        resources = bunker.domains.flatMap { it.resources },
        edges = edges,
        onConnect = { a, b -> viewModel.connect(a, b) },
        modifier = modifier
    )
}

@Composable
fun WhiteboardScreenContent(
    resources: List<Resource>,
    edges: List<WhiteboardEdge>,
    onConnect: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    BunkerTheme {
        // Node positions — start in grid layout
        val positions = remember(resources) {
            mutableStateMapOf<String, Offset>().apply {
                resources.forEachIndexed { idx, res ->
                    val col = idx % 3
                    val row = idx / 3
                    put(res.id, Offset(x = 20f + col * 280f, y = 20f + row * 180f))
                }
            }
        }
        // Selection for connecting
        var selectedId by remember { mutableStateOf<String?>(null) }
        var connectMode by remember { mutableStateOf(false) }

        Column(modifier = modifier.background(BunkerWhite).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Toolbar
            Row(
                modifier = Modifier
                    .border(1.dp, BunkerBlack)
                    .background(BunkerWhite)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WHITEBOARD  •  ${resources.size} nodes  •  ${edges.size} edges",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = BunkerBlack
                )
                Box(
                    modifier = Modifier
                        .border(1.dp, BunkerBlack)
                        .background(if (connectMode) BunkerBlack else BunkerWhite)
                        .clickable { connectMode = !connectMode; selectedId = null }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (connectMode) "CONNECT: ON" else "CONNECT: OFF",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = if (connectMode) BunkerWhite else BunkerBlack
                    )
                }
                if (selectedId != null) {
                    Text(
                        text = "selected: $selectedId → click another node",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        color = Color(0xFF555555)
                    )
                }
            }

            // Edge legend
            if (edges.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    edges.forEach { e ->
                        Box(
                            modifier = Modifier
                                .border(1.dp, BunkerBlack)
                                .background(BunkerWhite)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${e.fromId} — ${e.toId}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 7.sp,
                                color = BunkerBlack
                            )
                        }
                    }
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, BunkerBlack)
                    .background(BunkerWhite)
            ) {
                // Canvas for edges — drawn behind nodes
                Canvas(modifier = Modifier.fillMaxSize()) {
                    edges.forEach { edge ->
                        val from = positions[edge.fromId]
                        val to = positions[edge.toId]
                        if (from != null && to != null) {
                            // Node box size approx 240x120, center offset
                            val fromCenter = Offset(from.x + 120f, from.y + 60f)
                            val toCenter = Offset(to.x + 120f, to.y + 60f)
                            drawLine(
                                color = Color.Black,
                                start = fromCenter,
                                end = toCenter,
                                strokeWidth = 1.5f
                            )
                            // Draw small endpoint circles
                            drawCircle(color = Color.Black, radius = 4f, center = fromCenter)
                            drawCircle(color = Color.Black, radius = 4f, center = toCenter)
                        }
                    }
                    // If connectMode and one node selected, draw preview line to center? Skipped (needs pointer pos)
                }

                // Nodes as boxes (ResourceCard) — draggable via Compose foundation
                resources.forEach { res ->
                    val pos = positions[res.id] ?: Offset(0f, 0f)
                    val isSelected = selectedId == res.id
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                            .size(width = 240.dp, height = 140.dp)
                            .border(1.dp, if (isSelected) Color(0xFF000000) else BunkerBlack)
                            .background(if (isSelected) Color(0xFFF0F0F0) else BunkerWhite)
                            .pointerInput(res.id) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val cur = positions[res.id] ?: Offset(0f, 0f)
                                    positions[res.id] = Offset(
                                        x = (cur.x + dragAmount.x).coerceIn(0f, 1200f),
                                        y = (cur.y + dragAmount.y).coerceIn(0f, 800f)
                                    )
                                }
                            }
                            .clickable {
                                if (connectMode) {
                                    if (selectedId == null) {
                                        selectedId = res.id
                                    } else {
                                        val first = selectedId!!
                                        if (first != res.id) {
                                            onConnect(first, res.id)
                                        }
                                        selectedId = null
                                    }
                                }
                            }
                            .padding(6.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ResourceCard(resource = res, modifier = Modifier.width(228.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, BunkerBlack)
                                        .background(BunkerWhite)
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = res.id,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 6.sp,
                                        color = BunkerBlack
                                    )
                                }
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .border(1.dp, BunkerBlack)
                                            .background(BunkerBlack)
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "SELECTED",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 6.sp,
                                            color = BunkerWhite
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (resources.isEmpty()) {
                    Box(
                        modifier = Modifier.align(Alignment.Center)
                            .border(1.dp, BunkerBlack)
                            .background(BunkerWhite)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "(whiteboard empty — add resources to bunker)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF888888)
                        )
                    }
                }
            }
        }
    }
}
