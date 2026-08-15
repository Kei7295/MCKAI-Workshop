package com.mckai.app.domain.tools

import android.content.Context
import android.util.Log
import com.mckai.app.data.llm.ToolDef
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap

enum class ToolPermission { STANDARD, ADMIN, ROOT }

data class ToolMetadata(
    val name: String,
    val description: String,
    val parameters: JsonObject,
    val required: List<String> = emptyList(),
    val permission: ToolPermission = ToolPermission.STANDARD,
    val category: String = "general"
)

class ToolRegistry {
    private val tools = ConcurrentHashMap<String, Pair<ToolMetadata, suspend (JsonObject) -> String>>()

    fun register(meta: ToolMetadata, handler: suspend (JsonObject) -> String) {
        tools[meta.name] = meta to handler
    }

    fun unregister(name: String) { tools.remove(name) }

    fun getMetadata(name: String): ToolMetadata? = tools[name]?.first

    fun getAllMetadata(): List<ToolMetadata> = tools.values.map { it.first }

    fun getToolsForLlm(): List<ToolDef> = tools.values.map { (meta, _) ->
        ToolDef(name = meta.name, description = meta.description, parameters = meta.parameters, required = meta.required)
    }

    fun getCategories(): Map<String, List<ToolMetadata>> = tools.values.map { it.first }.groupBy { it.category }

    /**
     * 统一执行入口。
     * @param allowSensitive 是否放行 ADMIN 权限工具（由调用方自行完成确认门禁）。
     * ROOT 工具永远拒绝。
     */
    suspend fun execute(name: String, args: JsonObject, allowSensitive: Boolean = false): String {
        val (meta, handler) = tools[name] ?: return "错误：未知工具 '$name'"
        if (meta.permission == ToolPermission.ROOT) {
            return "工具 $name 需要 ROOT 权限，已跳过。"
        }
        if (meta.permission == ToolPermission.ADMIN && !allowSensitive) {
            return "工具 $name 需要确认权限，已跳过。"
        }
        return try {
            handler(args)
        } catch (e: Exception) {
            Log.e("ToolRegistry", "Tool $name failed", e)
            "工具执行失败：${e.message}"
        }
    }

    companion object {
        fun buildDefault(context: Context, db: com.mckai.app.data.db.AppDatabase): ToolRegistry {
            val registry = ToolRegistry()
            registerCoreTools(registry)
            registerFileTools(registry)
            registerNetworkTools(registry)
            registerMinecraftTools(registry)
            registerMemoryTools(registry, db.memoryDao())
            registerSystemTools(registry, context)
            return registry
        }
    }
}
