package com.mckai.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mckai.app.ui.theme.AppleFonts

/**
 * Markdown 渲染器：标题 / 代码块（带轻量高亮）/ 段落 / 列表 / 分隔线 / 表格。
 * RikkaHub Markdown 渲染的极简移植，无第三方依赖。
 */
@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    SelectionContainer(modifier) {
        Column {
            val blocks = parseMarkdownBlocks(text)
            blocks.forEach { block ->
                when (block) {
                    is MdBlock.Heading -> {
                        val style = when (block.level) {
                            1 -> MaterialTheme.typography.headlineMedium
                            2 -> MaterialTheme.typography.headlineSmall
                            else -> MaterialTheme.typography.titleMedium
                        }
                        Text(block.text, style = style.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(vertical = 4.dp))
                    }
                    is MdBlock.CodeBlock -> {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Text(
                                highlightCode(block.code, block.lang),
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = AppleFonts.Mono, fontSize = 13.sp)
                            )
                        }
                    }
                    is MdBlock.Paragraph -> {
                        RichParagraph(block.text, modifier = Modifier.padding(vertical = 2.dp))
                    }
                    is MdBlock.ListItem -> {
                        Text(
                            if (block.ordered) "${block.index}. ${block.text}" else "• ${block.text}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp, top = 1.dp, bottom = 1.dp)
                        )
                    }
                    is MdBlock.HorizontalRule -> {
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    }
                    is MdBlock.Table -> {
                        MarkdownTable(block)
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownTable(table: MdBlock.Table) {
    val border = MaterialTheme.colorScheme.outlineVariant
    val bg = MaterialTheme.colorScheme.surfaceVariant
    Column(Modifier.padding(vertical = 4.dp)) {
        // header
        Row(Modifier.background(bg)) {
            table.headers.forEach { h ->
                Text(
                    h,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
        HorizontalDivider(color = border, thickness = 1.dp)
        table.rows.forEachIndexed { i, row ->
            if (i > 0) HorizontalDivider(color = border.copy(alpha = 0.5f), thickness = 1.dp)
            Row(Modifier.background(if (i % 2 == 0) bg.copy(alpha = 0.4f) else Color.Transparent)) {
                row.forEach { cell ->
                    Text(
                        cell,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RichParagraph(text: String, modifier: Modifier = Modifier) {
    val annotated = buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) || text.startsWith("__", i) -> {
                    val end1 = text.indexOf("**", i + 2)
                    val end2 = text.indexOf("__", i + 2)
                    val end = when {
                        end1 >= 0 && end2 >= 0 -> minOf(end1, end2)
                        end1 >= 0 -> end1
                        else -> end2
                    }
                    if (end > 0) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(i + 2, end)) }
                        i = end + 2
                    } else { append(text[i]); i++ }
                }
                text.startsWith("*", i) || text.startsWith("_", i) -> {
                    val end1 = text.indexOf("*", i + 1)
                    val end2 = text.indexOf("_", i + 1)
                    val end = when {
                        end1 >= 0 && end2 >= 0 -> minOf(end1, end2)
                        end1 >= 0 -> end1
                        else -> end2
                    }
                    if (end > 0) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text.substring(i + 1, end)) }
                        i = end + 1
                    } else { append(text[i]); i++ }
                }
                text.startsWith("`", i) -> {
                    val end = text.indexOf("`", i + 1)
                    if (end > 0) {
                        withStyle(SpanStyle(
                            fontFamily = AppleFonts.Mono,
                            fontSize = 13.sp,
                            background = MaterialTheme.colorScheme.surfaceVariant,
                            color = MaterialTheme.colorScheme.primary
                        )) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else { append(text[i]); i++ }
                }
                text.startsWith("```", i) -> {
                    val end = text.indexOf("```", i + 3)
                    if (end > 0) { i = end + 3 } else { append(text[i]); i++ }
                }
                else -> { append(text[i]); i++ }
            }
        }
    }
    Text(annotated, style = MaterialTheme.typography.bodyMedium, modifier = modifier)
}

// ================================================================ 轻量代码高亮

private val KEYWORDS = setOf(
    "val", "var", "fun", "if", "else", "when", "for", "while", "do", "return",
    "class", "object", "interface", "data", "enum", "sealed", "abstract", "open",
    "override", "private", "public", "protected", "internal", "import", "package",
    "this", "super", "null", "true", "false", "try", "catch", "finally", "throw",
    "in", "is", "as", "companion", "init", "constructor", "typealias", "suspend",
    "lazy", "inline", "reified", "by", "outer"
)
private val KEYWORDS_JAVA = setOf(
    "public", "static", "class", "void", "int", "String", "boolean", "new",
    "return", "if", "else", "for", "while", "package", "import", "final", "extends", "implements"
)

private fun highlightCode(code: String, lang: String): AnnotatedString {
    val keywords = when (lang.lowercase()) {
        "java" -> KEYWORDS_JAVA
        else -> KEYWORDS
    }
    val keywordColor = Color(0xFF007AFF)
    val stringColor = Color(0xFF34C759)
    val numberColor = Color(0xFFFF9500)
    val commentColor = Color(0xFF8E8E93)

    return buildAnnotatedString {
        var i = 0
        val lines = code.split("\n")
        lines.forEachIndexed { li, line ->
            if (li > 0) append("\n")
            var j = 0
            val trimmed = line.trimStart()
            val indent = line.length - trimmed.length
            if (indent > 0) append(line.substring(0, indent))
            // 注释行
            if (trimmed.startsWith("//") || trimmed.startsWith("#")) {
                withStyle(SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)) { append(trimmed) }
                return@forEachIndexed
            }
            while (j < trimmed.length) {
                val ch = trimmed[j]
                when {
                    trimmed.startsWith("//", j) || trimmed.startsWith("#", j) -> {
                        withStyle(SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)) { append(trimmed.substring(j)) }
                        j = trimmed.length
                    }
                    trimmed.startsWith("\"", j) || trimmed.startsWith("'", j) -> {
                        val quote = trimmed[j]
                        val end = trimmed.indexOf(quote, j + 1)
                        if (end > 0) {
                            withStyle(SpanStyle(color = stringColor)) { append(trimmed.substring(j, end + 1)) }
                            j = end + 1
                        } else { append(ch); j++ }
                    }
                    ch.isDigit() -> {
                        var k = j
                        while (k < trimmed.length && (trimmed[k].isDigit() || trimmed[k] == '.' || trimmed[k] == 'x')) k++
                        withStyle(SpanStyle(color = numberColor)) { append(trimmed.substring(j, k)) }
                        j = k
                    }
                    ch.isLetter() || ch == '_' -> {
                        var k = j
                        while (k < trimmed.length && (trimmed[k].isLetterOrDigit() || trimmed[k] == '_')) k++
                        val word = trimmed.substring(j, k)
                        if (word in keywords) {
                            withStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.SemiBold)) { append(word) }
                        } else {
                            append(word)
                        }
                        j = k
                    }
                    else -> { append(ch); j++ }
                }
            }
        }
    }
}

