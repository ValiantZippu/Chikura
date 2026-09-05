package com.chikura.notion

import com.chikura.platform.FileSystem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

/**
 * Persists brainstorm Notion blocks to disk per domain.
 * Saves to ~/.Chikura/chikuthreads/<threadId>/brainstorm/<domainId>.json
 * Also supports markdown export.
 */
object BrainstormStore {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private var loaded = mutableMapOf<String, List<NotionBlock>>()

    private fun dirPath(): String {
        val home = FileSystem.homeChikuThreadsPath().trimEnd('/', '\\')
        return "$home/default/.brainstorm"
    }

    private fun filePath(domainId: String): String {
        return "${dirPath()}/${domainId}.json"
    }

    fun load(domainId: String): List<NotionBlock> {
        loaded[domainId]?.let { return it }
        try {
            val path = filePath(domainId)
            if (!FileSystem.exists(path) || !FileSystem.isFile(path)) {
                val blocks = defaultBlocks(domainId)
                loaded[domainId] = blocks
                return blocks
            }
            val text = FileSystem.readText(path)
            if (text.isBlank()) {
                val blocks = defaultBlocks(domainId)
                loaded[domainId] = blocks
                return blocks
            }
            val data = json.decodeFromString<BrainstormData>(text)
            val blocks = data.blocks.mapNotNull { serialToBlock(it) }
            loaded[domainId] = blocks
            return blocks
        } catch (_: Exception) {
            val blocks = defaultBlocks(domainId)
            loaded[domainId] = blocks
            return blocks
        }
    }

    fun save(domainId: String, blocks: List<NotionBlock>) {
        loaded[domainId] = blocks
        try {
            FileSystem.mkdirs(dirPath())
            val data = BrainstormData(blocks = blocks.map { blockToSerial(it) })
            val text = json.encodeToString(data)
            FileSystem.writeText(filePath(domainId), text)
        } catch (_: Exception) {}
    }

    fun exportMarkdown(domainId: String, blocks: List<NotionBlock>): String {
        val sb = StringBuilder()
        sb.appendLine("# $domainId")
        sb.appendLine()
        for (block in blocks) {
            when (block) {
                is NotionBlock.Heading -> {
                    val prefix = "#".repeat(block.level.coerceIn(1, 3))
                    sb.appendLine("$prefix ${block.text}")
                    sb.appendLine()
                }
                is NotionBlock.Paragraph -> {
                    sb.appendLine(block.text)
                    sb.appendLine()
                }
                is NotionBlock.Todo -> {
                    val check = if (block.checked) "[x]" else "[ ]"
                    sb.appendLine("- $check ${block.text}")
                }
                is NotionBlock.Bulleted -> {
                    val indent = "  ".repeat(block.indent)
                    sb.appendLine("$indent- ${block.text}")
                }
                is NotionBlock.Numbered -> {
                    val indent = "  ".repeat(block.indent)
                    sb.appendLine("$indent${block.number}. ${block.text}")
                }
                is NotionBlock.Toggle -> {
                    sb.appendLine("<details>")
                    sb.appendLine("<summary>${block.title}</summary>")
                    sb.appendLine()
                    for (child in block.children) {
                        sb.appendLine(child.toString())
                    }
                    sb.appendLine("</details>")
                    sb.appendLine()
                }
                is NotionBlock.Quote -> sb.appendLine("> ${block.text}")
                is NotionBlock.Callout -> sb.appendLine("> ${block.emoji} ${block.text}")
                is NotionBlock.Divider -> sb.appendLine("---")
                is NotionBlock.Code -> {
                    sb.appendLine("```${block.lang}")
                    sb.appendLine(block.code)
                    sb.appendLine("```")
                }
                is NotionBlock.Image -> sb.appendLine("![${block.caption}](${block.url})")
                is NotionBlock.Bookmark -> sb.appendLine("[${block.caption.ifBlank { block.url }}](${block.url})")
                is NotionBlock.InlineCode -> sb.appendLine("`${block.text}`")
                is NotionBlock.Highlight -> sb.appendLine("==${block.text}==")
                is NotionBlock.Table -> {
                    block.rows.forEachIndexed { rowIdx, row ->
                        sb.appendLine("| ${row.joinToString(" | ")} |")
                        if (rowIdx == 0) sb.appendLine("| ${row.joinToString(" | ") { "---" }} |")
                    }
                }
                is NotionBlock.ColumnList -> {
                    block.columns.forEach { col ->
                        col.forEach { sb.appendLine(it.toString()) }
                        sb.appendLine()
                    }
                }
            }
        }
        return sb.toString()
    }

