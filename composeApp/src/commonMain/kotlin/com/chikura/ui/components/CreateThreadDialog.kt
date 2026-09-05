package com.chikura.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chikura.repo.AuthorResult
import com.chikura.repo.authoringGuideSteps
import com.chikura.repo.chikuThreadsRoot
import com.chikura.repo.createDomainFile
import com.chikura.repo.createThread
import com.chikura.repo.domainTemplate
import com.chikura.repo.kebabCase

private enum class CreateTab { DOMAIN, THREAD, GUIDE }

// Megathread servers: create + guided authoring, same AMOLED system.
@Composable
fun CreateThreadDialog(
    isOpen: Boolean,
    threadDir: String?,
    onClose: () -> Unit,
    onCreated: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tab by remember(isOpen) { mutableStateOf(CreateTab.DOMAIN) }
    var domainName by remember(isOpen) { mutableStateOf("") }
    var authorName by remember(isOpen) { mutableStateOf("") }
    var threadName by remember(isOpen) { mutableStateOf("") }
    var message by remember(isOpen) { mutableStateOf<String?>(null) }
    var messageOk by remember(isOpen) { mutableStateOf(false) }

    AnimatedVisibility(visible = isOpen, enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }), exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })) {
        Box(modifier = modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable { onClose() }, contentAlignment = Alignment.CenterEnd) {
            Column(
                modifier = Modifier.fillMaxHeight().width(460.dp).clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)).background(Color(0xFF0A0A0A)).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)).clickable(enabled = false) {}.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("NEW MEGATHREAD", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color.White).clickable { onClose() }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("CLOSE X", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CreateTab.values().forEach { t ->
                        val active = t == tab
                        Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (active) Color.White else Color(0xFF111111)).border(1.dp, if (active) Color.White else Color(0xFF2A2A2A), RoundedCornerShape(999.dp)).clickable { tab = t; message = null }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(t.name, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = if (active) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                AnimatedContent(targetState = tab, transitionSpec = { fadeIn() + slideInHorizontally { it / 4 } togetherWith fadeOut() }, label = "createTab") { current ->
                    when (current) {
                        CreateTab.DOMAIN -> DomainTab(
                            threadDir = threadDir,
                            name = domainName,
                            onName = { domainName = it; message = null },
                            message = message,
                            messageOk = messageOk,
                            onCreate = {
                                val dir = threadDir
                                if (dir == null) {
                                    message = "No thread loaded — LOAD one first."
                                    messageOk = false
                                } else {
                                    when (val r = createDomainFile(dir, domainName)) {
                                        is AuthorResult.Ok -> {
                                            message = "Created ${r.path.substringAfterLast("/")}"
                                            messageOk = true
                                            onCreated()
                                        }
                                        is AuthorResult.Err -> {
                                            message = r.message
                                            messageOk = false
                                        }
                                    }
                                }
                            }
                        )
                        CreateTab.THREAD -> ThreadTab(
                            threadDir = threadDir,
                            author = authorName,
                            thread = threadName,
                            onAuthor = { authorName = it; message = null },
                            onThread = { threadName = it; message = null },
                            message = message,
                            messageOk = messageOk,
                            onCreate = {
                                val dir = threadDir
                                val root = dir?.let { chikuThreadsRoot(it) }
                                if (root == null) {
                                    message = "No thread loaded — LOAD one first."
                                    messageOk = false
                                } else {
                                    when (val r = createThread(root, authorName, threadName)) {
                                        is AuthorResult.Ok -> {
                                            message = "Created ${r.path}"
                                            messageOk = true
                                            onCreated()
                                        }
                                        is AuthorResult.Err -> {
                                            message = r.message
                                            messageOk = false
                                        }
                                    }
                                }
                            }
                        )
                        CreateTab.GUIDE -> GuideTab()
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("Markdown is the DB · git is the truth · UI is the window.", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color(0xFF3A3A3A))
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String, onValue: (String) -> Unit, hint: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFF9A9A9A), fontWeight = FontWeight.Bold)
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.Black).border(1.dp, Color(0xFF222222), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 2.dp)) {
            TextField(
                value = value, onValueChange = onValue, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(hint, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF3A3A3A)) },
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = Color.White, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp), singleLine = true
            )
        }
    }
}

@Composable
private fun StatusLine(message: String?, ok: Boolean) {
    if (message == null) return
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (ok) Color(0xFF00D084).copy(alpha = 0.12f) else Color(0xFFFF3B30).copy(alpha = 0.12f)).border(1.dp, if (ok) Color(0xFF00D084) else Color(0xFFFF3B30), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(message, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = if (ok) Color(0xFF00D084) else Color(0xFFFF8A80))
    }
}

@Composable
private fun DomainTab(threadDir: String?, name: String, onName: (String) -> Unit, message: String?, messageOk: Boolean, onCreate: () -> Unit) {
    val preview = kebabCase(name).ifBlank { "your-domain" }
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("New domain file in the loaded thread", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Field("Domain name", name, onName, "vocal-mixing")
        Text("File: $preview.md", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFF6B6B6B))
        Text("Template preview", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFF9A9A9A), fontWeight = FontWeight.Bold)
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.Black).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(10.dp)).padding(10.dp)) {
            Text(domainTemplate("Preview").take(420), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color(0xFF6B6B6B), lineHeight = 13.sp)
        }
        StatusLine(message, messageOk)
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(999.dp)).background(Color.White).clickable { onCreate() }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text("CREATE $preview.md", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
        }
        if (threadDir == null) Text("Tip: LOAD a thread first — the file lands beside its domains.", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color(0xFF6B6B6B))
    }
}

@Composable
private fun ThreadTab(threadDir: String?, author: String, thread: String, onAuthor: (String) -> Unit, onThread: (String) -> Unit, message: String?, messageOk: Boolean, onCreate: () -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("New author thread with inbox workflow", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Field("Author", author, onAuthor, "valiantzippu")
        Field("Thread", thread, onThread, "chikuthread-1")
        val authorId = kebabCase(author).ifBlank { "author" }
        val threadId = kebabCase(thread).ifBlank { "thread" }
        Text("ChikuThreads/$authorId/$threadId/", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFF6B6B6B))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("thread.json — metadata", "archive-box/inbox.md — bulk intake", "archive-box/quarantine.md — unsure links", "start-here.md — first domain").forEach { item ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFF00D084)))
                    Text(item, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFFE0E0E0))
                }
            }
        }
        StatusLine(message, messageOk)
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(999.dp)).background(Color.White).clickable { onCreate() }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text("CREATE THREAD", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
        }
        if (threadDir == null) Text("Tip: LOAD a thread first — the folder lands beside it.", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color(0xFF6B6B6B))
    }
}

@Composable
private fun GuideTab() {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Author workflow — the format contract", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
        authoringGuideSteps.forEachIndexed { idx, step ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(8.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                    Text("${idx + 1}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(step.first, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(step.second, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFF9A9A9A), lineHeight = 14.sp)
                }
            }
        }
        Text("Grammar", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFF9A9A9A), fontWeight = FontWeight.Bold)
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.Black).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(10.dp)).padding(10.dp)) {
            Text("# Domain\n\n## Section\n\n### Category\n\n- https://example.com — note\n\n> why this is good", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFFE0E0E0), lineHeight = 15.sp)
        }
    }
}
