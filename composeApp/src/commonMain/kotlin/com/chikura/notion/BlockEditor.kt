package com.chikura.notion

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Theme tokens ────────────────────────────────────────────────────────────

object NotionTokens {
    val Radius = RoundedCornerShape(16.dp)
    val Bg = Color(0xFF000000)
    val Card = Color(0xFF111111)
    val Surface = Color(0xFF0A0A0A)
    val Border = Color(0xFF1A1A1A)
    val BorderLight = Color(0xFF222222)
    val TextMuted = Color(0xFF9A9A9A)
    val TextDim = Color(0xFF3A3A3A)
    val TextFaint = Color(0xFF6B6B6B)
    val Accent = Color(0xFF00D084)
    val White = Color.White
    val Black = Color.Black
}

// ─── Full Notion block types ─────────────────────────────────────────────────

sealed interface NotionBlock {
    val id: String

    data class Heading(override val id: String = genId(), val level: Int, val text: String) : NotionBlock
    data class Paragraph(override val id: String = genId(), val text: String) : NotionBlock
    data class Todo(override val id: String = genId(), val checked: Boolean, val text: String) : NotionBlock
    data class Bulleted(override val id: String = genId(), val text: String, val indent: Int = 0) : NotionBlock
    data class Numbered(override val id: String = genId(), val text: String, val number: Int = 1, val indent: Int = 0) : NotionBlock
    data class Toggle(override val id: String = genId(), val title: String, val children: List<NotionBlock> = emptyList(), val expanded: Boolean = false) : NotionBlock
    data class Quote(override val id: String = genId(), val text: String) : NotionBlock
    data class Callout(override val id: String = genId(), val emoji: String, val text: String) : NotionBlock
    data class Divider(override val id: String = genId()) : NotionBlock
    data class Code(override val id: String = genId(), val code: String, val lang: String = "text") : NotionBlock
    data class Image(override val id: String = genId(), val url: String, val caption: String = "") : NotionBlock
    data class Bookmark(override val id: String = genId(), val url: String, val caption: String = "") : NotionBlock
    data class Table(override val id: String = genId(), val rows: List<List<String>> = listOf(listOf("", ""), listOf("", ""))) : NotionBlock
    data class InlineCode(override val id: String = genId(), val text: String) : NotionBlock
    data class Highlight(override val id: String = genId(), val text: String) : NotionBlock
    data class ColumnList(override val id: String = genId(), val columns: List<List<NotionBlock>> = listOf(emptyList())) : NotionBlock
}

private var _idCounter = 0
private fun genId(): String = "blk-${_idCounter++}"

// ─── Block list operations (pure functions for state management) ──────────────

object BlockOps {
    /** Insert a block after the given index (or at end if index < 0). */
    fun insert(blocks: List<NotionBlock>, afterIndex: Int, block: NotionBlock): List<NotionBlock> {
        val mutable = blocks.toMutableList()
        val idx = if (afterIndex in 0 until blocks.size) afterIndex + 1 else blocks.size
        mutable.add(idx, block)
        return mutable
    }

    /** Delete a block by index. */
    fun delete(blocks: List<NotionBlock>, index: Int): List<NotionBlock> {
        if (index !in blocks.indices) return blocks
        return blocks.toMutableList().apply { removeAt(index) }
    }

    /** Move a block from one index to another. */
    fun move(blocks: List<NotionBlock>, fromIndex: Int, toIndex: Int): List<NotionBlock> {
        if (fromIndex !in blocks.indices || toIndex !in blocks.indices) return blocks
        val mutable = blocks.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        return mutable
    }

