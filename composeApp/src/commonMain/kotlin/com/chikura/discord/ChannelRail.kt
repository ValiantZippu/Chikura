package com.chikura.discord

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chikura.model.Resource

@Composable
fun DiscordServerIcon(name: String, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    val src = remember { MutableInteractionSource() }
    val hov by src.collectIsHoveredAsState()
    val s by animateFloatAsState(if (hov && !selected) 1.12f else 1f, tween(150), label = "railHover")
    val bg = if (selected || hov) Color.White else Color(0xFF111111)
    val fg = if (selected || hov) Color.Black else Color.White
    Box(
        modifier = Modifier.scale(s).clip(RoundedCornerShape(12.dp)).background(bg).border(1.dp, if (selected || hov) Color.White else Color(0xFF222222), RoundedCornerShape(12.dp)).let { if (onClick != null) it.clickable(interactionSource = src, indication = null, onClick = onClick) else it }.padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(name.take(2).uppercase(), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = fg, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DiscordChannelRow(name: String, unread: Boolean = false, active: Boolean = false, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (active) Color(0xFF1A1A1A) else if (unread) Color(0xFF111111) else Color.Transparent).let { if (onClick != null) it.clickable(onClick = onClick) else it }.padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {                Text("#", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = if (active) Color.White else Color(0xFF6B6B6B))
        val displayName = name.replace("-", " ").replace("_", " ").lowercase().split(" ").joinToString(" ") { w -> w.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
        Text(displayName, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = if (active || unread) Color.White else Color(0xFF9A9A9A), fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (unread) Box(Modifier.size(6.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFFFF3B30)))
    }
}

@Composable
fun DiscordCategoryHeader(title: String, expanded: Boolean, onToggle: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(if (expanded) "▾" else "▸", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color(0xFF6B6B6B))
        Text(title.uppercase(), fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color(0xFF9A9A9A), fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, modifier = Modifier.weight(1f))
        Text("+", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFF3A3A3A))
    }
}

data class ChannelItem(val id: String, val label: String)

@Composable
fun DiscordChannelSidebar(
    domains: List<String>,
    channels: Map<String, List<ChannelItem>>,
    selectedChannel: String?,
    onSelectChannel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Plain Column: this sidebar nests inside the app sidebar LazyColumn item,
    // and a nested vertical LazyColumn gets infinity height constraints (crash).
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF0A0A0A)).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 6.dp)) {
                Text("Search or create a post...", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color(0xFF3A3A3A))
            }
            Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF111111)).border(1.dp, Color(0xFF222222), RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text("Sort & View", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color(0xFF9A9A9A))
            }
        }
        domains.forEach { domain ->
            val chans = channels[domain] ?: emptyList()
            var expanded by remember(domain) { mutableStateOf(true) }
            Column {
                DiscordCategoryHeader(title = domain, expanded = expanded) { expanded = !expanded }
                if (expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(start = 4.dp)) {
                        chans.forEach { ch ->
                            DiscordChannelRow(name = ch.label, active = ch.id == selectedChannel, unread = ch.id == selectedChannel, onClick = { onSelectChannel(ch.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiscordPostCard(resource: Resource, onClick: (() -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF0A0A0A)).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(12.dp)).let { if (onClick != null) it.clickable(onClick = onClick) else it }.padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFF1A1A1A)), contentAlignment = Alignment.Center) { Text(resource.typeHint.take(1).uppercase(), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold) }
            Column(modifier = Modifier.weight(1f)) {
                Text(resource.raw.take(48), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(resource.url.take(48), fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color(0xFF6B6B6B), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF111111)).border(1.dp, Color(0xFF222222), RoundedCornerShape(8.dp)))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${resource.typeHint}", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color(0xFF6B6B6B))
            Spacer(Modifier.weight(1f))
            Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF111111)).padding(horizontal = 6.dp, vertical = 3.dp)) { Text("OPEN ↗", fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = Color.White) }
        }
    }
}
