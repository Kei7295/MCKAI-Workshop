package com.mckai.app.domain.workshop.template

import kotlinx.serialization.json.*
import kotlinx.serialization.json.buildJsonObject

/**
 * 合成配方 JSON 生成器。移植自 ModCrafting recipe-utils.ts：
 * shaped / shapeless / smelting / blasting / stonecutting 五种，
 * 带模式与键值校验（行列等长、键存在性、产物非空）。
 */
internal fun jstr(s: String): String = buildString {
    append('"')
    for (c in s) {
        when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
        }
    }
    append('"')
}

object RecipeGenerator {

    private fun prettyJson(json: JsonElement, indent: Int = 0): String {
        val pad = "  ".repeat(indent)
        return when (json) {
            is JsonObject -> buildString {
                appendLine("{")
                json.toList().forEachIndexed { i, (k, v) ->
                    append(pad).append(jstr(k)).append(": ").append(prettyJson(v, indent + 1))
                    if (i < json.size - 1) append(',')
                    appendLine()
                }
                append(pad.substring(2)).append("}")
            }
            is JsonArray -> buildString {
                appendLine("[")
                json.forEachIndexed { i, el ->
                    append(pad).append(prettyJson(el, indent + 1))
                    if (i < json.size - 1) append(',')
                    appendLine()
                }
                append(pad.substring(2)).append("]")
            }
            is JsonPrimitive -> if (json.isString) jstr(json.content) else json.content
            JsonNull -> "null"
        }
    }

    private fun validatePattern(pattern: List<String>, keys: Map<String, String>, type: String): String? {
        if (pattern.isEmpty()) return "pattern 不能为空"
        val widths = pattern.map { it.length }.distinct()
        if (widths.size > 1) return "pattern 各行长度不一致：${pattern.joinToString(" / ")}"
        val used = pattern.flatMap { it.toList() }.map { it.toString() }.filter { it != " " }.distinct()
        val unknown = used.filter { it !in keys }
        if (unknown.isNotEmpty()) return "pattern 使用了未定义的键：${unknown.joinToString(", ")}"
        val unused = keys.keys.filter { it !in used }
        if (unused.isNotEmpty()) return "keys 中定义了未在 pattern 使用的键：${unused.joinToString(", ")}"
        return null
    }

    fun generate(
        type: String,
        pattern: List<String> = emptyList(),
        keys: Map<String, String> = emptyMap(),
        resultItem: String,
        resultCount: Int = 1,
        experience: Float = 0.7f,
        cookingTime: Int = 200
    ): String {
        val normalized = type.lowercase()
        val result = resultItem.ifBlank { return """{"error": "resultItem 不能为空"}""" }
        val root = when (normalized) {
            "shaped", "crafting_shaped" -> {
                val err = validatePattern(pattern, keys, normalized)
                if (err != null) return """{"error": ${jstr(err)}}"""
                buildJsonObject {
                    put("type", JsonPrimitive("minecraft:crafting_shaped"))
                    put("pattern", JsonArray(pattern.map { JsonPrimitive(it) }))
                    put("key", buildJsonObject {
                        keys.forEach { (k, item) -> put(k, buildJsonObject { put("item", JsonPrimitive(item)) }) }
                    })
                    put("result", buildJsonObject {
                        put("item", JsonPrimitive(result))
                        if (resultCount > 1) put("count", JsonPrimitive(resultCount))
                    })
                }
            }
            "shapeless", "crafting_shapeless" -> {
                val ingredients = keys.values.toList()
                buildJsonObject {
                    put("type", JsonPrimitive("minecraft:crafting_shapeless"))
                    put("ingredients", JsonArray(ingredients.map { buildJsonObject { put("item", JsonPrimitive(it)) } }))
                    put("result", buildJsonObject {
                        put("item", JsonPrimitive(result))
                        if (resultCount > 1) put("count", JsonPrimitive(resultCount))
                    })
                }
            }
            "smelting", "furnace" -> smeltingJson("minecraft:smelting", result, keys, experience, cookingTime)
            "blasting", "blast_furnace" -> smeltingJson("minecraft:blasting", result, keys, experience, cookingTime / 2)
            "stonecutting", "stonecutter" -> buildJsonObject {
                put("type", JsonPrimitive("minecraft:stonecutting"))
                put("ingredient", buildJsonObject { put("item", JsonPrimitive(keys.values.firstOrNull() ?: "minecraft:stone")) })
                put("result", buildJsonObject { put("id", JsonPrimitive(result)) })
                put("count", JsonPrimitive(resultCount))
            }
            else -> buildJsonObject { put("error", JsonPrimitive("未知配方类型 '$normalized'，支持：shaped/shapeless/smelting/blasting/stonecutting")) }
        }
        return prettyJson(root)
    }

    private fun smeltingJson(type: String, result: String, keys: Map<String, String>, experience: Float, cookingTime: Int) = buildJsonObject {
        put("type", JsonPrimitive(type))
        put("ingredient", buildJsonObject { put("item", JsonPrimitive(keys.values.firstOrNull() ?: "minecraft:raw_iron")) })
        put("result", buildJsonObject { put("id", JsonPrimitive(result)) })
        put("experience", JsonPrimitive(experience))
        put("cookingtime", JsonPrimitive(cookingTime))
    }
}

/**
 * lang JSON 深合并（移植自 ModCrafting mergeLangEntries）：
 * 读取既有 lang 文件，合并新条目，不覆盖已有值，返回序列化结果。
 */
object LangMerger {

    /** 解析 lang JSON；解析失败时按空表处理（保留原始文本不动）。 */
    fun merge(existing: String?, newEntries: Map<String, String>): String {
        if (newEntries.isEmpty()) return existing ?: "{}"
        val merged = linkedMapOf<String, String>()
        if (existing != null) {
            try {
                val parsed = Json.parseToJsonElement(existing).jsonObject
                parsed.forEach { (k, v) -> if (v is JsonPrimitive && v.isString) merged[k] = v.content }
            } catch (_: Exception) {
                // 不可解析：保留原内容，新条目追加
            }
        }
        newEntries.forEach { (k, v) -> merged.putIfAbsent(k, v) }
        return buildString {
            appendLine("{")
            merged.toList().forEachIndexed { i, (k, v) ->
                append("  ").append(jstr(k)).append(": ").append(jstr(v))
                if (i < merged.size - 1) append(',')
                appendLine()
            }
            append("}")
        }
    }
}