    /** Update a block's text content (for text-based blocks). */
    fun updateText(blocks: List<NotionBlock>, index: Int, newText: String): List<NotionBlock> {
        if (index !in blocks.indices) return blocks
        val block = blocks[index]
        val updated: NotionBlock = when (block) {
            is NotionBlock.Heading -> block.copy(text = newText)
            is NotionBlock.Paragraph -> block.copy(text = newText)
            is NotionBlock.Todo -> block.copy(text = newText)
            is NotionBlock.Bulleted -> block.copy(text = newText)
            is NotionBlock.Numbered -> block.copy(text = newText)
            is NotionBlock.Toggle -> block.copy(title = newText)
            is NotionBlock.Quote -> block.copy(text = newText)
            is NotionBlock.Callout -> block.copy(text = newText)
            is NotionBlock.Code -> block.copy(code = newText)
            is NotionBlock.InlineCode -> block.copy(text = newText)
            is NotionBlock.Highlight -> block.copy(text = newText)
            is NotionBlock.Image -> block.copy(caption = newText)
            is NotionBlock.Bookmark -> block.copy(caption = newText)
            else -> block
        }
        return blocks.toMutableList().apply { set(index, updated) }
    }

    /** Toggle a todo's checked state. */
    fun toggleTodo(blocks: List<NotionBlock>, index: Int): List<NotionBlock> {
        if (index !in blocks.indices) return blocks
        val block = blocks[index]
        if (block !is NotionBlock.Todo) return blocks
        return blocks.toMutableList().apply { set(index, block.copy(checked = !block.checked)) }
    }

    /** Convert block type while preserving text content. */
    fun convertType(blocks: List<NotionBlock>, index: Int, newType: String): List<NotionBlock> {
        if (index !in blocks.indices) return blocks
        val old = blocks[index]
        val text = when (old) {
            is NotionBlock.Heading -> old.text
            is NotionBlock.Paragraph -> old.text
            is NotionBlock.Todo -> old.text
            is NotionBlock.Bulleted -> old.text
            is NotionBlock.Numbered -> old.text
            is NotionBlock.Toggle -> old.title
            is NotionBlock.Quote -> old.text
            is NotionBlock.Callout -> old.text
            is NotionBlock.Code -> old.code
            is NotionBlock.InlineCode -> old.text
            is NotionBlock.Highlight -> old.text
            else -> ""
        }
        val converted: NotionBlock = when (newType) {
            "h1" -> NotionBlock.Heading(level = 1, text = text)
            "h2" -> NotionBlock.Heading(level = 2, text = text)
            "h3" -> NotionBlock.Heading(level = 3, text = text)
            "p" -> NotionBlock.Paragraph(text = text)
            "todo" -> NotionBlock.Todo(checked = false, text = text)
            "bulleted" -> NotionBlock.Bulleted(text = text)
            "numbered" -> NotionBlock.Numbered(text = text)
            "toggle" -> NotionBlock.Toggle(title = text)
            "quote" -> NotionBlock.Quote(text = text)
            "callout" -> NotionBlock.Callout(emoji = "💡", text = text)
            "code" -> NotionBlock.Code(code = text)
            "divider" -> NotionBlock.Divider()
            "image" -> NotionBlock.Image(url = text, caption = "")
            "bookmark" -> NotionBlock.Bookmark(url = text, caption = "")
            else -> old
        }
        return blocks.toMutableList().apply { set(index, converted) }
    }

    /** Indent/dedent a bulleted or numbered block. */
    fun changeIndent(blocks: List<NotionBlock>, index: Int, delta: Int): List<NotionBlock> {
        if (index !in blocks.indices) return blocks
        val block = blocks[index]
        val updated: NotionBlock = when (block) {
            is NotionBlock.Bulleted -> block.copy(indent = (block.indent + delta).coerceIn(0, 4))
            is NotionBlock.Numbered -> block.copy(indent = (block.indent + delta).coerceIn(0, 4))
            else -> block
        }
        return blocks.toMutableList().apply { set(index, updated) }
    }

