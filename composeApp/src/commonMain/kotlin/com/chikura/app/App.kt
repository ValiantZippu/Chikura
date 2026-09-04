package com.chikura.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.chikura.ui.theme.*

private val ChikuraBlack = Color(0xFF000000)
private val ChikuraWhite = Color(0xFFFFFFFF)
private val ChikuraGray = Color(0xFF9A9A9A)
private val ChikuraDim = Color(0xFF1A1A1A)
private val ChikuraLight = Color(0xFF0A0A0A)
private val ChikuraBorder = Color(0xFF2A2A2A)
private val ChikuraSurface = Color(0xFF111111)

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
            res("music-0003","https://soundcloud.com/artist/track","music","Production","Mix"),
            res("music-0004","https://open.spotify.com/track/abc","music","Production","Mix")
        )
    )
    val games = Domain(
        id = "games",
        name = "games",
        sections = listOf(Section("FPS", listOf(Category("Aim", listOf(res("games-0001","https://youtu.be/9bZkp7q19f0","games","FPS","Aim")))))),
        resources = listOf(res("games-0001","https://youtu.be/9bZkp7q19f0","games","FPS","Aim"), res("games-0002","https://store.steampowered.com/app/730","games","FPS","Aim"), res("games-0003","https://twitch.tv/shroud","games","FPS","Aim"))
    )
    val tech = Domain(
        id = "technology",
        name = "technology",
        sections = listOf(Section("Tools", listOf(Category("Editors", listOf(res("tech-0001","https://github.com/JetBrains/compose-multiplatform","technology","Tools","Editors")))))),
        resources = listOf(res("tech-0001","https://github.com/JetBrains/compose-multiplatform","technology","Tools","Editors"), res("tech-0002","https://kotlinlang.org","technology","Tools","Editors"))
    )
    val japan = Domain(
        id = "japan",
        name = "japan",
        sections = emptyList(),
        resources = listOf(res("japan-0001","https://youtu.be/jNQXAC9IVRw","japan",null,null), res("japan-0002","https://japan-guide.com","japan",null,null))
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
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF000000))) {
            Column(modifier = Modifier.fillMaxSize().background(Color(0xFF000000))) {
                // AMOLED Header — true black, rounded 16, super smooth
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF0A0A0A)).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Box(modifier = Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(Color.White), contentAlignment = Alignment.Center) { Text("蔵", fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = Color.Black, fontWeight = FontWeight.Bold) }
                            Text("CHIKURA", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            Text("知蔵", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ChikuraGray)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF111111)).border(1.dp, Color(0xFF222222), RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text("⬢", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ChikuraGray)
                            Text(repoInput, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White)
                        }
                        var hovered by remember { mutableStateOf(false) }
                        val scale by animateFloatAsState(if (hovered) 1.04f else 1f, spring(dampingRatio = 0.7f, stiffness = 600f), label = "loadScale")
                        Box(modifier = Modifier.scale(scale).clip(RoundedCornerShape(999.dp)).background(Color.White).border(1.dp, Color.White, RoundedCornerShape(999.dp)).clickable {
                            isLoading = true
                            selectedThread = sampleThread()
                            isLoading = false
                        }.padding(horizontal = 18.dp, vertical = 7.dp)) {
                            Text(if (isLoading) "…" else "LOAD", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF111111)).border(1.dp, Color(0xFF222222), RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                            Text("${selectedThread.domains.size} files · ${selectedThread.domains.sumOf { it.resources.size }} links", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraGray)
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(999.dp)).background(Color.Transparent).clickable {}.padding(horizontal = 10.dp, vertical = 5.dp)) {
                            Text("VIEW ON GITHUB ↗", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraGray)
                        }
                        if (READ_ONLY) Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color.White).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("READ_ONLY", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // View tabs — AMOLED rounded pill
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF0A0A0A)).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(16.dp)).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    ViewMode.values().forEach { mode ->
                        val active = mode == viewMode
                        val label = when(mode) { ViewMode.LIST -> "≡ LIST"; ViewMode.KANBAN -> "▦ KANBAN"; ViewMode.WHITEBOARD -> "⬢ WHITEBOARD" }
                        val bg by animateColorAsState(if (active) Color.White else Color.Transparent, tween(220), label = "tabBg")
                        val fg by animateColorAsState(if (active) Color.Black else Color(0xFF9A9A9A), tween(220), label = "tabFg")
                        Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(bg).border(1.dp, if (active) Color.White else Color(0xFF1A1A1A), RoundedCornerShape(999.dp)).clickable { viewMode = mode }.padding(horizontal = 16.dp, vertical = 7.dp)) {
                            Text(label, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = fg, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text("${getPlatformName()} • ${viewMode.name}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraGray, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF111111)).padding(horizontal = 8.dp, vertical = 4.dp))
                }

                Row(modifier = Modifier.weight(1f).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Sidebar — AMOLED true black, rounded 20, super smooth
                    Column(modifier = Modifier.width(230.dp).fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(Color(0xFF0A0A0A)).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(20.dp))) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("CHIKUTHREADS", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.2.sp)
                            Text("${selectedThread.domains.size} files · ${selectedThread.domains.sumOf { it.resources.size }} links", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraGray)
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1A1A1A)))
                        LazyColumn(modifier = Modifier.weight(1f).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            item { SidebarItem("ALL DOMAINS", "${selectedThread.domains.sumOf { it.resources.size }}", selectedDomain == null) { selectedDomain = null } }
                            items(selectedThread.domains) { d -> SidebarItem(d.id.uppercase(), "${d.resources.size}", selectedDomain == d.id) { selectedDomain = d.id } }
                        }
                        Box(modifier = Modifier.fillMaxWidth().padding(8.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF111111)).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(14.dp)).padding(10.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Chikura — 知蔵", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("AMOLED vault · ${selectedThread.name}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraGray)
                            }
                        }
                    }

                    // Content — AMOLED, rounded 20, smooth
                    Column(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(Color(0xFF0A0A0A)).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(20.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val visible = if (selectedDomain == null) selectedThread.domains else selectedThread.domains.filter { it.id == selectedDomain }
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("BUNKER: ${selectedThread.name} — ${visible.size} domains", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color.Black).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text("${visible.sumOf { it.resources.size }} resources · ${viewMode.name}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.Black).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(16.dp)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(visible) { domain -> DomainBlock(domain, viewMode) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarItem(label: String, count: String, active: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (active) Color.White else Color(0xFF111111), tween(200), label = "sbBg")
    val fg by animateColorAsState(if (active) Color.Black else Color.White, tween(200), label = "sbFg")
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(bg).border(1.dp, if (active) Color.White else Color(0xFF1A1A1A), RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = fg, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
        Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (active) Color.Black else Color(0xFF1A1A1A)).padding(horizontal = 7.dp, vertical = 3.dp)) {
            Text(count, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = if (active) Color.White else ChikuraGray, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DomainBlock(domain: Domain, viewMode: ViewMode) {
    var expanded by remember { mutableStateOf(true) }
    val bg by animateColorAsState(if (expanded) Color(0xFF111111) else Color(0xFF0A0A0A), tween(220), label = "domBg")
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bg).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(16.dp)).animateContentSize(spring(dampingRatio = 0.8f, stiffness = 400f))) {
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (expanded) Color(0xFF111111) else Color.Transparent).clickable { expanded = !expanded }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(8.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                Text(if (expanded) "▾" else "▸", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Text(domain.id, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color.White).padding(horizontal = 8.dp, vertical = 3.dp)) { Text("${domain.resources.size}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.weight(1f))
            Text(if (expanded) "—" else "+", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = ChikuraGray)
        }
        AnimatedVisibility(visible = expanded, enter = fadeIn(tween(220)) + expandVertically(tween(320, easing = FastOutSlowInEasing)), exit = fadeOut(tween(180)) + shrinkVertically()) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (viewMode) {
                    ViewMode.LIST -> domain.resources.forEach { res -> ResourceCard(resource = res, onExpand = {}) }
                    ViewMode.KANBAN -> domain.sections.forEach { section ->
                        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF0A0A0A)).border(1.dp, Color(0xFF222222), RoundedCornerShape(14.dp))) {
                            Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text("## ${section.name}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                section.categories.forEach { cat ->
                                    Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF111111)).border(1.dp, Color(0xFF222222), RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text("### ${cat.name}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraGray)
                                    }
                                    cat.resources.forEach { res -> ResourceCard(resource = res, onExpand = {}) }
                                }
                            }
                        }
                    }
                    ViewMode.WHITEBOARD -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            domain.resources.chunked(2).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    row.forEach { res ->
                                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Color(0xFF111111)).border(1.dp, Color(0xFF222222), RoundedCornerShape(14.dp)).padding(6.dp)) {
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
