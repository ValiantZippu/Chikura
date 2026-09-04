package com.chikura.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chikura.model.Category
import com.chikura.model.ChikuThread
import com.chikura.model.Domain
import com.chikura.model.Resource
import com.chikura.model.Section
import com.chikura.ui.components.ResourceCard
import com.chikura.ui.theme.ChikuraTheme

private val ChikuraBlack = Color(0xFF000000)
private val ChikuraWhite = Color(0xFFFFFFFF)
private val ChikuraGray = Color(0xFF6B6B6B)
private val ChikuraDim = Color(0xFFD9D9D9)
private val ChikuraLight = Color(0xFFF7F7F7)

enum class ViewMode { LIST, KANBAN, WHITEBOARD }

private fun sampleThread(): ChikuThread {
    fun res(id: String, url: String, domain: String, section: String?, cat: String?): Resource =
        Resource(id = id, url = url, raw = url, domain = domain, section = section, category = cat, subcategory = null, typeHint = "video", indent = 0)
    val music = Domain(
        id = "music",
        name = "music",
        sections = listOf(
            Section("Sound & Theory", listOf(Category("EQ", listOf(res("music-0001","https://youtu.be/RIuqjFP2cHg","music","Sound & Theory","EQ"))))),
            Section("Production", listOf(Category("Mix", listOf(res("music-0002","https://youtu.be/dQw4w9WgXcQ","music","Production","Mix")))))
        ),
        resources = listOf(
            res("music-0001","https://youtu.be/RIuqjFP2cHg","music","Sound & Theory","EQ"),
            res("music-0002","https://youtu.be/dQw4w9WgXcQ","music","Production","Mix"),
            res("music-0003","https://soundcloud.com/artist/track","music","Production","Mix")
        )
    )
    val games = Domain(
        id = "games",
        name = "games",
        sections = listOf(Section("FPS", listOf(Category("Aim", listOf(res("games-0001","https://youtu.be/9bZkp7q19f0","games","FPS","Aim")))))),
        resources = listOf(res("games-0001","https://youtu.be/9bZkp7q19f0","games","FPS","Aim"), res("games-0002","https://store.steampowered.com/app/730","games","FPS","Aim"))
    )
    val tech = Domain(
        id = "technology",
        name = "technology",
        sections = listOf(Section("Tools", listOf(Category("Editors", listOf(res("tech-0001","https://github.com/JetBrains/compose-multiplatform","technology","Tools","Editors")))))),
        resources = listOf(res("tech-0001","https://github.com/JetBrains/compose-multiplatform","technology","Tools","Editors"))
    )
    val japan = Domain(
        id = "japan",
        name = "japan",
        sections = emptyList(),
        resources = listOf(res("japan-0001","https://youtu.be/jNQXAC9IVRw","japan",null,null))
    )
    return ChikuThread(id = "chikura", name = "Chikura — 知蔵", domains = listOf(music, games, tech, japan))
}

