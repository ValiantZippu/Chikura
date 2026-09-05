package com.chikura.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chikura.hydrator.Hydrator
import com.chikura.media.PlaybackStore
import com.chikura.media.extractVideoId
import com.chikura.model.Category
import com.chikura.model.ChikuThread
import com.chikura.model.Domain
import com.chikura.model.Resource
import com.chikura.model.Section
import com.chikura.parser.buildChikuThread
import com.chikura.platform.FileSystem
import com.chikura.media.PlaybackMode
import com.chikura.media.YouTubeEmbed
import com.chikura.media.DownloadPanel
import com.chikura.media.MiniPlayer
import com.chikura.media.MediaQueue
import com.chikura.media.QueueItem
import com.chikura.media.NowPlaying
import com.chikura.media.QueuePanel
import com.chikura.ui.components.QuickCaptureBar
import com.chikura.discord.DiscordChannelSidebar
import com.chikura.discord.DiscordServerIcon
import com.chikura.repo.resolveThreadDir
import com.chikura.ui.components.CreateThreadDialog
import com.chikura.ui.components.ResourceCard
import com.chikura.ui.components.SettingsPanel
import com.chikura.ui.theme.ChikuraLogoHorizontal
import com.chikura.ui.theme.ChikuraMark
import com.chikura.ui.theme.ChikuraLogoSmall
import com.chikura.ui.theme.ChikuraTheme
import com.chikura.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ChikuraBlack = Color(0xFF000000)
private val ChikuraWhite = Color(0xFFFFFFFF)
private val ChikuraGray = Color(0xFF9A9A9A)
private val ChikuraDim = Color(0xFF1A1A1A)
private val ChikuraLight = Color(0xFF0A0A0A)
private val ChikuraBorder = Color(0xFF2A2A2A)
private val ChikuraSurface = Color(0xFF111111)

enum class ViewMode { LIST, WHITEBOARD, BRAINSTORM }
enum class EditMode { VIEW, EDIT }

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

// Smart match: every query token must appear somewhere in the resource
// (url, raw, section, category, domain, type, hydrated title).
private fun matchQuery(r: Resource, query: String, hydraTitle: String?): Boolean {
    if (query.isBlank()) return true
    val tokens = query.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (tokens.isEmpty()) return true
    val hay = listOf(r.url, r.raw, r.section ?: "", r.category ?: "", r.domain, r.typeHint, hydraTitle ?: "").joinToString(" ").lowercase()
    return tokens.all { it in hay }
}

private fun matchType(r: Resource, typeFilter: String): Boolean =
    typeFilter == "ALL" || r.typeHint.equals(typeFilter, ignoreCase = true)

private fun matchSeen(r: Resource, hideSeen: Boolean): Boolean {
    if (!hideSeen) return true
    val vid = extractVideoId(r.url) ?: return true
    return try {
        !PlaybackStore.get(vid).watched
    } catch (_: Exception) {
        true
    }
}

private fun loadRealChikuThread(): ChikuThread {
    val candidates = listOf(
        "ChikuThreads/ValiantZippu/ChikuThread 1",
        "ChikuThreads/ValiantZippu/ChikuThread_1",
        "../ChikuThreads/ValiantZippu/ChikuThread 1",
        "composeApp/src/commonTest/resources/sample-chikuthread"
    )
    for (base in candidates) {
        try {
            if (!FileSystem.exists(base) || !FileSystem.isDirectory(base)) continue
            val files = FileSystem.listFiles(base)
            val mdFiles = files.filter { it.endsWith(".md") && !it.contains("README") }
            if (mdFiles.isEmpty()) continue
            val byDomain = linkedMapOf<String, String>()
            for (abs in mdFiles) {
                val name = abs.substringAfterLast("/").substringAfterLast("\\").removeSuffix(".md")
                if (name.startsWith(".")) continue
                try {
                    val text = FileSystem.readText(abs)
                    val id = name.lowercase()
                    byDomain[id] = (byDomain[id]?.let { it + "\n" + text } ?: text)
                } catch (_: Exception) { }
            }
            // also inbox
            val inboxPath = "$base/archive-box/inbox.md"
            if (FileSystem.exists(inboxPath)) {
                try { byDomain["inbox"] = FileSystem.readText(inboxPath) } catch (_: Exception) {}
            }
            if (byDomain.isEmpty()) continue
            val thread = buildChikuThread(id = "ValiantZippu/ChikuThread 1", name = "Chikura — 知蔵 · ValiantZippu", markdownByDomain = byDomain)
            if (thread.domains.isNotEmpty() && thread.domains.sumOf { it.resources.size } > 11) return thread
        } catch (_: Exception) { }
    }
    return sampleThread()
}

