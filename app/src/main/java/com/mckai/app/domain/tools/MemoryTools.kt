package com.mckai.app.domain.tools

import kotlinx.serialization.json.*

fun registerMemoryTools(r: ToolRegistry) {
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
        // Memory will be saved by the MemoryManager through the ViewModel
        "记忆已记录：[$category] ${content.take(100)}"
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
        "搜索记忆：'$query'（实际搜索由 MemoryManager 处理）"
    })
}
