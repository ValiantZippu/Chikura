package com.knowledgebunker.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BunkerParserTest {

    @Test
    fun parseMusic() {
        val text = "# Music\n## Sound\n### EQ\n- https://youtu.be/RIuqjFP2cHg?si=xxx\n"
        val res = parseMarkdown(text, "music")
        assertEquals(1, res.size)
        assertEquals("EQ", res[0].category)
        assertEquals("https://youtu.be/RIuqjFP2cHg?si=xxx", res[0].url)
        assertEquals("Sound", res[0].section)
        assertEquals("music", res[0].domain)
        assertEquals("video", res[0].typeHint)
    }

    @Test
    fun handlesMalformedUrl() {
        val text = "## Sound\n### EQ\n- htt ps://youtu.be/RIuqjFP2cHg?si=xxx\n"
        val res = parseMarkdown(text, "music")
        assertEquals(1, res.size)
        assertEquals("https://youtu.be/RIuqjFP2cHg?si=xxx", res[0].url)
    }

    @Test
    fun handlesBareRef() {
        val text = "## Sites\n### Tools\n- example.com/path\n"
        val res = parseMarkdown(text, "tech")
        assertEquals(1, res.size)
        assertEquals("example.com/path", res[0].url)
        assertEquals("bare", res[0].typeHint)
    }

    @Test
    fun handlesBareRefWww() {
        val text = "## Sites\n### Tools\n- www.example.org\n"
        val res = parseMarkdown(text, "tech")
        assertEquals(1, res.size)
        assertTrue(res[0].url.contains("example.org"))
    }

    @Test
    fun handlesExeBare() {
        val text = "## Tools\n### Utils\n- tool.exe\n"
        val res = parseMarkdown(text, "tech")
        assertEquals(1, res.size)
        assertEquals("bare", res[0].typeHint)
    }

    @Test
    fun infersTypeHintPlaylist() {
        val text = "## Music\n### Mix\n- https://youtube.com/playlist?list=PL123&si=abc\n"
        val res = parseMarkdown(text, "music")
        assertEquals("playlist", res[0].typeHint)
    }

    @Test
    fun infersTypeHintChannel() {
        val text = "## Music\n### Artists\n- https://www.youtube.com/channel/UCHWo8VJwi7LUM9rKDCaRlfw\n"
        val res = parseMarkdown(text, "music")
        assertEquals("channel", res[0].typeHint)
    }

    @Test
    fun infersTypeHintChannelAt() {
        val text = "## Music\n### Artists\n- https://youtube.com/@memphy?si=CNq9Dhg61jilwMRP\n"
        val res = parseMarkdown(text, "music")
        assertEquals("channel", res[0].typeHint)
    }

    @Test
    fun infersTypeHintShorts() {
        val text = "## Music\n### Clips\n- https://youtube.com/shorts/VnkghtobusU?si=iFk3zX_rKWlKCpuf\n"
        val res = parseMarkdown(text, "music")
        assertEquals("shorts", res[0].typeHint)
    }

    @Test
    fun preservesIndent() {
        val text = "## Sound\n### EQ\n    - https://youtu.be/RIuqjFP2cHg?si=xxx\n"
        val res = parseMarkdown(text, "music")
        assertEquals(1, res.size)
        assertTrue(res[0].indent >= 4)
    }

    @Test
    fun handlesIndentPreservingHierarchy() {
        // Outer bullet at 0, inner at 4 — both should be separate resources with correct indent
        val text = "# Music\n## Sound\n### EQ\n- https://a.com\n    - https://b.com\n"
        val res = parseMarkdown(text, "music")
        assertEquals(2, res.size)
        assertEquals(0, res[0].indent)
        assertTrue(res[1].indent >= 4)
    }

    @Test
    fun handlesMultipleSectionsAndCategories() {
        val text = "# Music\n## Sound\n### EQ\n- https://a.com\n## DAW\n### FL Studio\n- https://b.com\n"
        val res = parseMarkdown(text, "music")
        assertEquals(2, res.size)
        assertEquals("Sound", res[0].section)
        assertEquals("EQ", res[0].category)
        assertEquals("DAW", res[1].section)
        assertEquals("FL Studio", res[1].category)
    }
}
