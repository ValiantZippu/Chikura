package com.chikura.ui.components

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chikura.hydrator.Hydrated
import com.chikura.model.Resource
import com.chikura.ui.theme.*

@Composable
fun ResourceCard(
    resource: Resource,
    modifier: Modifier = Modifier,
    hydrated: Hydrated? = null,
    onExpand: (() -> Unit)? = null
) {
    var hovered by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (hovered) 1.015f else 1f, tween(240, easing = ChikuraEase), label = "cardScale")
    val borderColor by animateColorAsState(if (hovered) ChikuraWhite else ChikuraBorder, tween(200), label = "border")
    
    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(ChikuraCard)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { hovered = !hovered; onExpand?.invoke() }
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                // Thumb — AMOLED rounded, only color is thumbnail
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(ChikuraSurface2).border(1.dp, ChikuraBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (hydrated?.thumb?.isNotBlank() == true) {
                        Text("IMG", fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = ChikuraWhite, fontWeight = FontWeight.Bold)
                    } else {
                        Text("THUMB", fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = ChikuraGray, fontWeight = FontWeight.Bold)
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    val titleText = if (hydrated?.title?.isNotBlank() == true) hydrated.title else resource.raw
                    Text(titleText, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = ChikuraWhite, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp)
                    val subtitle = when { hydrated?.author?.isNotBlank() == true -> hydrated.author else -> resource.url }
                    Text(subtitle, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (resource.section != null || resource.category != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (resource.section != null) Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(ChikuraSurface2).border(1.dp, ChikuraBorder, RoundedCornerShape(999.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text(resource.section, fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = ChikuraGray)
                            }
                            if (resource.category != null) Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(ChikuraWhite).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text(resource.category, fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = ChikuraBlack, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                // Type icon — rounded pill
                Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(ChikuraWhite).padding(horizontal = 7.dp, vertical = 4.dp)) {
                    Text(resource.typeHint.uppercase().take(4), fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = ChikuraBlack, fontWeight = FontWeight.Bold)
                }
            }
            // URL bar — AMOLED
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ChikuraSurface1).border(1.dp, ChikuraDim, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 5.dp)) {
                Text(resource.url, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).border(1.dp, ChikuraWhite, RoundedCornerShape(999.dp)).background(Color.Transparent).padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text(resource.typeHint, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = ChikuraWhite)
                }
                if (hydrated?.thumb?.isNotBlank() == true) Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(ChikuraCard).border(1.dp, ChikuraBorder, RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text("cached", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = ChikuraGray)
                }
                Spacer(Modifier.weight(1f))
                Text("↗", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = ChikuraGray)
            }
        }
    }
}
