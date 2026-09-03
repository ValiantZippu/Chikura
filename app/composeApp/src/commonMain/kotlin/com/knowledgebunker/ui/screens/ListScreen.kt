package com.knowledgebunker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knowledgebunker.model.Bunker
import com.knowledgebunker.ui.components.ResourceCard
import com.knowledgebunker.ui.theme.BunkerBlack
import com.knowledgebunker.ui.theme.BunkerTheme
import com.knowledgebunker.ui.theme.BunkerWhite

/**
 * Task 4: ListScreen — Notion-like list per domain, grouped as Domain -> Section -> Category -> Resource.
 * Black & white terminal: 1px borders, mono, white bg.
 * Expandable domains/sections.
 */
@Composable
fun ListScreen(
    bunker: Bunker,
    modifier: Modifier = Modifier
) {
    BunkerTheme {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(BunkerWhite)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BunkerBlack)
                        .background(BunkerWhite)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "BUNKER: ${bunker.id}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = BunkerBlack
                    )
                    Text(
                        text = "${bunker.domains.size} domains  •  ${bunker.domains.sumOf { it.resources.size }} resources",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color(0xFF555555)
                    )
                    if (bunker.domains.isEmpty()) {
                        Text(
                            text = "(blank — add *.md files to bunker)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF888888)
                        )
                    }
                }
            }

            bunker.domains.forEach { domain ->
                item {
                    var expanded by remember(domain.id) { mutableStateOf(true) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BunkerBlack)
                            .background(BunkerWhite)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BunkerWhite)
                                .clickable { expanded = !expanded }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (expanded) "▼" else "▶",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = BunkerBlack
                                )
                                Text(
                                    text = domain.id,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = BunkerBlack
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .border(1.dp, BunkerBlack)
                                    .background(BunkerWhite)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${domain.resources.size}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = BunkerBlack
                                )
                            }
                        }

                        if (expanded) {
                            Column(
                                modifier = Modifier.padding(start = 12.dp, end = 8.dp, bottom = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (domain.sections.isEmpty()) {
                                    if (domain.resources.isEmpty()) {
                                        Text(
                                            text = "(no resources)",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            color = Color(0xFF888888)
                                        )
                                    } else {
                                        domain.resources.forEach { res ->
                                            ResourceCard(resource = res, modifier = Modifier.fillMaxWidth())
                                        }
                                    }
                                } else {
                                    domain.sections.forEach { section ->
                                        var sectionExpanded by remember(domain.id + section.name) { mutableStateOf(true) }
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color(0xFFCCCCCC))
                                                .padding(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { sectionExpanded = !sectionExpanded },
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = if (sectionExpanded) "▾" else "▸",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp,
                                                    color = BunkerBlack
                                                )
                                                Text(
                                                    text = "## ${section.name}",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 12.sp,
                                                    color = BunkerBlack
                                                )
                                            }
                                            if (sectionExpanded) {
                                                section.categories.forEach { cat ->
                                                    Column(
                                                        modifier = Modifier.padding(start = 8.dp),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "### ${cat.name}",
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF333333)
                                                        )
                                                        cat.resources.forEach { res ->
                                                            ResourceCard(resource = res, modifier = Modifier.fillMaxWidth())
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Preview helper for Compose Preview / desktopRun — loads sample if available.
 */
@Composable
fun ListScreenPreview(bunker: Bunker) {
    BunkerTheme {
        ListScreen(bunker = bunker)
    }
}
