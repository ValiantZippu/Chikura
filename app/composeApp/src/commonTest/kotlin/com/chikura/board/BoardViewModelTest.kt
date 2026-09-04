package com.chikura.board

import com.chikura.model.Bunker
import com.chikura.parser.buildBunker
import com.chikura.parser.moveResourceInMarkdown
import com.chikura.parser.parseMarkdown
import com.chikura.ui.board.BoardViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoardViewModelTest {

    @Test
    fun moveResourceSectionChanged() {
        val markdown = "# Music\n\n## Sound\n\n### EQ\n\n- https://youtu.be/RIuqjFP2cHg?si=xxx\n- https://youtube.com/playlist?list=PL123\n\n## DAW\n\n### FL Studio\n\n- https://www.image-line.com/\n"
        val bunker = buildBunker("sample", "sample", mapOf("music" to markdown))
        // Verify initial id
        assertEquals("Sound", bunker.domains.first().resources.first().section)
        assertEquals("music-0001", bunker.domains.first().resources.first().id)

        val vm = BoardViewModel(bunker, mapOf("music" to markdown))
        vm.move("music-0001", "Music Theory")
        val after = vm.getBunker()
        val moved = after.domains.first().resources.find { it.id == "music-0001" } ?: after.domains.flatMap { it.resources }.find { it.url.contains("RIuqjFP2cHg") }
        // find may return null if re-parsed changes id order; so check by url
        val byUrl = after.domains.flatMap { it.resources }.find { it.url.contains("RIuqjFP2cHg") }
        assertTrue(byUrl != null, "resource with that url should exist after move")
        assertEquals("Music Theory", byUrl!!.section)

        val newMd = vm.getMarkdown("music")
        assertTrue(newMd.contains("## Music Theory"), "markdown should contain new section header")
        assertTrue(newMd.contains("https://youtu.be/RIuqjFP2cHg"), "markdown should still contain url")
        // Indent preserved (original was 0)
        val movedLine = newMd.split("\n").find { it.contains("RIuqjFP2cHg") } ?: ""
        assertTrue(movedLine.trimStart().startsWith("-"), "moved line should still be bullet")
        assertEquals(0, movedLine.length - movedLine.trimStart().length, "indent preserved 0")
    }

    @Test
    fun preserveIndent() {
        val markdown = "## Sound\n### EQ\n    - https://youtu.be/RIuqjFP2cHg?si=xxx\n    - https://a.com\n\n## DAW\n\n- https://b.com\n"
        val bunker = buildBunker("sample", "sample", mapOf("music" to markdown))
        val first = bunker.domains.first().resources.find { it.url.contains("RIuqjFP2cHg") }!!
        assertTrue(first.indent >= 4)
        val vm = BoardViewModel(bunker, mapOf("music" to markdown))
        vm.move(first.id, "Music Theory")
        val newMd = vm.getMarkdown("music")
        val movedLine = newMd.split("\n").find { it.contains("RIuqjFP2cHg") } ?: ""
        // indent should still be >=4
        val indent = movedLine.length - movedLine.trimStart().length
        assertTrue(indent >= 4, "indent preserved, got $indent line: '$movedLine'")
        // Section changed
        val byUrl = vm.getBunker().domains.flatMap { it.resources }.find { it.url.contains("RIuqjFP2cHg") }!!
        assertEquals("Music Theory", byUrl.section)
    }

    @Test
    fun moveResourceInMarkdownDirect() {
        val text = "# Music\n\n## Sound\n\n- https://a.com\n\n## DAW\n\n- https://b.com\n"
        val newText = moveResourceInMarkdown(text, "music", "music-0001", "DAW")
        // a.com should now be under DAW section
        assertTrue(newText.contains("## DAW"))
        // Count occurrences: a.com should appear after DAW header
        val dawIdx = newText.indexOf("## DAW")
        val aIdx = newText.indexOf("https://a.com")
        val soundIdx = newText.indexOf("## Sound")
        assertTrue(dawIdx < aIdx, "a.com after DAW")
        assertTrue(aIdx > soundIdx, "still after Sound header but before or after DAW? Actually removed from Sound")
        // Ensure Sound section still exists but without a.com in its immediate area before DAW
        val between = newText.substring(soundIdx, dawIdx)
        assertTrue(!between.contains("https://a.com"), "a.com not in Sound section anymore")
    }

    @Test
    fun moveToNewSectionCreatesHeader() {
        val markdown = "# Music\n\n## Sound\n\n- https://a.com\n"
        val bunker = buildBunker("test", "test", mapOf("music" to markdown))
        val vm = BoardViewModel(bunker, mapOf("music" to markdown))
        vm.move("music-0001", "Music Theory")
        val md = vm.getMarkdown("music")
        assertTrue(md.contains("## Music Theory"))
        val byUrl = vm.getBunker().domains.flatMap { it.resources }.find { it.url.contains("a.com") }!!
        assertEquals("Music Theory", byUrl.section)
    }
}
