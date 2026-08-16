package com.mckai.app.data.backup

import com.mckai.app.data.db.dao.ConversationDao
import com.mckai.app.data.db.dao.MessageDao
import com.mckai.app.data.db.entity.ConversationEntity
import com.mckai.app.data.db.entity.MessageEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 对话备份/恢复（移植自 rikkahub ExportData 框架思路）：
 * 单个会话（含消息树、分支、token 统计）导出为带版本标记的 JSON，
 * 导入时重映射 id 并保留时间戳与分支结构。
 */
object ConversationBackup {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    @Serializable
    data class BackupData(
        val version: Int = 1,
        val type: String = "conversation",
        val exportedAt: Long = System.currentTimeMillis(),
        val conversation: ConversationDto,
        val messages: List<MessageDto>
    )

    @Serializable
    data class ConversationDto(
        val title: String,
        val createdAt: Long,
        val updatedAt: Long,
        val branchParentId: Long? = null,
        val branchLabel: String? = null,
        val assistantId: Long? = null,
        val providerId: String? = null,
        val modelId: String? = null,
        val metadata: String? = null,
        val isFavorite: Boolean = false
    )

    @Serializable
    data class MessageDto(
        val role: String,
        val content: String,
        val parentId: Long? = null,
        val reasoningContent: String? = null,
        val toolCallId: String? = null,
        val toolCallsJson: String? = null,
        val isFavorite: Boolean = false,
        val createdAt: Long,
        val metadata: String? = null,
        val branchGroupId: String? = null,
        val isHidden: Boolean = false,
        val promptTokens: Int? = null,
        val completionTokens: Int? = null
    )

    fun export(conv: ConversationEntity, messages: List<MessageEntity>): String {
        val data = BackupData(
            conversation = ConversationDto(
                title = conv.title,
                createdAt = conv.createdAt,
                updatedAt = conv.updatedAt,
                branchParentId = conv.branchParentId,
                branchLabel = conv.branchLabel,
                assistantId = conv.assistantId,
                providerId = conv.providerId,
                modelId = conv.modelId,
                metadata = conv.metadata,
                isFavorite = conv.isFavorite
            ),
            messages = messages.map {
                MessageDto(
                    role = it.role,
                    content = it.content,
                    parentId = it.parentId,
                    reasoningContent = it.reasoningContent,
                    toolCallId = it.toolCallId,
                    toolCallsJson = it.toolCallsJson,
                    isFavorite = it.isFavorite,
                    createdAt = it.createdAt,
                    metadata = it.metadata,
                    branchGroupId = it.branchGroupId,
                    isHidden = it.isHidden,
                    promptTokens = it.promptTokens,
                    completionTokens = it.completionTokens
                )
            }
        )
        return json.encodeToString(BackupData.serializer(), data)
    }

    /** 导入备份：重映射会话与消息 id，恢复为全新会话。返回新会话 id。 */
    suspend fun import(raw: String, conversationDao: ConversationDao, messageDao: MessageDao): Result<Long> =
        runCatching {
            val data = json.decodeFromString(BackupData.serializer(), raw)
            require(data.type == "conversation") { "不是对话备份文件" }
            val dto = data.conversation
            val newId = conversationDao.insert(
                ConversationEntity(
                    title = dto.title.ifBlank { "导入的对话" },
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                    branchParentId = null,
                    branchLabel = dto.branchLabel,
                    assistantId = dto.assistantId,
                    providerId = dto.providerId,
                    modelId = dto.modelId,
                    metadata = dto.metadata,
                    isFavorite = dto.isFavorite
                )
            )
            // parentId 旧→新映射；父消息缺失时置为根
            val idMap = mutableMapOf<Long, Long>()
            data.messages.sortedBy { it.createdAt }.forEach { m ->
                val newParent = m.parentId?.let { idMap[it] }
                val newMsgId = messageDao.insert(
                    MessageEntity(
                        conversationId = newId,
                        role = m.role,
                        content = m.content,
                        parentId = newParent,
                        reasoningContent = m.reasoningContent,
                        toolCallId = m.toolCallId,
                        toolCallsJson = m.toolCallsJson,
                        isFavorite = m.isFavorite,
                        createdAt = m.createdAt,
                        metadata = m.metadata,
                        branchGroupId = m.branchGroupId,
                        isHidden = m.isHidden,
                        promptTokens = m.promptTokens,
                        completionTokens = m.completionTokens
                    )
                )
                m.parentId?.let { idMap[it] = newMsgId }
            }
            newId
        }
}