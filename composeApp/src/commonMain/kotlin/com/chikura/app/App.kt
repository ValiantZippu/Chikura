package com.chikura.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chikura.model.ChikuThread
import com.chikura.model.Resource
import com.chikura.ui.components.ResourceCard
import com.chikura.ui.theme.ChikuraTheme

private val ChikuraBlack = Color(0xFF000000)
private val ChikuraWhite = Color(0xFFFFFFFF)
private val ChikuraGray = Color(0xFF888888)
private val ChikuraLightGray = Color(0xFFF5F5F5)
private val ChikuraBorder = Color(0xFFE0E0E0)

enum class ViewMode { LIST, KANBAN, WHITEBOARD }

@Composable
fun App() {
    var selectedThread by remember { mutableStateOf<ChikuThread?>(null) }
    var selectedDomain by remember { mutableStateOf<String?>(null) }
    var viewMode by remember { mutableStateOf(ViewMode.LIST) }
    var repoInput by remember { mutableStateOf("ValiantZippu/Chikura") }

    ChikuraTheme {
        Column(modifier = Modifier.fillMaxSize().background(ChikuraWhite)) {
            // Top bar — CHIKURA 知蔵 + repo input + stats
            Row(
                modifier = Modifier.fillMaxWidth().background(ChikuraWhite).border(1.dp, ChikuraBlack).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("CHIKURA", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ChikuraBlack)
                    Text("知蔵", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = ChikuraGray)
                    Box(modifier = Modifier.border(1.dp, ChikuraBlack).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(repoInput, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = ChikuraBlack)
                    }
                    Box(modifier = Modifier.border(1.dp, ChikuraBlack).background(ChikuraBlack).clickable { /* load */ }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("LOAD", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = ChikuraWhite, fontWeight = FontWeight.Bold)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("VIEW ON GITHUB ↗", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ChikuraGray, modifier = Modifier.clickable {})
                    if (READ_ONLY) Text("READ_ONLY", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color(0xFFFF4444), modifier = Modifier.border(1.dp, Color(0xFFFF4444)).padding(4.dp))
                }
            }

            // View mode toggle
            Row(modifier = Modifier.fillMaxWidth().background(ChikuraLightGray).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ViewMode.values().forEach { mode ->
                    val active = mode == viewMode
                    Box(
                        modifier = Modifier.border(1.dp, if (active) ChikuraBlack else ChikuraBorder).background(if (active) ChikuraBlack else ChikuraWhite).clickable { viewMode = mode }.padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(mode.name, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = if (active) ChikuraWhite else ChikuraBlack)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("${getPlatformName()}${if (READ_ONLY) " • Web Wiki" else " • Desktop"}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ChikuraGray)
            }

            Row(modifier = Modifier.weight(1f)) {
                // Sidebar — CHIKUTHREADS
                Column(
                    modifier = Modifier.width(240.dp).fillMaxHeight().background(ChikuraWhite).border(1.dp, ChikuraBorder).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("CHIKUTHREADS", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ChikuraBlack)
                    Text("${selectedThread?.domains?.size ?: 0} files · ${selectedThread?.domains?.sumOf { it.resources.size } ?: 0} links", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ChikuraGray)
                    HorizontalDivider(color = ChikuraBorder, thickness = 1.dp)
                    // All domains
                    SidebarItem("ALL DOMAINS", "${selectedThread?.domains?.sumOf { it.resources.size } ?: 0}", selectedDomain == null) { selectedDomain = null }
                    selectedThread?.domains?.forEach { domain ->
                        SidebarItem(domain.id, "${domain.resources.size}", selectedDomain == domain.id) { selectedDomain = domain.id }
                    }
                    if (selectedThread == null) {
                        Box(modifier = Modifier.fillMaxWidth().border(1.dp, ChikuraBorder).background(ChikuraLightGray).padding(12.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Chikura — 知蔵", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ChikuraBlack)
                                Text("knowledge vault", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ChikuraGray)
                                Spacer(Modifier.height(8.dp))
                                Text("Load a ChikuThread", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ChikuraGray)
                            }
                        }
                    }
                }

                // Content area
                Column(modifier = Modifier.weight(1f).fillMaxHeight().background(ChikuraWhite).padding(12.dp)) {
                    if (selectedThread == null) {
                        // Empty professional state with icons placeholder
                        Box(modifier = Modifier.fillMaxSize().border(1.dp, ChikuraBlack).background(ChikuraWhite), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(48.dp).background(ChikuraBlack), contentAlignment = Alignment.Center) {
                                    Text("蔵", fontFamily = FontFamily.Monospace, fontSize = 24.sp, color = ChikuraWhite)
                                }
                                Text("BUNKER: ${selectedThread?.name ?: "No thread loaded"}", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = ChikuraBlack)
                                Text("Enter owner/repo above and LOAD — reads via GitHub raw", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ChikuraGray)
                                if (READ_ONLY) {
                                    Box(modifier = Modifier.border(1.dp, ChikuraBlack).clickable {}.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                        Text("Edit in App", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = ChikuraBlack)
                                    }
                                }
                            }
                        }
                    } else {
                        val thread = selectedThread!!
                        val visibleDomains = if (selectedDomain == null) thread.domains else thread.domains.filter { it.id == selectedDomain }
                        Row(modifier = Modifier.fillMaxWidth().background(ChikuraLightGray).border(1.dp, ChikuraBorder).padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("BUNKER: ${thread.name}", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ChikuraBlack)
                            Text("${visibleDomains.size} domains · ${visibleDomains.sumOf { it.resources.size }} resources", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ChikuraGray)
                        }
                        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().border(1.dp, ChikuraBorder), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            items(visibleDomains) { domain ->
                                DomainBlock(domain, viewMode)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarItem(label: String, count: String, active: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().border(1.dp, if (active) ChikuraBlack else ChikuraBorder).background(if (active) ChikuraBlack else ChikuraWhite).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = if (active) ChikuraWhite else ChikuraBlack, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
        Text(count, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = if (active) ChikuraWhite else ChikuraGray)
    }
}

@Composable
private fun DomainBlock(domain: com.chikura.model.Domain, viewMode: ViewMode) {
    var expanded by remember { mutableStateOf(true) }
    Column(modifier = Modifier.fillMaxWidth().background(ChikuraWhite).border(1.dp, ChikuraBorder)) {
        Row(modifier = Modifier.fillMaxWidth().background(ChikuraLightGray).clickable { expanded = !expanded }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (expanded) "▼" else "▶", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ChikuraBlack)
            Spacer(Modifier.width(8.dp))
            Text(domain.id, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ChikuraBlack)
            Spacer(Modifier.width(8.dp))
            Text("${domain.resources.size}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ChikuraGray, modifier = Modifier.background(ChikuraWhite).border(1.dp, ChikuraBorder).padding(horizontal = 6.dp, vertical = 2.dp))
        }
        if (expanded) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                when (viewMode) {
                    ViewMode.LIST -> domain.resources.forEach { res -> ResourceCard(resource = res, onExpand = {}) }
                    ViewMode.KANBAN -> {
                        // Kanban columns = sections
                        domain.sections.forEach { section ->
                            Text("## ${section.name}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ChikuraBlack, modifier = Modifier.background(ChikuraLightGray).fillMaxWidth().padding(6.dp))
                            section.categories.forEach { cat ->
                                Text("### ${cat.name}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ChikuraGray)
                                cat.resources.forEach { res -> ResourceCard(resource = res, onExpand = {}) }
                            }
                        }
                    }
                    ViewMode.WHITEBOARD -> {
                        // Whiteboard nodes as boxes
                        domain.resources.forEach { res ->
                            Box(modifier = Modifier.fillMaxWidth().border(1.dp, ChikuraBlack).padding(8.dp)) {
                                ResourceCard(resource = res, onExpand = {})
                            }
                        }
                    }
                }
            }
        }
    }
}
