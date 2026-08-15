package com.mckai.app.domain.tools

import com.mckai.app.data.db.dao.MemoryDao
import com.mckai.app.data.db.entity.MemoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

fun registerMemoryTools(r: ToolRegistry, dao: MemoryDao) {
    r.register(ToolMetadata(
        name = "save_memory",
        description = "保存一条记忆到长期记忆库",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("content", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("记忆内容")) })
                put("category", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("分类：general/user_preference/project/code/error")) })
                put("importance", buildJsonObject { put("type", JsonPrimitive("number")); put("description", JsonPrimitive("重要性 0-1，默认 0.5")) })
            })
            put("required", buildJsonArray { add(JsonPrimitive("content")) })
        },
        category = "memory"
    ), handler = { args ->
        val content = args["content"]?.jsonPrimitive?.content ?: return@register "请提供 content 参数"
        val category = args["category"]?.jsonPrimitive?.content ?: "general"
        val importance = args["importance"]?.jsonPrimitive?.floatOrNull ?: 0.5f
        withContext(Dispatchers.IO) {
            dao.insert(
                MemoryEntity(
                    content = content,
                    category = category,
                    importance = importance.coerceIn(0f, 1f)
                )
            )
        }
        "记忆已保存：[$category] ${content.take(100)}"
    })

    r.register(ToolMetadata(
        name = "search_memory",
        description = "搜索记忆库中的相关记忆",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("query", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("搜索关键词")) })
                put("limit", buildJsonObject { put("type", JsonPrimitive("integer")); put("description", JsonPrimitive("返回数量，默认5")) })
            })
            put("required", buildJsonArray { add(JsonPrimitive("query")) })
        },
        category = "memory"
    ), handler = { args ->
        val query = args["query"]?.jsonPrimitive?.content ?: return@register "请提供 query 参数"
        val limit = (args["limit"]?.jsonPrimitive?.intOrNull ?: 5).coerceIn(1, 20)
        val results = withContext(Dispatchers.IO) { dao.search(query, limit) }
        if (results.isEmpty()) "未找到相关记忆"
        else results.joinToString("\n") { "[${it.category}] ${it.content}" }
    })
}