    /** Duplicate a block. */
    fun duplicate(blocks: List<NotionBlock>, index: Int): List<NotionBlock> {
        if (index !in blocks.indices) return blocks
        val original = blocks[index]
        val copy = when (original) {
            is NotionBlock.Heading -> original.copy(id = genId())
            is NotionBlock.Paragraph -> original.copy(id = genId())
            is NotionBlock.Todo -> original.copy(id = genId())
            is NotionBlock.Bulleted -> original.copy(id = genId())
            is NotionBlock.Numbered -> original.copy(id = genId())
            is NotionBlock.Toggle -> original.copy(id = genId())
            is NotionBlock.Quote -> original.copy(id = genId())
            is NotionBlock.Callout -> original.copy(id = genId())
            is NotionBlock.Divider -> original.copy(id = genId())
            is NotionBlock.Code -> original.copy(id = genId())
            is NotionBlock.Image -> original.copy(id = genId())
            is NotionBlock.Bookmark -> original.copy(id = genId())
            is NotionBlock.Table -> original.copy(id = genId())
            is NotionBlock.InlineCode -> original.copy(id = genId())
            is NotionBlock.Highlight -> original.copy(id = genId())
            is NotionBlock.ColumnList -> original.copy(id = genId())
        }
        return insert(blocks, index, copy)
    }

    /** Parse text into numbered list (auto-increment numbers). */
    fun renumber(blocks: List<NotionBlock>): List<NotionBlock> {
        var counter = 0
        return blocks.map { b ->
            if (b is NotionBlock.Numbered) {
                counter++
                b.copy(number = counter)
            } else {
                counter = 0
                b
            }
        }
    }
}

// ─── Block renderer ──────────────────────────────────────────────────────────

