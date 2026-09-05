package com.chikura.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chikura.ui.theme.ChikuraLogoBunker

@Composable
fun SettingsPanel(
    isOpen: Boolean,
    onClose: () -> Unit,
    githubConnected: Boolean,
    onConnectGitHub: () -> Unit,
    onCreateRepo: () -> Unit,
    onConnectPrivateRepo: (String) -> Unit,
    onSync: () -> Unit,
    incognito: Boolean,
    onIncognitoToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var privateUrl by remember { mutableStateOf("") }
    var showCreate by remember { mutableStateOf(false) }

    AnimatedVisibility(visible = isOpen, enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }), exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })) {
        Box(modifier = modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable { onClose() }, contentAlignment = Alignment.CenterEnd) {
            Column(
                modifier = Modifier.fillMaxHeight().width(420.dp).clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)).background(Color(0xFF0A0A0A)).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)).clickable(enabled = false) {}.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("SETTINGS", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color.White).clickable { onClose() }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("CLOSE X", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                ChikuraLogoBunker(modifier = Modifier.fillMaxWidth())

                // GitHub Sync — Notion alternative relying on github
                Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF111111)).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(16.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("GITHUB SYNC", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (githubConnected) Color(0xFF00D084) else Color(0xFF222222)).padding(horizontal = 7.dp, vertical = 3.dp)) {
                            Text(if (githubConnected) "CONNECTED" else "NOT CONNECTED", fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = if (githubConnected) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("Connect your GitHub account, create a private repo, and sync your bunker. Markdown is the DB, git is the truth.", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color(0xFF9A9A9A), lineHeight = 11.sp)
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(999.dp)).background(if (githubConnected) Color(0xFF1A1A1A) else Color.White).border(1.dp, if (githubConnected) Color(0xFF333333) else Color.White, RoundedCornerShape(999.dp)).clickable { onConnectGitHub() }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(if (githubConnected) "RE-CONNECT GITHUB" else "CONNECT GITHUB ACCOUNT", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = if (githubConnected) Color.White else Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(999.dp)).background(Color(0xFF0A0A0A)).border(1.dp, Color(0xFF222222), RoundedCornerShape(999.dp)).clickable { showCreate = !showCreate; if (!showCreate) onCreateRepo() }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text("CREATE REPO", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(999.dp)).background(Color.White).clickable { if (privateUrl.isNotBlank()) onConnectPrivateRepo(privateUrl) }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text("CONNECT PRIVATE", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("private repo URL", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color(0xFF6B6B6B))
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.Black).border(1.dp, Color(0xFF222222), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Text(if (privateUrl.isBlank()) "https://github.com/you/chikura-private" else privateUrl, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = if (privateUrl.isBlank()) Color(0xFF3A3A3A) else Color.White)
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(999.dp)).background(Color.White).clickable { onSync() }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text("SYNC NOW — git push/pull", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                // Playback
                Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF111111)).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(16.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PLAYBACK", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF0A0A0A)).padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Column { Text("YouTube login", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.White); Text(if (incognito) "Incognito — no sync" else "Signed in — sync history", fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = Color(0xFF9A9A9A)) }
                        Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (incognito) Color(0xFF00D084) else Color.White).clickable { onIncognitoToggle(!incognito) }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(if (incognito) "INC" else "ACC", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Downloads
                Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF111111)).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(16.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DOWNLOADS", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF0A0A0A)).padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Column {
                            Text("Download location", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.White)
                            Text("~/Downloads/Chikura/", fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = Color(0xFF9A9A9A))
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF00D084)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text("OPEN FOLDER", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF0A0A0A)).padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Column {
                            Text("Default format", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.White)
                            Text("MP4 (video) · MP3 (audio)", fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = Color(0xFF9A9A9A))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF0A0A0A)).padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Column {
                            Text("ffmpeg status", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.White)
                            Text("Used for MP3 conversion", fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = Color(0xFF9A9A9A))
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF222222)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("AUTO-DETECT", fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = Color.White)
                        }
                    }
                }

                // Appearance
                Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF111111)).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(16.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("APPEARANCE", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("AMOLED true black #000 · 1px border · rounded 16 · JetBrains Mono · bunker", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color(0xFF6B6B6B))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(999.dp)).background(Color.White).padding(vertical = 6.dp), contentAlignment = Alignment.Center) { Text("AMOLED", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold) }
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(999.dp)).border(1.dp, Color(0xFF222222), RoundedCornerShape(999.dp)).padding(vertical = 6.dp), contentAlignment = Alignment.Center) { Text("BUNKER", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color.White) }
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("Chikura — markdown is DB, git is truth, UI is window.", fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = Color(0xFF3A3A3A), modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}
