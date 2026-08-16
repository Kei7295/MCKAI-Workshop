package com.mckai.app.data.character

import com.mckai.app.data.db.entity.AssistantEntity
import kotlinx.serialization.json.*
import kotlinx.serialization.json.buildJsonObject

/**
 * 角色卡（Character Card）仓库。移植自 Operit CharacterCardManager 的思路：
 * 支持 Tavern v2 JSON 导入导出（含 PNG 尾部附加 JSON 的常见格式），
 * 映射为 MCKAI 的 AssistantEntity。
 */
data class CharacterCard(
    val name: String,
    val description: String = "",
    val personality: String = "",
    val scenario: String = "",
    val firstMes: String = "",
    val mesExample: String = "",
    val systemPrompt: String = "",
    val creatorNotes: String = "",
    val creator: String = "",
    val characterVersion: String = "1.0.0",
    val tags: List<String> = emptyList()
)

object CharacterCardRepository {

    /** 解析 Tavern v2 JSON 文本。 */
    fun parseJson(text: String): CharacterCard? = runCatching {
        val obj = Json.parseToJsonElement(text).jsonObject
        CharacterCard(
            name = obj["name"]?.jsonPrimitive?.contentOrNull ?: return null,
            description = str(obj, "description"),
            personality = str(obj, "personality"),
            scenario = str(obj, "scenario"),
            firstMes = str(obj, "first_mes"),
            mesExample = str(obj, "mes_example"),
            systemPrompt = str(obj, "system_prompt"),
            creatorNotes = str(obj, "creator_notes"),
            creator = str(obj, "creator"),
            characterVersion = str(obj, "character_version").ifBlank { "1.0.0" },
            tags = obj["tags"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
        )
    }.getOrNull()

    /** 解析角色卡文件字节：纯 JSON 或 PNG 尾部附加 JSON（Tavern 常见格式）。 */
    fun parseBytes(bytes: ByteArray): CharacterCard? {
        val asText = bytes.toString(Charsets.UTF_8).trim()
        parseJson(asText)?.let { return it }
        // PNG 尾部 JSON：从尾部向前找 '{'，尝试解析
        if (asText.startsWith("\u0089PNG") || bytes.size > 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte()) {
            val lastBrace = asText.lastIndexOf('{')
            if (lastBrace > 0) {
                parseJson(asText.substring(lastBrace))?.let { return it }
            }
        }
        return null
    }

    /** 导出 Tavern v2 JSON。 */
    fun toTavernJson(card: CharacterCard): String {
        val root = buildJsonObject {
            put("name", JsonPrimitive(card.name))
            put("description", JsonPrimitive(card.description))
            put("personality", JsonPrimitive(card.personality))
            put("scenario", JsonPrimitive(card.scenario))
            put("first_mes", JsonPrimitive(card.firstMes))
            put("mes_example", JsonPrimitive(card.mesExample))
            put("system_prompt", JsonPrimitive(card.systemPrompt))
            put("creator_notes", JsonPrimitive(card.creatorNotes))
            put("creator", JsonPrimitive(card.creator))
            put("character_version", JsonPrimitive(card.characterVersion))
            put("tags", JsonArray(card.tags.map { JsonPrimitive(it) }))
        }
        return Json { prettyPrint = true; encodeDefaults = true }.encodeToString(JsonElement.serializer(), root)
    }

    /** 映射为 MCKAI 助手实体：personality/mes_example/scenario 并入 systemPrompt。 */
    fun toAssistant(card: CharacterCard): AssistantEntity {
        val prompt = buildString {
            append(card.systemPrompt)
            if (card.personality.isNotBlank()) {
                append("\n\n【角色性格】").append(card.personality)
            }
            if (card.scenario.isNotBlank()) {
                append("\n\n【场景】").append(card.scenario)
            }
            if (card.mesExample.isNotBlank()) {
                append("\n\n【对话示例】\n").append(card.mesExample)
            }
            if (card.firstMes.isNotBlank()) {
                append("\n\n【开场白】").append(card.firstMes)
            }
        }.trim()
        return AssistantEntity(
            name = card.name,
            avatar = null,
            systemPrompt = prompt,
            description = card.description.ifBlank { "角色卡：${card.name}" },
            toolsEnabled = false
        )
    }

    private fun str(obj: JsonObject, key: String): String =
        obj[key]?.jsonPrimitive?.contentOrNull ?: ""
}