private fun syncThreadToDisk(thread: ChikuThread): Boolean {
    return try {
        val base = "ChikuThreads/ValiantZippu/ChikuThread 1"
        if (!FileSystem.exists(base)) return false
        for (domain in thread.domains) {
            val sb = StringBuilder()
            sb.append("# ${domain.name}\n\n")
            // Group by section/category to preserve hierarchy
            val bySection = domain.resources.groupBy { it.section ?: "General" }
            for ((section, resList) in bySection) {
                sb.append("## $section\n\n")
                val byCat = resList.groupBy { it.category ?: "Uncategorized" }
                for ((cat, list) in byCat) {
                    if (cat != "Uncategorized") sb.append("### $cat\n\n")
                    for (r in list) {
                        sb.append("- ${r.raw}\n")
                    }
                    sb.append("\n")
                }
            }
            val path = "$base/${domain.id}.md"
            FileSystem.writeText(path, sb.toString())
        }
        true
    } catch (_: Exception) { false }
}

@Composable
fun App() {
    // Fast sample first so the window paints instantly; the real 1005-link
    // parse runs on Dispatchers.Default and swaps in when ready.
    var selectedThread by remember { mutableStateOf(sampleThread()) }
    var selectedDomain by remember { mutableStateOf<String?>(null) }
    var viewMode by remember { mutableStateOf(ViewMode.LIST) }
    var editMode by remember { mutableStateOf(EditMode.VIEW) }
    var incognito by remember { mutableStateOf(true) }
    var settingsOpen by remember { mutableStateOf(false) }
    var downloadOpen by remember { mutableStateOf(false) }
    var githubConnected by remember { mutableStateOf(false) }
    var repoInput by remember { mutableStateOf("ValiantZippu/Chikura") }
    var searchQuery by remember { mutableStateOf("") }
    // Debounced query drives filtering: typing stays at 60fps, the 1005-link
    // filter + recompose runs 250ms after the last keystroke.
    var searchDebounced by remember { mutableStateOf("") }
    LaunchedEffect(searchQuery) {
        delay(250)
        searchDebounced = searchQuery
    }
    var collapsed by remember { mutableStateOf(emptySet<String>()) }
    var typeFilter by remember { mutableStateOf("ALL") }
    var hideSeen by remember { mutableStateOf(false) }
    var selectedSection by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var sidebarWidth by remember { mutableStateOf(230.dp) }
    var queueOpen by remember { mutableStateOf(false) }
    var quickCaptureOpen by remember { mutableStateOf(false) }
    var showCreate by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        val real = withContext(Dispatchers.Default) { loadRealChikuThread() }
        selectedThread = real
        isLoading = false
    }
    val reloadThread = {
        if (!isLoading) {
            isLoading = true
            scope.launch(Dispatchers.Default) {
                val real = loadRealChikuThread()
                selectedThread = real
                isLoading = false
            }
        }
    }

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
                        ChikuraLogoHorizontal(markSize = 26.dp, showJp = true)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF111111)).border(1.dp, Color(0xFF222222), RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text("⬢", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ChikuraGray)
                            Text(repoInput, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White)
                        }
                        Box(modifier = Modifier.width(180.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFF111111)).border(1.dp, Color(0xFF222222), RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                            androidx.compose.foundation.text.BasicTextField(value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White), singleLine = true, decorationBox = { inner ->
                                if (searchQuery.isEmpty()) androidx.compose.material3.Text("Search or create a post...", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color(0xFF3A3A3A))
                                inner()
                            })
                        }
                        val editBg by animateColorAsState(if (editMode == EditMode.EDIT) Color.White else Color(0xFF111111), tween(200), label = "editBg")
                        val editFg by animateColorAsState(if (editMode == EditMode.EDIT) Color.Black else Color.White, tween(200), label = "editFg")
                        Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(editBg).border(1.dp, if (editMode == EditMode.EDIT) Color.White else Color(0xFF2A2A2A), RoundedCornerShape(999.dp)).clickable { editMode = if (editMode == EditMode.VIEW) EditMode.EDIT else EditMode.VIEW }.padding(horizontal = 14.dp, vertical = 6.dp)) {
                            Text(if (editMode == EditMode.VIEW) "VIEW" else "EDIT", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = editFg, fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFF111111)).border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(999.dp)).clickable { downloadOpen = true }, contentAlignment = Alignment.Center) {
                            Text("DL", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color(0xFF00D084), fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFF111111)).border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(999.dp)).clickable { quickCaptureOpen = !quickCaptureOpen }, contentAlignment = Alignment.Center) {
                            Text("+", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color(0xFF00D084), fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFF111111)).border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(999.dp)).clickable { settingsOpen = true }, contentAlignment = Alignment.Center) {
                            Text("SET", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        var hovered by remember { mutableStateOf(false) }
                        val scale by animateFloatAsState(if (hovered) 1.04f else 1f, spring(dampingRatio = 0.7f, stiffness = 600f), label = "loadScale")
                        Box(modifier = Modifier.scale(scale).clip(RoundedCornerShape(999.dp)).background(Color.White).border(1.dp, Color.White, RoundedCornerShape(999.dp)).clickable {
                            reloadThread()
                        }.padding(horizontal = 18.dp, vertical = 7.dp)) {
                            Text(if (isLoading) "…" else "LOAD", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF111111)).border(1.dp, Color(0xFF222222), RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                            Text("${selectedThread.domains.size} files · ${selectedThread.domains.sumOf { it.resources.size }} links", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraGray)
                        }

                        if (READ_ONLY) Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color.White).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("READ_ONLY", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF0A0A0A)).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(16.dp)).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val incBg by animateColorAsState(if (incognito) Color(0xFF00D084) else Color(0xFF1A1A1A), tween(200), label = "incBg")
                    val incFg by animateColorAsState(if (incognito) Color.Black else Color.White, tween(200), label = "incFg")
                    Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(incBg).border(1.dp, if (incognito) Color(0xFF00D084) else Color(0xFF333333), RoundedCornerShape(999.dp)).clickable { incognito = !incognito }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(if (incognito) "INCOGNITO" else "SIGNED IN", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = incFg, fontWeight = FontWeight.Bold)
                    }
                    Text("no YT sync", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = ChikuraGray)
                    Spacer(Modifier.weight(1f))
                    Text("EDIT MODE", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = ChikuraGray)
                }
                // View tabs — AMOLED rounded pill (LIST = linear, WHITEBOARD = infinite nodes)
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF0A0A0A)).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(16.dp)).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    ViewMode.values().forEach { mode ->
                        val active = mode == viewMode
                        val label = when(mode) { ViewMode.LIST -> "≡ LIST"; ViewMode.WHITEBOARD -> "⬢ WHITEBOARD · ∞"; ViewMode.BRAINSTORM -> "✎ BRAINSTORM" }
                        val bg by animateColorAsState(if (active) Color.White else Color.Transparent, tween(220), label = "tabBg")
                        val fg by animateColorAsState(if (active) Color.Black else Color(0xFF9A9A9A), tween(220), label = "tabFg")
                        Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(bg).border(1.dp, if (active) Color.White else Color(0xFF1A1A1A), RoundedCornerShape(999.dp)).clickable { viewMode = mode }.padding(horizontal = 16.dp, vertical = 7.dp)) {
                            Text(label, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = fg, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text("${getPlatformName()} • ${viewMode.name} · ${if(viewMode==ViewMode.WHITEBOARD) "∞ canvas" else "linear"}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraGray, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF111111)).padding(horizontal = 8.dp, vertical = 4.dp))
                }

                Row(modifier = Modifier.weight(1f).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Sidebar — AMOLED true black, rounded 20, super smooth, resizable
                    Column(modifier = Modifier.width(sidebarWidth).fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(Color(0xFF0A0A0A)).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(20.dp))) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("CHIKUTHREADS", fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.2.sp)
                            Text("${selectedThread.domains.size} megathreads · ${selectedThread.domains.sumOf { it.resources.size }} links", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = ChikuraGray)
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1A1A1A)))
                        LazyColumn(modifier = Modifier.weight(1f).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            item { SidebarItem("ALL DOMAINS", "${selectedThread.domains.sumOf { it.resources.size }}", selectedDomain == null) { selectedDomain = null; selectedSection = null } }
                            items(selectedThread.domains) { d -> SidebarItem(d.id.uppercase(), "${d.resources.size}", selectedDomain == d.id) { selectedDomain = d.id; selectedSection = null } }
                            if (selectedDomain != null) {
                                val sel = selectedThread.domains.find { it.id == selectedDomain }
                                if (sel != null && sel.sections.isNotEmpty()) {
                                    item {
                                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(Color(0xFF1A1A1A)))
                                    }
                                    item {
                                        // Real channels: ## sections with their ### categories.
                                        // Row id "Sec|||Cat" filters the content to that slice.
                                        val chans = sel.sections.associate { sec ->
                                            sec.name to sec.categories.map { cat ->
                                                com.chikura.discord.ChannelItem("${sec.name}|||${cat.name}", cat.name)
                                            }
                                        }
                                        DiscordChannelSidebar(
                                            domains = sel.sections.map { it.name },
                                            channels = chans,
                                            selectedChannel = selectedSection,
                                            onSelectChannel = { id -> selectedSection = if (selectedSection == id) null else id }
                                        )
                                    }
                                }
                            }
                        }
                        // Create megathread button
                        val createBtnMod = Modifier.fillMaxWidth().padding(8.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF111111)).border(1.dp, Color(0xFF222222), RoundedCornerShape(12.dp)).clickable { showCreate = true }.padding(horizontal = 12.dp, vertical = 10.dp)
                        Box(modifier = createBtnMod, contentAlignment = Alignment.Center) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("+", fontFamily = FontFamily.Monospace, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("New Megathread", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = ChikuraGray, fontWeight = FontWeight.Medium)
                            }
                        }
                        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).clip(RoundedCornerShape(10.dp)).padding(6.dp)) {
                            Text("${selectedThread.domains.size} megathreads · ${selectedThread.domains.sumOf { it.resources.size }} links", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF4A4A4A))
                        }
                    }
                    // Resizable handle — drag to adjust sidebar
                    val density = LocalDensity.current
                    Box(
                        modifier = Modifier.width(10.dp).fillMaxHeight().clip(RoundedCornerShape(999.dp)).background(Color.Transparent).pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                with(density) { sidebarWidth = (sidebarWidth + dragAmount.x.toDp()).coerceIn(180.dp, 380.dp) }
                            }
                        }.padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.width(2.dp).fillMaxHeight(0.12f).clip(RoundedCornerShape(999.dp)).background(Color(0xFF2A2A2A)))
                    }

                    // Content — AMOLED, rounded 20, smooth
                    Column(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(Color(0xFF0A0A0A)).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(20.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val baseVisible = if (selectedDomain == null) selectedThread.domains else selectedThread.domains.filter { it.id == selectedDomain }
                        val hydraTitles = remember(selectedThread) { Hydrator.cache.asMap() }
                        fun matchRes(r: Resource): Boolean =
                            matchQuery(r, searchDebounced, hydraTitles[r.url]?.title) && matchType(r, typeFilter) && matchSeen(r, hideSeen)
                        val queried = baseVisible.map { d ->
                            d.copy(
                                resources = d.resources.filter { matchRes(it) },
                                sections = d.sections.map { s ->
                                    s.copy(categories = s.categories.map { c ->
                                        c.copy(resources = c.resources.filter { matchRes(it) })
                                    }.filter { it.resources.isNotEmpty() })
                                }.filter { it.categories.isNotEmpty() }
                            )
                        }.filter { it.resources.isNotEmpty() || it.sections.isNotEmpty() }
                        val visible = if (selectedSection == null) queried else queried.map { d ->
                            val secPart = selectedSection!!.substringBefore("|||")
                            val catPart = selectedSection!!.substringAfter("|||", "")
                            d.copy(
                                sections = d.sections.filter { it.name == secPart }.map { s ->
                                    if (catPart.isEmpty()) s else s.copy(categories = s.categories.filter { it.name == catPart })
                                },
                                resources = d.resources.filter { r -> r.section == secPart && (catPart.isEmpty() || r.category == catPart) }
                            )
                        }.filter { it.resources.isNotEmpty() || it.sections.isNotEmpty() }
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("BUNKER: ${selectedThread.name} — ${visible.size} domains", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color.Black).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text("${visible.sumOf { it.resources.size }} resources · ${viewMode.name}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            listOf("ALL", "VIDEO", "SHORTS", "CHANNEL", "WEBSITE", "BARE").forEach { t ->
                                FilterPill(label = t, active = typeFilter == t) { typeFilter = t }
                            }
                            Box(modifier = Modifier.width(1.dp).height(14.dp).background(Color(0xFF2A2A2A)))
                            FilterPill(label = "HIDE SEEN", active = hideSeen) { hideSeen = !hideSeen }
                            if (selectedSection != null) FilterPill(label = "X " + selectedSection!!.replace("|||", " / ").take(28), active = true) { selectedSection = null }
                        }
                        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.Black).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(16.dp)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Flat lazy items: only visible cards compose, so only visible
                            // cards fire thumbnail/hydration fetches. (A Column of all
                            // 481 cards composed everything at once: the slowness +
                            // thumbnail failures.)
                            visible.forEach { domain ->
                                val isCollapsed = collapsed.contains(domain.id)
                                item(key = "hdr-${domain.id}") {
                                    Box(Modifier.fillMaxWidth().animateItem()) {
                                        DomainHeader(domain = domain, expanded = !isCollapsed, onToggle = {
                                            collapsed = if (isCollapsed) collapsed - domain.id else collapsed + domain.id
                                        })
                                    }
                                }
                                if (!isCollapsed) {
                                    when (viewMode) {
                                        ViewMode.LIST -> items(domain.resources, key = { r -> "res-${r.id}" }) { res ->
                                            Box(Modifier.fillMaxWidth().animateItem()) {
                                                ResourceCard(resource = res, editMode = editMode, incognito = incognito, onExpand = {})
                                            }
                                        }
                                        ViewMode.WHITEBOARD -> item(key = "wb-${domain.id}") {
                                            Box(Modifier.fillMaxWidth().animateItem()) {
                                                InfiniteWhiteboard(domain, editMode, incognito)
                                            }
                                        }
                                        ViewMode.BRAINSTORM -> item(key = "brain-${domain.id}") {
                                            Box(Modifier.fillMaxWidth().animateItem()) {
                                                BrainstormWorkspace(domain = domain, editMode = editMode)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // Discord-style mini player at bottom
                MiniPlayer(
                    isPlaying = NowPlaying.videoId != null,
                    onStop = {
                        NowPlaying.videoId = null
                        MediaQueue.clear()
                    },
                    onPlayPause = { },
                    onNext = {
                        val next = MediaQueue.next()
                        if (next != null) NowPlaying.videoId = next.videoId
                    },
                    onPrevious = {
                        val prev = MediaQueue.previous()
                        if (prev != null) NowPlaying.videoId = prev.videoId
                    },
                    onQueueOpen = { queueOpen = true }
                )
                QueuePanel(
                    isVisible = queueOpen,
                    onClose = { queueOpen = false },
                    onPlayItem = { videoId ->
                        NowPlaying.videoId = videoId
                    }
                )
                QuickCaptureBar(
                    domains = selectedThread.domains.map { it.id },
                    isVisible = quickCaptureOpen,
                    onDismiss = { quickCaptureOpen = false },
                    onSave = { url, note, domain ->
                        // TODO: Save to ChikuThread domain
                    }
                )
                DownloadPanel(
                    isVisible = downloadOpen,
                    onClose = { downloadOpen = false }
                )
                CreateThreadDialog(
                    isOpen = showCreate,
                    threadDir = resolveThreadDir(),
                    onClose = { showCreate = false },
                    onCreated = { reloadThread() }
                )
                SettingsPanel(
                    isOpen = settingsOpen,
                    onClose = { settingsOpen = false },
                    githubConnected = githubConnected,
                    onConnectGitHub = { githubConnected = true },
                    onCreateRepo = { githubConnected = true },
                    onConnectPrivateRepo = { githubConnected = true },
                    onSync = {
                        val ok = syncThreadToDisk(selectedThread)
                        githubConnected = ok
                        settingsOpen = false
                    },
                    incognito = incognito,
                    onIncognitoToggle = { incognito = it }
                )
            }
        }
    }
}

@Composable
private fun FilterPill(label: String, active: Boolean, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val hov by src.collectIsHoveredAsState()
    val bg by animateColorAsState(if (active) Color.White else if (hov) Color(0xFF1E1E1E) else Color(0xFF111111), tween(160), label = "pillBg")
    val bd by animateColorAsState(if (active || hov) Color.White else Color(0xFF2A2A2A), tween(160), label = "pillBd")
    Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(bg).border(1.dp, bd, RoundedCornerShape(999.dp)).clickable(interactionSource = src, indication = null, onClick = onClick).padding(horizontal = 10.dp, vertical = 5.dp)) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = if (active) Color.Black else if (hov) Color.White else ChikuraGray, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun SidebarItem(label: String, count: String, active: Boolean, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val hov by src.collectIsHoveredAsState()
    val bg by animateColorAsState(if (active) Color.White else if (hov) Color(0xFF1C1C1C) else Color(0xFF111111), tween(180), label = "sbBg")
    val fg by animateColorAsState(if (active) Color.Black else Color.White, tween(180), label = "sbFg")
    val bd by animateColorAsState(if (active || hov) Color.White else Color(0xFF1A1A1A), tween(180), label = "sbBd")
    val shift by animateFloatAsState(if (hov && !active) 4f else 0f, tween(180), label = "sbShift")
    Row(
        modifier = Modifier.fillMaxWidth().offset(x = shift.dp).clip(RoundedCornerShape(12.dp)).background(bg).border(1.dp, bd, RoundedCornerShape(12.dp)).clickable(interactionSource = src, indication = null, onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = fg, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
        Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (active) Color.Black else Color(0xFF1A1A1A)).padding(horizontal = 7.dp, vertical = 3.dp)) {
            Text(count, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = if (active) Color.White else ChikuraGray, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DomainHeader(domain: Domain, expanded: Boolean, onToggle: () -> Unit) {
    val bg by animateColorAsState(if (expanded) Color(0xFF111111) else Color(0xFF0A0A0A), tween(220), label = "domBg")
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bg).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(16.dp))) {
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (expanded) Color(0xFF111111) else Color.Transparent).clickable { onToggle() }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
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
    }
}

