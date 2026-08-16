package com.mckai.app.data.document

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

/**
 * 文档解析器（移植自 rikkahub document 模块）：
 * DOCX/PPTX/EPUB 为纯 zip+XML 零依赖解析；PDF 走 pdfbox-android（Apache 2.0）；
 * TXT/MD 直读。输出 Markdown 风格文本，供聊天注入。
 */
object DocumentParser {

    /** 按扩展名分发解析。file 必须已落到本地缓存。 */
    fun parse(context: Context, file: File): String {
        val name = file.name.lowercase()
        return when {
            name.endsWith(".docx") -> parseDocx(file)
            name.endsWith(".pptx") -> parsePptx(file)
            name.endsWith(".epub") -> parseEpub(file)
            name.endsWith(".pdf") -> parsePdf(context, file)
            name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".json") || name.endsWith(".log") ->
                file.readText(Charsets.UTF_8)
            else -> "不支持的文件格式：${file.name}"
        }
    }

    private fun parsePdf(context: Context, file: File): String {
        return try {
            PDFBoxResourceLoader.init(context)
            PDDocument.load(file).use { doc ->
                PDFTextStripper().getText(doc)
            }
        } catch (e: Exception) {
            "PDF 解析失败：${e.message}"
        }
    }

    /* ---------- DOCX（移植自 rikkahub DocxParser） ---------- */

    private fun parseDocx(file: File): String {
        return try {
            file.inputStream().use { fis ->
                ZipInputStream(fis).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (entry.name == "word/document.xml") {
                            return parseDocumentXml(zip)
                        }
                        entry = zip.nextEntry
                    }
                    "未在 DOCX 中找到文档内容"
                }
            }
        } catch (e: Exception) {
            "DOCX 解析失败：${e.message}"
        }
    }

    private fun parseDocumentXml(input: InputStream): String {
        return try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(input, "UTF-8")

            val result = StringBuilder()
            var inBody = false
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                when (parser.eventType) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "body" -> inBody = true
                        "p" -> if (inBody) processParagraph(parser, result)
                        "tbl" -> if (inBody) processTable(parser, result)
                    }
                    XmlPullParser.END_TAG -> if (parser.name == "body") inBody = false
                }
                parser.next()
            }
            result.toString().trim()
        } catch (e: Exception) {
            "DOCX XML 解析失败：${e.message}"
        }
    }

    private fun processParagraph(parser: XmlPullParser, result: StringBuilder) {
        val startDepth = parser.depth
        val content = StringBuilder()
        var headingLevel = 0
        var listLevel = -1
        var numbered = false

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "r" -> extractRunText(parser, content)
                    "pStyle" -> {
                        val v = parser.getAttributeValue(null, "val") ?: ""
                        if (v.startsWith("Heading") || v.startsWith("heading")) {
                            headingLevel = v.lastOrNull()?.digitToIntOrNull() ?: 1
                        }
                    }
                    "ilvl" -> listLevel = parser.getAttributeValue(null, "val")?.toIntOrNull() ?: 0
                    "numId" -> numbered = parser.getAttributeValue(null, "val") != null
                }
                XmlPullParser.END_TAG ->
                    if (parser.name == "p" && parser.depth == startDepth) break
            }
        }

        val text = content.toString().trim()
        if (text.isNotBlank()) {
            when {
                listLevel >= 0 -> result.append("  ".repeat(listLevel)).append(if (numbered) "1. " else "- ").append(text).append('\n')
                headingLevel > 0 -> result.append("#".repeat(headingLevel)).append(' ').append(text).append("\n\n")
                else -> result.append(text).append("\n\n")
            }
        }
    }

    private fun extractRunText(parser: XmlPullParser, result: StringBuilder) {
        val startDepth = parser.depth
        var bold = false
        var italic = false
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "b" -> bold = true
                    "i" -> italic = true
                    "t" -> {
                        parser.next()
                        if (parser.eventType == XmlPullParser.TEXT) {
                            val text = parser.text ?: ""
                            result.append(
                                when {
                                    bold && italic -> "***$text***"
                                    bold -> "**$text**"
                                    italic -> "*$text*"
                                    else -> text
                                }
                            )
                        }
                    }
                }
                XmlPullParser.END_TAG ->
                    if (parser.name == "r" && parser.depth == startDepth) break
            }
        }
    }

    private fun processTable(parser: XmlPullParser, result: StringBuilder) {
        val startDepth = parser.depth
        val rows = mutableListOf<List<String>>()
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG ->
                    if (parser.name == "tr") {
                        val cells = mutableListOf<String>()
                        extractRow(parser, cells)
                        if (cells.isNotEmpty()) rows.add(cells)
                    }
                XmlPullParser.END_TAG ->
                    if (parser.name == "tbl" && parser.depth == startDepth) break
            }
        }
        val maxCols = rows.maxOfOrNull { it.size } ?: 0
        rows.forEachIndexed { index, row ->
            result.append("| ")
            (0 until maxCols).forEach { c ->
                result.append(if (c < row.size) row[c] else "").append(" | ")
            }
            result.append('\n')
            if (index == 0) {
                result.append("| ")
                repeat(maxCols) { result.append("--- | ") }
                result.append('\n')
            }
        }
        result.append('\n')
    }

    private fun extractRow(parser: XmlPullParser, cells: MutableList<String>) {
        val startDepth = parser.depth
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG ->
                    if (parser.name == "tc") {
                        val cell = StringBuilder()
                        extractCell(parser, cell)
                        cells.add(cell.toString().trim())
                    }
                XmlPullParser.END_TAG ->
                    if (parser.name == "tr" && parser.depth == startDepth) break
            }
        }
    }

    private fun extractCell(parser: XmlPullParser, out: StringBuilder) {
        val startDepth = parser.depth
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG ->
                    if (parser.name == "r") extractRunText(parser, out)
                XmlPullParser.END_TAG ->
                    if (parser.name == "tc" && parser.depth == startDepth) break
            }
        }
    }

    /* ---------- PPTX（移植自 rikkahub PptxParser，精简） ---------- */

    private fun parsePptx(file: File): String {
        return try {
            ZipFile(file).use { zip ->
                val slides = zip.entries().toList()
                    .filter { it.name.matches(Regex("ppt/slides/slide\\d+\\.xml")) }
                    .sortedBy { it.name.substringAfter("slide").substringBefore(".xml").toIntOrNull() ?: 0 }
                if (slides.isEmpty()) return "PPTX 中未找到幻灯片"
                val result = StringBuilder()
                slides.forEachIndexed { index, entry ->
                    result.append("\n## 幻灯片 ").append(index + 1).append('\n')
                    zip.getInputStream(entry).use { result.append(parseSlideXml(it)) }
                    zip.getEntry("ppt/notesSlides/notesSlide${index + 1}.xml")?.let { notes ->
                        zip.getInputStream(notes).use { n ->
                            val text = parseNotesXml(n)
                            if (text.isNotBlank()) result.append("\n> 备注：").append(text).append('\n')
                        }
                    }
                }
                result.toString().trim()
            }
        } catch (e: Exception) {
            "PPTX 解析失败：${e.message}"
        }
    }

    private fun parseSlideXml(input: InputStream): String {
        val result = StringBuilder()
        var inText = false
        parseXmlText(input) { parser, name, event ->
            when {
                event == XmlPullParser.TEXT && inText -> result.append(parser.text ?: "")
                event == XmlPullParser.START_TAG && name == "a:t" -> inText = true
                event == XmlPullParser.START_TAG && name == "a:p" -> result.append('\n')
                event == XmlPullParser.END_TAG && name == "a:t" -> inText = false
            }
        }
        return result.toString().trim()
    }

    private fun parseNotesXml(input: InputStream): String {
        val result = StringBuilder()
        var inText = false
        parseXmlText(input) { parser, name, event ->
            when {
                event == XmlPullParser.TEXT && inText -> result.append(parser.text ?: "")
                event == XmlPullParser.START_TAG && name == "a:t" -> inText = true
                event == XmlPullParser.END_TAG && name == "a:t" -> inText = false
            }
        }
        return result.toString().trim()
    }

    /* ---------- EPUB（移植自 rikkahub EpubParser，精简） ---------- */

    private fun parseEpub(file: File): String {
        return try {
            ZipFile(file).use { zip ->
                val container = zip.getEntry("META-INF/container.xml") ?: return "EPUB 缺少 container.xml"
                val opfPath = zip.getInputStream(container).use { readContainerOpf(it) } ?: return "EPUB 缺少 OPF"
                val opfDir = opfPath.substringBeforeLast('/', "")
                val opf = zip.getEntry(opfPath) ?: return "EPUB 缺少 OPF 文件"
                val (manifest, spine) = zip.getInputStream(opf).use { parseOpf(it) }
                val result = StringBuilder()
                for (itemId in spine) {
                    val item = manifest[itemId] ?: continue
                    if (!item.second.contains("html")) continue
                    val path = if (opfDir.isEmpty()) item.first else "$opfDir/${item.first}"
                    val entry = zip.getEntry(path) ?: continue
                    zip.getInputStream(entry).use { result.append(parseXhtml(it)) }.also { result.append("\n\n") }
                }
                result.toString().trim().ifEmpty { "EPUB 无可读内容" }
            }
        } catch (e: Exception) {
            "EPUB 解析失败：${e.message}"
        }
    }

    private fun readContainerOpf(input: InputStream): String? {
        var opf: String? = null
        parseXmlText(input) { parser, name, _ ->
            if (name == "rootfile") {
                opf = parser.getAttributeValue(null, "full-path") ?: opf
            }
        }
        return opf
    }

    private fun parseOpf(input: InputStream): Pair<Map<String, Pair<String, String>>, List<String>> {
        val manifest = mutableMapOf<String, Pair<String, String>>() // id -> (href, mediaType)
        val spine = mutableListOf<String>()
        parseXmlText(input) { parser, name, _ ->
            when (name) {
                "item" -> {
                    val id = parser.getAttributeValue(null, "id") ?: return@parseXmlText
                    manifest[id] = (parser.getAttributeValue(null, "href") ?: "") to
                        (parser.getAttributeValue(null, "media-type") ?: "")
                }
                "itemref" -> {
                    val ref = parser.getAttributeValue(null, "idref")
                    if (ref != null) spine.add(ref)
                }
            }
        }
        return manifest to spine
    }

    private fun parseXhtml(input: InputStream): String {
        val result = StringBuilder()
        parseXmlText(input) { parser, name, event ->
            when {
                event == XmlPullParser.TEXT -> {
                    val t = parser.text ?: ""
                    if (t.isNotBlank() && t.any { it.isLetterOrDigit() }) result.append(t.trim())
                }
                name == "p" -> result.append('\n')
                name == "h1" || name == "h2" || name == "h3" || name == "h4" || name == "h5" || name == "h6" ->
                    result.append('\n').append("#".repeat(name[1].digitToIntOrNull() ?: 1)).append(' ')
                name == "br" -> result.append('\n')
            }
        }
        return result.toString().trim()
    }

    /* ---------- 公共 XML 遍历 ---------- */

    private inline fun parseXmlText(
        input: InputStream,
        handler: (XmlPullParser, String, Int) -> Unit
    ) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(input, "UTF-8")
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG || parser.eventType == XmlPullParser.TEXT) {
                    handler(parser, parser.name ?: "", parser.eventType)
                }
                parser.next()
            }
        } catch (_: Exception) {
        }
    }
}