@Composable
fun App() {
    var selectedThread by remember { mutableStateOf(sampleThread()) }
    var selectedDomain by remember { mutableStateOf<String?>(null) }
    var viewMode by remember { mutableStateOf(ViewMode.LIST) }
    var repoInput by remember { mutableStateOf("ValiantZippu/Chikura") }
    var isLoading by remember { mutableStateOf(false) }

    ChikuraTheme {
        Column(modifier = Modifier.fillMaxSize().background(ChikuraWhite)) {
            // Header — compact, 1px, mono
            Row(
                modifier = Modifier.fillMaxWidth().background(ChikuraWhite).border(1.dp, ChikuraBlack).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(22.dp).background(ChikuraBlack), contentAlignment = Alignment.Center) { Text("蔵", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = ChikuraWhite) }
                        Text("CHIKURA", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ChikuraBlack)
                        Text("知蔵", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ChikuraGray)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.border(1.dp, ChikuraBlack).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("⬢", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ChikuraGray)
                        Text(repoInput, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = ChikuraBlack)
                    }
                    Box(modifier = Modifier.border(1.dp, ChikuraBlack).background(ChikuraBlack).clickable {
                        isLoading = true
                        selectedThread = sampleThread()
                        isLoading = false
                    }.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        Text(if (isLoading) "…" else "LOAD", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = ChikuraWhite, fontWeight = FontWeight.Bold)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${selectedThread.domains.size} files · ${selectedThread.domains.sumOf { it.resources.size }} links", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraGray, modifier = Modifier.border(1.dp, ChikuraDim).padding(horizontal = 6.dp, vertical = 3.dp))
                    Text("VIEW ON GITHUB ↗", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraGray, modifier = Modifier.border(1.dp, ChikuraDim).clickable {}.padding(horizontal = 8.dp, vertical = 4.dp))
                    if (READ_ONLY) Text("READ_ONLY", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = ChikuraWhite, modifier = Modifier.background(Color(0xFF111111)).padding(horizontal = 6.dp, vertical = 3.dp))
                }
            }

            // View tabs — tighter, professional
            Row(modifier = Modifier.fillMaxWidth().background(ChikuraLight).border(1.dp, ChikuraDim).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                ViewMode.values().forEach { mode ->
                    val active = mode == viewMode
                    val label = when(mode) { ViewMode.LIST -> "≡ LIST"; ViewMode.KANBAN -> "▦ KANBAN"; ViewMode.WHITEBOARD -> "⬢ WHITEBOARD" }
                    Box(modifier = Modifier.border(1.dp, if (active) ChikuraBlack else ChikuraDim).background(if (active) ChikuraBlack else ChikuraWhite).clickable { viewMode = mode }.padding(horizontal = 14.dp, vertical = 5.dp)) {
                        Text(label, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = if (active) ChikuraWhite else ChikuraBlack, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("${getPlatformName()} • ${viewMode.name}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraGray)
            }

            Row(modifier = Modifier.weight(1f)) {
                // Sidebar — denser, professional
                Column(modifier = Modifier.width(220.dp).fillMaxHeight().background(ChikuraWhite).border(1.dp, ChikuraDim)) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("CHIKUTHREADS", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ChikuraBlack, letterSpacing = 0.8.sp)
                        Text("${selectedThread.domains.size} files · ${selectedThread.domains.sumOf { it.resources.size }} links", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraGray)
                    }
                    HorizontalDivider(color = ChikuraDim, thickness = 1.dp)
                    LazyColumn(modifier = Modifier.weight(1f).padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        item { SidebarItem("ALL DOMAINS", "${selectedThread.domains.sumOf { it.resources.size }}", selectedDomain == null) { selectedDomain = null } }
                        items(selectedThread.domains) { d -> SidebarItem(d.id.uppercase(), "${d.resources.size}", selectedDomain == d.id) { selectedDomain = d.id } }
                    }
                    Box(modifier = Modifier.fillMaxWidth().border(1.dp, ChikuraDim).background(ChikuraLight).padding(10.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Chikura — 知蔵", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ChikuraBlack)
                            Text("knowledge vault · ${selectedThread.name}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraGray)
                        }
                    }
                }

                // Content — now populated, no more 0 files
                Column(modifier = Modifier.weight(1f).fillMaxHeight().background(ChikuraLight).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val visible = if (selectedDomain == null) selectedThread.domains else selectedThread.domains.filter { it.id == selectedDomain }
                    Row(modifier = Modifier.fillMaxWidth().background(ChikuraWhite).border(1.dp, ChikuraBlack).padding(horizontal = 10.dp, vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("BUNKER: ${selectedThread.name} — ${visible.size} domains", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ChikuraBlack)
                        Text("${visible.sumOf { it.resources.size }} resources · ${viewMode.name}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraWhite, modifier = Modifier.background(ChikuraBlack).padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().background(ChikuraWhite).border(1.dp, ChikuraDim).padding(6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(visible) { domain -> DomainBlock(domain, viewMode) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarItem(label: String, count: String, active: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().border(1.dp, if (active) ChikuraBlack else ChikuraDim).background(if (active) ChikuraBlack else ChikuraWhite).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = if (active) ChikuraWhite else ChikuraBlack, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
        Box(modifier = Modifier.border(1.dp, if (active) ChikuraWhite else ChikuraDim).padding(horizontal = 5.dp, vertical = 2.dp)) {
            Text(count, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = if (active) ChikuraWhite else ChikuraGray)
        }
    }
}

@Composable
private fun DomainBlock(domain: Domain, viewMode: ViewMode) {
    var expanded by remember { mutableStateOf(true) }
    Column(modifier = Modifier.fillMaxWidth().background(ChikuraWhite).border(1.dp, ChikuraDim)) {
        Row(modifier = Modifier.fillMaxWidth().background(if (expanded) ChikuraWhite else ChikuraLight).clickable { expanded = !expanded }.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (expanded) "▾" else "▸", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ChikuraBlack, modifier = Modifier.width(16.dp))
            Text(domain.id, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ChikuraBlack)
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.background(ChikuraBlack).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("${domain.resources.size}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraWhite) }
            Spacer(Modifier.weight(1f))
            Text(if (expanded) "—" else "+", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ChikuraGray)
        }
        if (expanded) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                when (viewMode) {
                    ViewMode.LIST -> domain.resources.forEach { res -> ResourceCard(resource = res, onExpand = {}) }
                    ViewMode.KANBAN -> domain.sections.forEach { section ->
                        Column(modifier = Modifier.fillMaxWidth().border(1.dp, ChikuraBlack)) {
                            Text("## ${section.name}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ChikuraWhite, modifier = Modifier.fillMaxWidth().background(ChikuraBlack).padding(horizontal = 8.dp, vertical = 4.dp))
                            Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                section.categories.forEach { cat ->
                                    Text("### ${cat.name}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraGray, modifier = Modifier.background(ChikuraLight).padding(horizontal = 6.dp, vertical = 3.dp))
                                    cat.resources.forEach { res -> ResourceCard(resource = res, onExpand = {}) }
                                }
                            }
                        }
                    }
                    ViewMode.WHITEBOARD -> {
                        // Canvas-like grid
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            domain.resources.chunked(2).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                    row.forEach { res ->
                                        Box(modifier = Modifier.weight(1f).border(1.dp, ChikuraBlack).background(ChikuraWhite).padding(4.dp)) {
                                            ResourceCard(resource = res, onExpand = {})
                                        }
                                    }
                                    if (row.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
