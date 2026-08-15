package com.mckai.app.domain.tools

import android.content.Context
import kotlinx.serialization.json.*
import java.io.File

fun registerFileTools(r: ToolRegistry) {
    r.register(ToolMetadata(
        name = "read_file",
        description = "读取文件内容",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("path", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("文件路径")) })
            })
            put("required", buildJsonArray { add(JsonPrimitive("path")) })
        },
        category = "file"
    ), handler = { args ->
        val path = args["path"]?.jsonPrimitive?.content ?: return@register "请提供 path 参数"
        try {
            val file = File(path)
            if (!file.exists()) "文件不存在：$path"
            else if (file.isDirectory) "这是一个目录，不是文件：$path"
            else {
                val content = file.readText()
                if (content.length > 50000) content.take(50000) + "\n\n... (文件过大，已截断)"
                else content
            }
        } catch (e: Exception) {
            "读取文件失败：${e.message}"
        }
    })

    r.register(ToolMetadata(
        name = "write_file",
        description = "写入文件内容（会覆盖已有内容）",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("path", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("文件路径")) })
                put("content", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("文件内容")) })
            })
            put("required", buildJsonArray { add(JsonPrimitive("path")); add(JsonPrimitive("content")) })
        },
        permission = ToolPermission.ADMIN,
        category = "file"
    ), handler = { args ->
        val path = args["path"]?.jsonPrimitive?.content ?: return@register "请提供 path 参数"
        val content = args["content"]?.jsonPrimitive?.content ?: return@register "请提供 content 参数"
        try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeText(content)
            "文件已写入：$path（${content.length} 字符）"
        } catch (e: Exception) {
            "写入文件失败：${e.message}"
        }
    })

    r.register(ToolMetadata(
        name = "list_directory",
        description = "列出目录中的文件和子目录",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("path", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("目录路径")) })
                put("recursive", buildJsonObject { put("type", JsonPrimitive("boolean")); put("description", JsonPrimitive("是否递归列出子目录")) })
            })
            put("required", buildJsonArray { add(JsonPrimitive("path")) })
        },
        category = "file"
    ), handler = { args ->
        val path = args["path"]?.jsonPrimitive?.content ?: return@register "请提供 path 参数"
        val recursive = args["recursive"]?.jsonPrimitive?.boolean ?: false
        try {
            val dir = File(path)
            if (!dir.exists()) "目录不存在：$path"
            else if (!dir.isDirectory) "这不是一个目录：$path"
            else {
                val files = if (recursive) dir.walkTopDown().toList() else dir.listFiles()?.toList() ?: emptyList()
                files.take(200).joinToString("\n") { f ->
                    val rel = f.relativeTo(dir).path
                    val type = if (f.isDirectory) "[DIR]" else "[${f.extension.ifBlank { "file" }}]"
                    val size = if (f.isFile) " (${formatSize(f.length())})" else ""
                    "$type $rel$size"
                } + if (files.size > 200) "\n\n... 共 ${files.size} 项（已截断）" else "\n共 ${files.size} 项"
            }
        } catch (e: Exception) {
            "列出目录失败：${e.message}"
        }
    })

    r.register(ToolMetadata(
        name = "search_files",
        description = "在目录中搜索包含指定文本的文件",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("path", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("搜索目录")) })
                put("query", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("搜索关键词")) })
                put("extension", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("文件扩展名过滤，如 .kt")) })
            })
            put("required", buildJsonArray { add(JsonPrimitive("path")); add(JsonPrimitive("query")) })
        },
        category = "file"
    ), handler = { args ->
        val path = args["path"]?.jsonPrimitive?.content ?: return@register "请提供 path 参数"
        val query = args["query"]?.jsonPrimitive?.content ?: return@register "请提供 query 参数"
        val ext = args["extension"]?.jsonPrimitive?.content
        try {
            val dir = File(path)
            if (!dir.exists()) return@register "目录不存在：$path"
            if (!dir.isDirectory) return@register "不是目录：$path"
            val results = mutableListOf<String>()
            var scanned = 0
            val iterator = dir.walkTopDown().iterator()
            while (iterator.hasNext() && results.size < 50 && scanned < 5000) {
                val file = iterator.next()
                if (!file.isFile) continue
                scanned++
                if (ext != null && file.extension != ext.removePrefix(".")) continue
                // 跳过超大/二进制文件，防止 OOM
                if (file.length() > 5 * 1024 * 1024) continue
                try {
                    val content = file.readText()
                    if (content.contains(query, ignoreCase = true)) {
                        val line = content.lines().indexOfFirst { it.contains(query, ignoreCase = true) } + 1
                        results.add("${file.relativeTo(dir).path}:L$line")
                    }
                } catch (_: Exception) { }
            }
            if (results.isEmpty()) "未找到包含 '$query' 的文件"
            else "找到 ${results.size} 个文件（扫描 ${scanned} 个）:\n${results.joinToString("\n")}"
        } catch (e: Exception) {
            "搜索失败：${e.message}"
        }
    })

    r.register(ToolMetadata(
        name = "delete_file",
        description = "删除文件",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("path", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("文件路径")) })
            })
            put("required", buildJsonArray { add(JsonPrimitive("path")) })
        },
        permission = ToolPermission.ADMIN,
        category = "file"
    ), handler = { args ->
        val path = args["path"]?.jsonPrimitive?.content ?: return@register "请提供 path 参数"
        try {
            val file = File(path)
            if (!file.exists()) "文件不存在：$path"
            else {
                file.delete()
                "已删除：$path"
            }
        } catch (e: Exception) {
            "删除文件失败：${e.message}"
        }
    })

    r.register(ToolMetadata(
        name = "create_directory",
        description = "创建目录（包括父目录）",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("path", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("目录路径")) })
            })
            put("required", buildJsonArray { add(JsonPrimitive("path")) })
        },
        permission = ToolPermission.ADMIN,
        category = "file"
    ), handler = { args ->
        val path = args["path"]?.jsonPrimitive?.content ?: return@register "请提供 path 参数"
        try {
            val dir = File(path)
            dir.mkdirs()
            "目录已创建：$path"
        } catch (e: Exception) {
            "创建目录失败：${e.message}"
        }
    })
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${bytes / (1024 * 1024)} MB"
}
