package com.chikura.ui

import androidx.compose.ui.graphics.Color
import com.chikura.model.Bunker
import com.chikura.model.Domain
import com.chikura.model.Resource
import com.chikura.parser.parseMarkdown
import com.chikura.ui.theme.BunkerBlack
import com.chikura.ui.theme.BunkerWhite
import com.chikura.ui.theme.BunkerThemeTokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BunkerThemeTest {

    @Test
    fun themeTokensAreBlackWhite() {
        assertEquals(Color.White, BunkerWhite)
        assertEquals(Color.Black, BunkerBlack)
        assertEquals(Color.White, BunkerThemeTokens.background)
        assertEquals(Color.Black, BunkerThemeTokens.foreground)
        assertEquals(Color.Black, BunkerThemeTokens.borderColor)
    }

    @Test
    fun resourceCardDataPreservedForListScreen() {
        val text = "# Music\n## Sound\n### EQ\n- https://youtu.be/RIuqjFP2cHg?si=xxx\n"
        val res = parseMarkdown(text, "music")
        assertEquals(1, res.size)
        // ResourceCard displays raw + typeHint; ensure values correct for UI
        assertEquals("https://youtu.be/RIuqjFP2cHg?si=xxx", res[0].raw)
        assertEquals("video", res[0].typeHint)
        assertEquals("https://youtu.be/RIuqjFP2cHg?si=xxx", res[0].url)
    }

    @Test
    fun listScreenBunkerGrouping() {
        // Simulate what ListScreen receives: Bunker with 2 domains
        val musicMd = "# Music\n## Sound\n### EQ\n- https://a.com\n"
        val techMd = "# Tech\n## Programming\n### Kotlin\n- https://b.com\n"
        val musicRes = parseMarkdown(musicMd, "music")
        val techRes = parseMarkdown(techMd, "tech")
        // Build via parser helper would create sections; here we test raw grouping
        assertEquals("music", musicRes[0].domain)
        assertEquals("tech", techRes[0].domain)
        assertEquals("Sound", musicRes[0].section)
        assertEquals("Programming", techRes[0].section)
        assertTrue(musicRes[0].raw.contains("a.com"))
    }

    @Test
    fun sampleBunkerPreviewBunkerNotEmpty() {
        // Preview/test that loads sample-bunker conceptually: build Bunker via parser
        // In commonTest we can't use File IO, so we simulate file contents directly
        val musicText = "# Music\n## Sound\n### EQ\n- https://youtu.be/RIuqjFP2cHg?si=xxx\n"
        val techText = "# Tech\n## Programming\n### Kotlin\n- https://kotlinlang.org\n"
        val bunker = com.chikura.parser.buildBunker(
            id = "sample",
            name = "sample",
            markdownByDomain = mapOf("music" to musicText, "tech" to techText)
        )
        assertEquals(2, bunker.domains.size)
        assertEquals("music", bunker.domains[0].id)
        assertEquals("tech", bunker.domains[1].id)
        // ListScreen would render 2 domains; ensure each has at least one section/category
        assertTrue(bunker.domains[0].sections.isNotEmpty())
    }
}
