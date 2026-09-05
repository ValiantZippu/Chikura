package com.chikura.obsidian

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class GraphNode(val id: String, val label: String, val x: Float, val y: Float)
data class GraphEdge(val from: String, val to: String)

@Composable
fun GraphNodeCard(node: GraphNode, selected: Boolean = false): Unit {
    Box(
        modifier = Modifier.size(80.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color.White else Color(0xFF111111))
            .border(1.dp, if (selected) Color.White else Color(0xFF1A1A1A), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(node.label.take(12), fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = if (selected) Color.Black else Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ObsidianGraph(nodes: List<GraphNode>, edges: List<GraphEdge>, modifier: Modifier = Modifier): Unit {
    Box(modifier = modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF0A0A0A)).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(16.dp)).padding(12.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            edges.forEach { edge ->
                val from = nodes.find { it.id == edge.from } ?: return@forEach
                val to = nodes.find { it.id == edge.to } ?: return@forEach
                drawLine(
                    color = Color(0xFF333333),
                    start = Offset(from.x * size.width, from.y * size.height),
                    end = Offset(to.x * size.width, to.y * size.height),
                    strokeWidth = 1.5f
                )
            }
        }
        nodes.forEach { node ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
                // positioned via offset - simplified; real impl uses layout
                GraphNodeCard(node)
            }
        }
    }
}