    private fun defaultBlocks(domainId: String): List<NotionBlock> = listOf(
        NotionBlock.Heading(level = 1, text = domainId.uppercase().replace("-", " ").replace("_", " ")),
        NotionBlock.Paragraph(text = "Brainstorm notes. Use /commands to add blocks."),
        NotionBlock.Divider(),
        NotionBlock.Callout(emoji = "💡", text = "Add your thoughts, ideas, and notes here."),
        NotionBlock.Paragraph(text = "")
    )

    // --- Serialization ---

    @Serializable
    private data class BrainstormData(val blocks: List<SerialBlock> = emptyList())

    @Serializable
    private data class SerialBlock(
        val type: String,
        val text: String = "",
        val level: Int = 0,
        val checked: Boolean = false,
        val number: Int = 1,
        val indent: Int = 0,
        val emoji: String = "",
        val lang: String = "",
        val url: String = "",
        val caption: String = "",
        val title: String = "",
        val code: String = ""
    )

    private fun blockToSerial(block: NotionBlock): SerialBlock = when (block) {
        is NotionBlock.Heading -> SerialBlock(type = "heading", text = block.text, level = block.level)
        is NotionBlock.Paragraph -> SerialBlock(type = "paragraph", text = block.text)
        is NotionBlock.Todo -> SerialBlock(type = "todo", text = block.text, checked = block.checked)
        is NotionBlock.Bulleted -> SerialBlock(type = "bulleted", text = block.text, indent = block.indent)
        is NotionBlock.Numbered -> SerialBlock(type = "numbered", text = block.text, number = block.number, indent = block.indent)
        is NotionBlock.Toggle -> SerialBlock(type = "toggle", title = block.title)
        is NotionBlock.Quote -> SerialBlock(type = "quote", text = block.text)
        is NotionBlock.Callout -> SerialBlock(type = "callout", text = block.text, emoji = block.emoji)
        is NotionBlock.Divider -> SerialBlock(type = "divider")
        is NotionBlock.Code -> SerialBlock(type = "code", code = block.code, lang = block.lang)
        is NotionBlock.Image -> SerialBlock(type = "image", url = block.url, caption = block.caption)
        is NotionBlock.Bookmark -> SerialBlock(type = "bookmark", url = block.url, caption = block.caption)
        is NotionBlock.InlineCode -> SerialBlock(type = "inlinecode", text = block.text)
        is NotionBlock.Highlight -> SerialBlock(type = "highlight", text = block.text)
        is NotionBlock.Table -> SerialBlock(type = "table", text = block.rows.joinToString("\n") { it.joinToString("\t") })
        is NotionBlock.ColumnList -> SerialBlock(type = "columns", text = "")
    }

    private fun serialToBlock(s: SerialBlock): NotionBlock? = when (s.type) {
        "heading" -> NotionBlock.Heading(level = s.level.coerceIn(1, 3), text = s.text)
        "paragraph" -> NotionBlock.Paragraph(text = s.text)
        "todo" -> NotionBlock.Todo(checked = s.checked, text = s.text)
        "bulleted" -> NotionBlock.Bulleted(text = s.text, indent = s.indent)
        "numbered" -> NotionBlock.Numbered(text = s.text, number = s.number, indent = s.indent)
        "toggle" -> NotionBlock.Toggle(title = s.title)
        "quote" -> NotionBlock.Quote(text = s.text)
        "callout" -> NotionBlock.Callout(emoji = s.emoji.ifBlank { "💡" }, text = s.text)
        "divider" -> NotionBlock.Divider()
        "code" -> NotionBlock.Code(code = s.code, lang = s.lang.ifBlank { "text" })
        "image" -> NotionBlock.Image(url = s.url, caption = s.caption)
        "bookmark" -> NotionBlock.Bookmark(url = s.url, caption = s.caption)
        "inlinecode" -> NotionBlock.InlineCode(text = s.text)
        "highlight" -> NotionBlock.Highlight(text = s.text)
        "table" -> {
            val rows = s.text.split("\n").filter { it.isNotBlank() }.map { it.split("\t") }
            if (rows.isNotEmpty()) NotionBlock.Table(rows = rows) else null
        }
        else -> null
    }
}
