package com.chikura.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BgBlack = Color(0xFF000000)
private val CardDark = Color(0xFF0A0A0A)
private val BorderDim = Color(0xFF1A1A1A)
private val TextMuted = Color(0xFF9A9A9A)
private val AccentGreen = Color(0xFF00D084)
private val White = Color.White
private val Black = Color.Black

/**
 * Quick-capture input bar: paste a URL, write a note, pick a domain, save instantly.
 * Shows inline below the search bar when activated.
 */
@Composable
fun QuickCaptureBar(
    domains: List<String>,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onSave: (url: String, note: String, domain: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var url by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedDomain by remember { mutableStateOf(domains.firstOrNull() ?: "") }
    var domainDropdown by remember { mutableStateOf(false) }

    AnimatedVisibility(visible = isVisible, modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(CardDark).border(1.dp, BorderDim, RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("QUICK CAPTURE", fontFamily = FontFamily.Monospace, fontSize = 9.sp,
                    color = AccentGreen, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFF111111))
                    .clickable { onDismiss() }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("ESC", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = TextMuted)
                }
            }

            // URL input
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(Black).border(1.dp, BorderDim, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)) {
                if (url.isEmpty()) {
                    Text("Paste URL here...", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFF3A3A3A))
                }
                BasicTextField(
                    value = url, onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = White),
                    singleLine = true, cursorBrush = SolidColor(White)
                )
            }

            // Note input
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(Black).border(1.dp, BorderDim, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)) {
                if (note.isEmpty()) {
                    Text("Add a note (optional)...", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFF3A3A3A))
                }
                BasicTextField(
                    value = note, onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFFE0E0E0)),
                    singleLine = true, cursorBrush = SolidColor(White)
                )
            }

            // Domain picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Domain:", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextMuted)
                Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Black)
                    .border(1.dp, BorderDim, RoundedCornerShape(999.dp)).clickable { domainDropdown = !domainDropdown }
                    .padding(horizontal = 12.dp, vertical = 5.dp)) {
                    Text(selectedDomain.uppercase(), fontFamily = FontFamily.Monospace, fontSize = 9.sp,
                        color = White, fontWeight = FontWeight.Bold)
                }

                // Domain dropdown
                AnimatedVisibility(visible = domainDropdown) {
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(CardDark)
                            .border(1.dp, BorderDim, RoundedCornerShape(10.dp)).padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        domains.take(8).forEach { domain ->
                            val isSelected = domain == selectedDomain
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(999.dp))
                                    .background(if (isSelected) White else Color(0xFF111111))
                                    .clickable { selectedDomain = domain; domainDropdown = false }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(domain.uppercase().take(10), fontFamily = FontFamily.Monospace, fontSize = 8.sp,
                                    color = if (isSelected) Black else TextMuted)
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // Save button
                val canSave = url.isNotBlank() && selectedDomain.isNotBlank()
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(999.dp))
                        .background(if (canSave) AccentGreen else Color(0xFF222222))
                        .clickable(enabled = canSave) {
                            onSave(url.trim(), note.trim(), selectedDomain)
                            url = ""; note = ""; onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SAVE", fontFamily = FontFamily.Monospace, fontSize = 9.sp,
                        color = if (canSave) Black else TextMuted, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