@Composable
private fun InfiniteWhiteboard(domain: Domain, editMode: EditMode, incognito: Boolean) {
    val sections = if (domain.sections.isNotEmpty()) domain.sections else listOf(com.chikura.model.Section("General", listOf(com.chikura.model.Category("All", domain.resources))))
    var offset by remember { mutableStateOf(Offset.Zero) }
    var scale by remember { mutableStateOf(1f) }
    // Single pan source: transformable only. (A second manual drag handler here
    // used to double every pan, which made the board feel weird/shaky.)
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.6f, 2.2f)
        offset += panChange
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(560.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .transformable(transformState)
                .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                sections.forEach { section ->
                    val all = section.categories.flatMap { it.resources }
                    // Light nodes, capped: full ResourceCards here meant hundreds of
                    // hydrate/detail fetches + animations per board (the lag).
                    val shown = all.take(10)
                    val rest = all.size - shown.size
                    Column(
                        modifier = Modifier.width(240.dp).fillMaxHeight().clip(RoundedCornerShape(14.dp)).background(Color(0xFF0A0A0A)).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(14.dp)).padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White).padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(section.name, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
                            Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color.Black).padding(horizontal = 7.dp, vertical = 3.dp)) { Text("${all.size}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                        }
                        shown.forEach { res -> WhiteboardNode(resource = res) }
                        if (rest > 0) Text("+ $rest more in LIST", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraGray, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF111111)).padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
        }
        Row(modifier = Modifier.align(Alignment.TopStart).padding(10.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFF111111)).border(1.dp, Color(0xFF222222), RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("WHITEBOARD", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Text(domain.id + " " + sections.size.toString() + " sections drag/pinch", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF9A9A9A))
        }
        Box(modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).clip(RoundedCornerShape(999.dp)).background(Color.White).padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text((scale*100).toInt().toString() + "% " + offset.x.toInt().toString() + "," + offset.y.toInt().toString(), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

// Lightweight board node: cached thumbnail + labels only. No Hydrator/detail
// fetches here — those belong to the LIST cards. Keeps the board at 60fps.
@Composable
private fun WhiteboardNode(resource: Resource) {
    val vid = remember(resource.url) { com.chikura.media.extractVideoId(resource.url) }
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF111111)).border(1.dp, Color(0xFF222222), RoundedCornerShape(12.dp)).clickable {
            if (vid != null) com.chikura.media.openVideoUrl(com.chikura.media.youtubeWatchUrlAt(vid, 0))
            else com.chikura.media.openVideoUrl(resource.url)
        }
    ) {
        if (vid != null) {
            Box(modifier = Modifier.fillMaxWidth().height(84.dp).background(Color(0xFF080808)), contentAlignment = Alignment.Center) {
                com.chikura.media.VideoThumbnail(imageUrl = com.chikura.media.youtubeThumbUrl(vid), modifier = Modifier.fillMaxSize())
                Box(modifier = Modifier.size(30.dp).clip(RoundedCornerShape(999.dp)).background(Color.White.copy(alpha = 0.94f)), contentAlignment = Alignment.Center) {
                    Text(">", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(resource.category ?: resource.section ?: resource.typeHint, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(resource.url, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraGray, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

// Brainstorm workspace — Notion-style note area for each domain
@Composable
private fun BrainstormWorkspace(domain: Domain, editMode: EditMode) {
    var blocks by remember(domain.id) { mutableStateOf(
        listOf<com.chikura.notion.NotionBlock>(
            com.chikura.notion.NotionBlock.Heading(level = 1, text = domain.name.uppercase()),
            com.chikura.notion.NotionBlock.Paragraph(text = "Brainstorm notes for ${domain.name}. Use /commands to add blocks."),
            com.chikura.notion.NotionBlock.Divider(),
            com.chikura.notion.NotionBlock.Callout(emoji = "💡", text = "Add your thoughts, ideas, and notes here."),
            com.chikura.notion.NotionBlock.Paragraph(text = ""),
        )
    ) }

    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0A0A0A)).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Domain header
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF111111)).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("✎", fontSize = 16.sp)
                Text(domain.name.uppercase(), fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                    color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text("${domain.resources.size} resources · BRAINSTORM", fontFamily = FontFamily.Monospace,
                fontSize = 8.sp, color = ChikuraGray)
        }

        // Notion-style block editor
        com.chikura.notion.NotionEditor(
            blocks = blocks,
            onBlocksChange = { blocks = it },
            modifier = Modifier.weight(1f)
        )

        // Resource links for this domain
        Column(
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF111111)).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("LINKED RESOURCES · ${domain.resources.size}", fontFamily = FontFamily.Monospace,
                fontSize = 9.sp, color = ChikuraGray, fontWeight = FontWeight.Bold)
            domain.resources.take(8).forEach { res ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0A0A0A)).clickable { com.chikura.media.openVideoUrl(res.url) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("→", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraGray)
                    Text(res.url.take(60), fontFamily = FontFamily.Monospace, fontSize = 8.sp,
                        color = Color(0xFFE0E0E0), maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f))
                    Text(res.typeHint.uppercase().take(4), fontFamily = FontFamily.Monospace,
                        fontSize = 7.sp, color = ChikuraGray)
                }
            }
            if (domain.resources.size > 8) {
                Text("+ ${domain.resources.size - 8} more", fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp, color = ChikuraGray)
            }
        }
    }
}