@Composable
fun NotionBlockRenderer(
    block: NotionBlock,
    onToggle: (() -> Unit)? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    editable: Boolean = false,
    onTextChange: ((String) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onConvert: ((String) -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onIndent: ((Int) -> Unit)? = null
) {
    var showActions by remember { mutableStateOf(false) }
    var hoverRow by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
        detectDragGestures { _, _ -> showActions = false }
    }) {
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(
            if (hoverRow) Color(0xFF111111) else Color.Transparent
        ).pointerInput(Unit) {
            detectDragGestures { _, _ -> }
        }.clickable { if (editable) showActions = !showActions }) {
            when (block) {
                is NotionBlock.Heading -> {
                    val size = when (block.level) { 1 -> 22.sp; 2 -> 16.sp; 3 -> 13.sp; else -> 12.sp }
                    val weight = when (block.level) { 1 -> FontWeight.Bold; 2 -> FontWeight.SemiBold; else -> FontWeight.Medium }
                    if (editable && onTextChange != null) {
                        EditableText(block.text, size, weight, NotionTokens.White, onTextChange)
                    } else {
                        Text(block.text, fontFamily = FontFamily.Monospace, fontSize = size, color = NotionTokens.White, fontWeight = weight, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
                is NotionBlock.Paragraph -> {
                    if (editable && onTextChange != null) {
                        EditableText(block.text, 11.sp, FontWeight.Normal, Color(0xFFE0E0E0), onTextChange)
                    } else {
                        Text(block.text, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFFE0E0E0), lineHeight = 16.sp, modifier = Modifier.padding(vertical = 3.dp))
                    }
                }
                is NotionBlock.Todo -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onCheckedChange?.invoke(!block.checked) }.padding(vertical = 3.dp)) {
                        Checkbox(checked = block.checked, onCheckedChange = { onCheckedChange?.invoke(it) },
                            colors = CheckboxDefaults.colors(checkedColor = NotionTokens.White, uncheckedColor = NotionTokens.Border, checkmarkColor = NotionTokens.Black))
                        if (editable && onTextChange != null) {
                            EditableText(block.text, 11.sp, FontWeight.Normal,
                                if (block.checked) NotionTokens.TextFaint else NotionTokens.White, onTextChange, strikethrough = block.checked)
                        } else {
                            Text(block.text, fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                                color = if (block.checked) NotionTokens.TextFaint else NotionTokens.White,
                                textDecoration = if (block.checked) TextDecoration.LineThrough else TextDecoration.None)
                        }
                    }
                }
                is NotionBlock.Bulleted -> {
                    val indent = block.indent
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = (indent * 20).dp, top = 2.dp, bottom = 2.dp)) {
                        Text("•", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NotionTokens.White)
                        if (editable && onTextChange != null) {
                            EditableText(block.text, 11.sp, FontWeight.Normal, NotionTokens.White, onTextChange)
                        } else {
                            Text(block.text, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NotionTokens.White)
                        }
                    }
                }
                is NotionBlock.Numbered -> {
                    val indent = block.indent
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = (indent * 20).dp, top = 2.dp, bottom = 2.dp)) {
                        Text("${block.number}.", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NotionTokens.TextMuted, modifier = Modifier.width(20.dp))
                        if (editable && onTextChange != null) {
                            EditableText(block.text, 11.sp, FontWeight.Normal, NotionTokens.White, onTextChange)
                        } else {
                            Text(block.text, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NotionTokens.White)
                        }
                    }
                }
                is NotionBlock.Toggle -> {
                    var expanded by remember { mutableStateOf(block.expanded) }
                    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(
                        if (expanded) NotionTokens.Card else Color.Transparent
                    ).clickable { expanded = !expanded; onToggle?.invoke() }.padding(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(if (expanded) "▼" else "▶", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NotionTokens.White)
                            if (editable && onTextChange != null) {
                                EditableText(block.title, 11.sp, FontWeight.SemiBold, NotionTokens.White, onTextChange)
                            } else {
                                Text(block.title, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NotionTokens.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        AnimatedVisibility(visible = expanded) {
                            Column(modifier = Modifier.padding(start = 16.dp, top = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                block.children.forEach { child ->
                                    NotionBlockRenderer(child, editable = false)
                                }
                                if (block.children.isEmpty()) {
                                    Text("(empty toggle)", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = NotionTokens.TextDim)
                                }
                            }
                        }
                    }
                }
                is NotionBlock.Quote -> {
                    Box(modifier = Modifier.fillMaxWidth().background(NotionTokens.Card, RoundedCornerShape(8.dp)).border(1.dp, NotionTokens.BorderLight, RoundedCornerShape(8.dp)).padding(10.dp)) {
                        if (editable && onTextChange != null) {
                            EditableText(block.text, 10.sp, FontWeight.Normal, NotionTokens.TextMuted, onTextChange, italic = true)
                        } else {
                            Text("\u201C ${block.text}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = NotionTokens.TextMuted, fontStyle = FontStyle.Italic)
                        }
                    }
                }
                is NotionBlock.Callout -> {
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(NotionTokens.Card).border(1.dp, NotionTokens.Border, RoundedCornerShape(10.dp)).padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Text(block.emoji, fontSize = 14.sp)
                        if (editable && onTextChange != null) {
                            EditableText(block.text, 10.sp, FontWeight.Normal, NotionTokens.White, onTextChange)
                        } else {
                            Text(block.text, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = NotionTokens.White)
                        }
                    }
                }
                is NotionBlock.Code -> {
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(NotionTokens.Surface).border(1.dp, NotionTokens.Border, RoundedCornerShape(10.dp)).padding(10.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(block.lang, fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = NotionTokens.TextFaint,
                                modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(NotionTokens.Border).padding(horizontal = 6.dp, vertical = 2.dp))
                            if (editable && onTextChange != null) {
                                EditableText(block.code, 10.sp, FontWeight.Normal, Color(0xFFE0E0E0), onTextChange, monospace = true)
                            } else {
                                Text(block.code, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFFE0E0E0))
                            }
                        }
                    }
                }
                is NotionBlock.Divider -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(NotionTokens.Border))
                }
                is NotionBlock.Image -> {
                    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(NotionTokens.Surface).border(1.dp, NotionTokens.Border, RoundedCornerShape(10.dp)).padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (block.url.isNotBlank()) {
                            // Show image URL as preview card (actual image loading is async)
                            Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)).background(NotionTokens.Card), contentAlignment = Alignment.Center) {
                                Text("[IMAGE] ${block.url.take(50)}", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = NotionTokens.TextMuted)
                            }
                        }
                        if (block.caption.isNotBlank()) Text(block.caption, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NotionTokens.TextMuted, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }
                is NotionBlock.Bookmark -> {
                    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(NotionTokens.Surface).border(1.dp, NotionTokens.Border, RoundedCornerShape(10.dp)).padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🔗", fontSize = 12.sp)
                            Text(block.url.take(60), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NotionTokens.Accent, maxLines = 1)
                        }
                        if (block.caption.isNotBlank()) Text(block.caption, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NotionTokens.TextMuted)
                    }
                }
                is NotionBlock.Table -> {
                    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(NotionTokens.Surface).border(1.dp, NotionTokens.Border, RoundedCornerShape(10.dp)).padding(4.dp)) {
                        block.rows.forEachIndexed { rowIdx, row ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                row.forEachIndexed { colIdx, cell ->
                                    Box(modifier = Modifier.weight(1f).border(0.5.dp, NotionTokens.Border).padding(6.dp)) {
                                        Text(cell.ifBlank { " " }, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = if (rowIdx == 0) NotionTokens.White else Color(0xFFE0E0E0),
                                            fontWeight = if (rowIdx == 0) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    }
                }
                is NotionBlock.InlineCode -> {
                    Text(block.text, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFFE0E0E0),
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF1A1A1A)).padding(horizontal = 6.dp, vertical = 2.dp))
                }
                is NotionBlock.Highlight -> {
                    Text(block.text, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NotionTokens.White,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF2A2A00)).padding(horizontal = 4.dp, vertical = 2.dp))
                }
                is NotionBlock.ColumnList -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        block.columns.forEach { colBlocks ->
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                colBlocks.forEach { child ->
                                    NotionBlockRenderer(child, editable = false)
                                }
                            }
                        }
                    }
                }
            }

            // Block action toolbar (shown on hover/click in edit mode)
            if (showActions && editable) {
                BlockActionToolbar(
                    onDelete = { onDelete?.invoke(); showActions = false },
                    onDuplicate = { onDuplicate?.invoke(); showActions = false },
                    onMoveUp = { onMoveUp?.invoke(); showActions = false },
                    onMoveDown = { onMoveDown?.invoke(); showActions = false },
                    onIndent = { delta -> onIndent?.invoke(delta); showActions = false },
                    onConvert = { type -> onConvert?.invoke(type); showActions = false }
                )
            }
        }
    }
}

