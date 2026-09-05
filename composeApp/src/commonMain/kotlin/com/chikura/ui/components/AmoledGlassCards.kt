package com.chikura.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AmoledGlassCard(content: @Composable () -> Unit, modifier: Modifier = Modifier): Unit {
    Box(
        modifier = modifier.clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0A0A0A).copy(alpha = 0.8f))
            .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        content()
    }
}
