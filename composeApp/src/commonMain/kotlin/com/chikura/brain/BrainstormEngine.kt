package com.chikura.brain

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

enum class BrainstormPhase { CAPTURE, INBOX, EXPLORE, ORGANIZE, CURATE, PUBLISH }

@Composable
fun BrainstormPhaseCard(phase: BrainstormPhase, title: String, active: Boolean = false): Unit {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(16.dp))
            .background(if (active) Color.White else Color(0xFF0A0A0A))
            .border(1.dp, if (active) Color.White else Color(0xFF1A1A1A), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(phase.name, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = if (active) Color.Black else Color(0xFF9A9A9A), fontWeight = FontWeight.Bold)
            Text(title, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = if (active) Color.Black else Color.White)
        }
    }
}
