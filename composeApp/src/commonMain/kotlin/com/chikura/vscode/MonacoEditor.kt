package com.chikura.vscode

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MonacoLine(number: Int, text: String, modifier: Modifier = Modifier): Unit {
    Row(
        modifier = modifier.fillMaxWidth()
            .background(if (number % 2 == 0) Color(0xFF000000) else Color(0xFF0A0A0A))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(number.toString().padStart(4, '0'), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color(0xFF6B6B6B), modifier = Modifier.width(40.dp))
        Text(text, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White)
    }
}

@Composable
fun MonacoEditor(lines: List<String>, modifier: Modifier = Modifier): Unit {
    LazyColumn(modifier = modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF000000))) {
        itemsIndexed(lines) { idx, line -> MonacoLine(idx + 1, line) }
    }
}
