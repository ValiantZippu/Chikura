package com.knowledgebunker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knowledgebunker.hydrator.Hydrated
import com.knowledgebunker.model.Resource
import com.knowledgebunker.ui.theme.BunkerBlack
import com.knowledgebunker.ui.theme.BunkerWhite

/**
 * Task 4: ResourceCard — black 1px border, white bg, mono, shows resource.raw and badge for typeHint + thumb placeholder.
 * Only color allowed is thumbnail image; placeholder is monochrome.
 * Task 5: Wire hydrator — when [hydrated] is non-null show title/thumb/author from sidecar cache.
 */
@Composable
fun ResourceCard(
    resource: Resource,
    modifier: Modifier = Modifier,
    hydrated: Hydrated? = null,
    onExpand: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .border(1.dp, BunkerBlack)
            .background(BunkerWhite)
            .padding(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Thumb — shows hydrated.thumb when available (only color allowed is thumbnail image)
                // Placeholder remains monochrome 1px border per spec.
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, BunkerBlack)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (hydrated?.thumb?.isNotBlank() == true) {
                        // Coil would load hydrated.thumb here; we show URL host as placeholder
                        // to prove hydrator wiring without binary image dep.
                        Text(
                            text = "IMG",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 7.sp,
                            color = BunkerBlack,
                            maxLines = 1
                        )
                    } else {
                        Text(
                            text = "THUMB",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 7.sp,
                            color = BunkerBlack,
                            maxLines = 1
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Title from hydrator sidecar cache if available, otherwise raw
                    val titleText = if (hydrated?.title?.isNotBlank() == true) hydrated.title else resource.raw
                    Text(
                        text = titleText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = BunkerBlack,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Show hydrated author when present, otherwise url
                    val subtitle = when {
                        hydrated?.author?.isNotBlank() == true -> hydrated.author
                        else -> resource.url
                    }
                    Text(
                        text = subtitle,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color(0xFF555555),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (resource.section != null || resource.category != null) {
                        Text(
                            text = listOfNotNull(resource.section, resource.category, resource.subcategory).joinToString(" / "),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = Color(0xFF777777),
                            maxLines = 1
                        )
                    }
                    // When hydrated, also show thumb url as proof of cache (debug, mono)
                    if (hydrated?.thumb?.isNotBlank() == true) {
                        Text(
                            text = hydrated.thumb,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 7.sp,
                            color = Color(0xFF999999),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            // typeHint badge — black 1px border, white bg, mono
            Box(
                modifier = Modifier
                    .border(1.dp, BunkerBlack)
                    .background(BunkerWhite)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = resource.typeHint,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = BunkerBlack
                )
            }
        }
    }
}