// ================================================================ Parser

private sealed class MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock()
    data class CodeBlock(val code: String, val lang: String = "") : MdBlock()
    data class Paragraph(val text: String) : MdBlock()
    data class ListItem(val text: String, val ordered: Boolean = false, val index: Int = 0) : MdBlock()
    object HorizontalRule : MdBlock()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MdBlock()
}

private val tableRowRegex = Regex("^\\s*\\|(.+)\\|\\s*$")
private val tableSeparatorRegex = Regex("^\\s*\\|?[\\s:|-]+\\|?\\s*$")

private fun isTableSeparator(line: String): Boolean {
    val stripped = line.replace(Regex("[|:\\s-]"), "")
    return stripped.isEmpty() && line.contains("|") && line.contains("-")
}

private fun splitTableRow(line: String): List<String> {
    val trimmed = line.trim().trimStart('|').trimEnd('|')
    return trimmed.split("|").map { it.trim() }.filterNot { it.isEmpty() && trimmed.isEmpty() }
}

private fun parseMarkdownBlocks(text: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = text.lines()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        when {
            line.startsWith("```") -> {
                val lang = line.removePrefix("```").trim()
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].startsWith("```")) {
                    if (code.isNotEmpty()) code.append("\n")
                    code.append(lines[i])
                    i++
                }
                blocks.add(MdBlock.CodeBlock(code.toString(), lang))
                i++
            }
            line.startsWith("# ") -> { blocks.add(MdBlock.Heading(1, line.removePrefix("# ").trim())); i++ }
            line.startsWith("## ") -> { blocks.add(MdBlock.Heading(2, line.removePrefix("## ").trim())); i++ }
            line.startsWith("### ") -> { blocks.add(MdBlock.Heading(3, line.removePrefix("### ").trim())); i++ }
            line.matches(Regex("^[-*_]{3,}$")) -> { blocks.add(MdBlock.HorizontalRule); i++ }
            line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> {
                blocks.add(MdBlock.ListItem(line.trim().removePrefix("- ").removePrefix("* ").trim()))
                i++
            }
            Regex("^\\d+\\. ").containsMatchIn(line.trimStart()) -> {
                val trimmed = line.trimStart()
                val match = Regex("^(\\d+)\\. (.+)$").find(trimmed)
                blocks.add(MdBlock.ListItem(match?.groupValues?.get(2) ?: trimmed, ordered = true, index = match?.groupValues?.get(1)?.toIntOrNull() ?: 0))
                i++
            }
            // 表格：表头行 + 分隔行
            tableRowRegex.matches(line) && i + 1 < lines.size && isTableSeparator(lines[i + 1]) -> {
                val headers = splitTableRow(line)
                i += 2
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && tableRowRegex.matches(lines[i])) {
                    rows.add(splitTableRow(lines[i]))
                    i++
                }
                blocks.add(MdBlock.Table(headers, rows))
            }
            line.isBlank() -> i++
            else -> {
                val para = StringBuilder(line)
                i++
                val listLike = Regex("^([-*] |\\d+\\. |>)")
                while (i < lines.size && lines[i].isNotBlank() && !lines[i].startsWith("#") && !lines[i].startsWith("```") && !lines[i].matches(Regex("^[-*_]{3,}$")) && !tableRowRegex.matches(lines[i]) && !listLike.containsMatchIn(lines[i].trimStart())) {
                    para.append(" ").append(lines[i].trim())
                    i++
                }
                blocks.add(MdBlock.Paragraph(para.toString()))
            }
        }
    }
    return blocks
}