package com.chikura.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class MarketplaceEntry(val id: String, val name: String, val author: String, val description: String, val url: String)

@Composable
fun MarketplaceCard(entry: MarketplaceEntry, onClick: (() -> Unit)? = null): Unit {
    Column(
        modifier = Modifier.clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF111111))
            .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(16.dp))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(entry.name, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Text("by " + entry.author, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color(0xFF9A9A9A))
        Text(entry.description, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFF6B6B6B), maxLines = 2)
        Text(entry.url, fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color(0xFF2A2A2A))
    }
}
