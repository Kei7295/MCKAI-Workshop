package com.mckai.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
                                block.code,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                            )
                        }
                    }
                    is MdBlock.Paragraph -> {
                        RichParagraph(block.text, modifier = Modifier.padding(vertical = 2.dp))
                    }
                    is MdBlock.ListItem -> {
                        Text("• ${block.text}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp, top = 1.dp, bottom = 1.dp))
                    }
                    is MdBlock.HorizontalRule -> {
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    }
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
                        withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, background = MaterialTheme.colorScheme.surfaceVariant)) {
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

private sealed class MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock()
    data class CodeBlock(val code: String, val lang: String = "") : MdBlock()
    data class Paragraph(val text: String) : MdBlock()
    data class ListItem(val text: String) : MdBlock()
    object HorizontalRule : MdBlock()
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
            line.isBlank() -> i++
            else -> {
                val para = StringBuilder(line)
                i++
                while (i < lines.size && lines[i].isNotBlank() && !lines[i].startsWith("#") && !lines[i].startsWith("```") && !lines[i].matches(Regex("^[-*_]{3,}$"))) {
                    para.append(" ").append(lines[i].trim())
                    i++
                }
                blocks.add(MdBlock.Paragraph(para.toString()))
            }
        }
    }
    return blocks
}