// ─── Editable text field ─────────────────────────────────────────────────────

@Composable
private fun EditableText(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
    color: Color,
    onTextChange: (String) -> Unit,
    monospace: Boolean = true,
    italic: Boolean = false,
    strikethrough: Boolean = false
) {
    BasicTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = Modifier.fillMaxWidth(),
        textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize,
            color = color,
            fontWeight = fontWeight,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            textDecoration = if (strikethrough) TextDecoration.LineThrough else TextDecoration.None
        ),
        cursorBrush = SolidColor(NotionTokens.White),
        decorationBox = { inner ->
            if (text.isEmpty()) {
                Text("Type something...", fontFamily = FontFamily.Monospace, fontSize = fontSize, color = NotionTokens.TextDim)
            }
            inner()
        }
    )
}

// ─── Block action toolbar ────────────────────────────────────────────────────

@Composable
private fun BlockActionToolbar(
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onIndent: (Int) -> Unit,
    onConvert: (String) -> Unit
) {
    var showConvertMenu by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(8.dp)).background(NotionTokens.Border).padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        // Delete
        ActionBtn("🗑", "Delete", onDelete)
        // Duplicate
        ActionBtn("📋", "Dup", onDuplicate)
        // Move
        ActionBtn("↑", "Up", onMoveUp)
        ActionBtn("↓", "Down", onMoveDown)
        // Indent
        ActionBtn("→", "In", { onIndent(1) })
        ActionBtn("←", "Out", { onIndent(-1) })
        // Convert
        Box(modifier = Modifier.wrapContentSize()) {
            ActionBtn("⟳", "Type", { showConvertMenu = !showConvertMenu })
            androidx.compose.animation.AnimatedVisibility(visible = showConvertMenu, modifier = Modifier.wrapContentSize()) {
                Column(modifier = Modifier.offset(y = 28.dp).clip(RoundedCornerShape(10.dp)).background(NotionTokens.Card).border(1.dp, NotionTokens.BorderLight, RoundedCornerShape(10.dp)).padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf("h1" to "H1", "h2" to "H2", "h3" to "H3", "p" to "Paragraph", "todo" to "Todo",
                        "bulleted" to "Bulleted", "numbered" to "Numbered", "toggle" to "Toggle",
                        "quote" to "Quote", "callout" to "Callout", "code" to "Code",
                        "divider" to "Divider", "image" to "Image", "bookmark" to "Bookmark"
                    ).forEach { (type, label) ->
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(NotionTokens.Surface).clickable { onConvert(type); showConvertMenu = false }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text(label, fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = NotionTokens.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionBtn(icon: String, label: String, onClick: () -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(NotionTokens.Surface).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center) {
        Text(icon, fontSize = 10.sp)
    }
}

// ─── Legacy block alias for existing parser ──────────────────────────────────

sealed class Block {
    data class Heading(val level: Int, val text: String) : Block()
    data class Paragraph(val text: String) : Block()
    data class Resource(val url: String, val note: String?) : Block()
    data class Callout(val text: String) : Block()
    data class Divider(val id: String) : Block()
}

private val blockShape = RoundedCornerShape(16.dp)
private val blockModifier = Modifier.clip(blockShape).background(NotionTokens.Surface).border(1.dp, NotionTokens.Border, blockShape).padding(10.dp)

@Composable
fun NotionBlock(text: String, number: Int? = null) {
    Box(modifier = blockModifier) {
        val label = if (number != null) "NOTION ${number.toString().padStart(4, '0')} -- $text" else text
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = NotionTokens.White)
    }
}

// ─── Full Notion editor with slash command palette ───────────────────────────

@Composable
fun NotionEditor(
    blocks: List<NotionBlock>,
    onBlocksChange: (List<NotionBlock>) -> Unit,
    modifier: Modifier = Modifier
) {
    var slashOpen by remember { mutableStateOf(false) }
    var slashFilter by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf("") }
    var undoStack by remember { mutableStateOf<List<List<NotionBlock>>>(emptyList()) }

    fun pushUndo(allBlocks: List<NotionBlock>) {
        undoStack = (undoStack + listOf(allBlocks)).takeLast(30)
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.last()
            undoStack = undoStack.dropLast(1)
            onBlocksChange(prev)
        }
    }

    val slashCommands = listOf(
        "heading1" to "H1 — Large heading",
        "heading2" to "H2 — Medium heading",
        "heading3" to "H3 — Small heading",
        "text" to "Paragraph — Plain text",
        "todo" to "☑ Todo — Checkbox item",
        "bullet" to "• Bulleted list",
        "number" to "1. Numbered list",
        "toggle" to "▶ Toggle — Collapsible",
        "quote" to "\" Quote — Blockquote",
        "callout" to "💡 Callout — Highlight box",
        "code" to "⟨/⟩ Code — Code block",
        "divider" to "— Divider — Horizontal rule",
        "image" to "🖼 Image — Embed image URL",
        "bookmark" to "🔗 Bookmark — Save a link",
        "table" to "▦ Table — Data table",
        "inlinecode" to "` Code — Inline code",
        "highlight" to "🖍 Highlight — Yellow mark",
        "columns" to "⫼ Columns — Multi-column layout"
    )

    val filteredCommands = if (slashFilter.isBlank()) slashCommands
        else slashCommands.filter { it.first.contains(slashFilter.lowercase()) || it.second.lowercase().contains(slashFilter.lowercase()) }

    Column(modifier = modifier.clip(NotionTokens.Radius).background(NotionTokens.Black).border(1.dp, NotionTokens.Border, NotionTokens.Radius).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Header toolbar
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("NOTION EDITOR", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NotionTokens.TextMuted, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(NotionTokens.Card).padding(horizontal = 8.dp, vertical = 4.dp))
            Text("${blocks.size} blocks", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = NotionTokens.TextDim)
            Spacer(Modifier.weight(1f))
            // Undo button
            Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(NotionTokens.Card).border(1.dp, NotionTokens.Border, RoundedCornerShape(999.dp))
                .clickable(enabled = undoStack.isNotEmpty()) { undo() }.padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text("↩ UNDO", fontFamily = FontFamily.Monospace, fontSize = 8.sp,
                    color = if (undoStack.isNotEmpty()) NotionTokens.White else NotionTokens.TextDim, fontWeight = FontWeight.Bold)
            }
            // Quick add buttons
            listOf("☑" to "Todo", "•" to "Bullet", "\"" to "Quote", "—" to "---").forEach { (icon, _) ->
                Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(NotionTokens.Card).border(1.dp, NotionTokens.Border, RoundedCornerShape(999.dp))
                    .clickable {
                        pushUndo(blocks)
                        val newBlock = when (icon) {
                            "☑" -> NotionBlock.Todo(checked = false, text = "")
                            "•" -> NotionBlock.Bulleted(text = "")
                            "\"" -> NotionBlock.Quote(text = "")
                            "—" -> NotionBlock.Divider()
                            else -> NotionBlock.Paragraph(text = "")
                        }
                        onBlocksChange(BlockOps.insert(blocks, blocks.size - 1, newBlock))
                    }.padding(horizontal = 6.dp, vertical = 3.dp)) {
                    Text(icon, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = NotionTokens.White)
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(NotionTokens.Border))

        // Block list
        blocks.forEachIndexed { idx, block ->
            var checked by remember(block) { mutableStateOf((block as? NotionBlock.Todo)?.checked ?: false) }
            NotionBlockRenderer(
                block = block,
                editable = true,
                onTextChange = { newText ->
                    pushUndo(blocks)
                    onBlocksChange(BlockOps.updateText(blocks, idx, newText))
                },
                onCheckedChange = { v ->
                    pushUndo(blocks)
                    onBlocksChange(BlockOps.toggleTodo(blocks, idx))
                },
                onDelete = {
                    pushUndo(blocks)
                    onBlocksChange(BlockOps.delete(blocks, idx))
                },
                onDuplicate = {
                    pushUndo(blocks)
                    onBlocksChange(BlockOps.duplicate(blocks, idx))
                },
                onConvert = { type ->
                    pushUndo(blocks)
                    onBlocksChange(BlockOps.convertType(blocks, idx, type))
                },
                onMoveUp = {
                    if (idx > 0) {
                        pushUndo(blocks)
                        onBlocksChange(BlockOps.move(blocks, idx, idx - 1))
                    }
                },
                onMoveDown = {
                    if (idx < blocks.size - 1) {
                        pushUndo(blocks)
                        onBlocksChange(BlockOps.move(blocks, idx, idx + 1))
                    }
                },
                onIndent = { delta ->
                    pushUndo(blocks)
                    onBlocksChange(BlockOps.changeIndent(blocks, idx, delta))
                }
            )
        }

        // Slash command input
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(NotionTokens.Surface).border(1.dp, NotionTokens.Border, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
            BasicTextField(
                value = draft,
                onValueChange = {
                    draft = it
                    if (it.startsWith("/")) {
                        slashOpen = true
                        slashFilter = it.removePrefix("/")
                    } else {
                        slashOpen = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NotionTokens.White),
                cursorBrush = SolidColor(NotionTokens.White),
                decorationBox = { inner ->
                    if (draft.isEmpty()) Text("Type '/' for commands — heading, todo, table, image, bookmark...", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NotionTokens.TextDim)
                    inner()
                }
            )
        }

        // Slash command palette
        AnimatedVisibility(visible = slashOpen && filteredCommands.isNotEmpty()) {
            Column(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(NotionTokens.Card).border(1.dp, NotionTokens.BorderLight, RoundedCornerShape(12.dp)).padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("SLASH COMMANDS", fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = NotionTokens.TextDim, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                filteredCommands.take(12).forEach { (cmd, desc) ->
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(NotionTokens.Surface).clickable {
                        pushUndo(blocks)
                        val content = draft.removePrefix("/").trim()
                        val newBlock: NotionBlock = when (cmd) {
                            "heading1" -> NotionBlock.Heading(level = 1, text = content.ifBlank { "New heading" })
                            "heading2" -> NotionBlock.Heading(level = 2, text = content.ifBlank { "New heading" })
                            "heading3" -> NotionBlock.Heading(level = 3, text = content.ifBlank { "New heading" })
                            "text" -> NotionBlock.Paragraph(text = content)
                            "todo" -> NotionBlock.Todo(checked = false, text = content.ifBlank { "New todo" })
                            "bullet" -> NotionBlock.Bulleted(text = content.ifBlank { "New item" })
                            "number" -> NotionBlock.Numbered(text = content.ifBlank { "New item" }, number = blocks.count { it is NotionBlock.Numbered } + 1)
                            "toggle" -> NotionBlock.Toggle(title = content.ifBlank { "Toggle" })
                            "quote" -> NotionBlock.Quote(text = content.ifBlank { "Quote text" })
                            "callout" -> NotionBlock.Callout(emoji = "💡", text = content.ifBlank { "Callout text" })
                            "code" -> NotionBlock.Code(code = content, lang = "text")
                            "divider" -> NotionBlock.Divider()
                            "image" -> NotionBlock.Image(url = content.ifBlank { "https://..." }, caption = "")
                            "bookmark" -> NotionBlock.Bookmark(url = content.ifBlank { "https://..." }, caption = "")
                            "table" -> NotionBlock.Table(rows = listOf(listOf("Header 1", "Header 2"), listOf("Cell 1", "Cell 2"), listOf("Cell 3", "Cell 4")))
                            "inlinecode" -> NotionBlock.InlineCode(text = content.ifBlank { "code" })
                            "highlight" -> NotionBlock.Highlight(text = content.ifBlank { "highlighted text" })
                            "columns" -> NotionBlock.ColumnList(columns = listOf(listOf(NotionBlock.Paragraph(text = "Column 1")), listOf(NotionBlock.Paragraph(text = "Column 2"))))
                            else -> NotionBlock.Paragraph(text = content)
                        }
                        val updated = if (newBlock is NotionBlock.Numbered) {
                            BlockOps.renumber(BlockOps.insert(blocks, blocks.size - 1, newBlock))
                        } else {
                            BlockOps.insert(blocks, blocks.size - 1, newBlock)
                        }
                        onBlocksChange(updated)
                        draft = ""
                        slashOpen = false
                    }.padding(horizontal = 10.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cmd.uppercase(), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NotionTokens.White, fontWeight = FontWeight.Bold)
                            Text(desc, fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = NotionTokens.TextFaint)
                        }
                    }
                }
            }
        }

        // Block count footer
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(NotionTokens.Card).padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            val counts = mapOf(
                "Todo" to blocks.count { it is NotionBlock.Todo },
                "Lists" to blocks.count { it is NotionBlock.Bulleted || it is NotionBlock.Numbered },
                "Code" to blocks.count { it is NotionBlock.Code },
                "Media" to blocks.count { it is NotionBlock.Image || it is NotionBlock.Bookmark }
            ).filter { it.value > 0 }
            counts.forEach { (label, count) ->
                Text("$label:$count", fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = NotionTokens.TextFaint)
            }
            Text("⌘+Z undo · slash commands", fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = NotionTokens.TextDim)
        }
    }
}
