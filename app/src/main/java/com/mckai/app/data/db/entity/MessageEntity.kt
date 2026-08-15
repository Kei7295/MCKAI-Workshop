package com.mckai.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversationId"), Index("parentId")]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val role: String,
    val content: String,
    val parentId: Long? = null,
    val reasoningContent: String? = null,
    val toolCallId: String? = null,
    val toolCallsJson: String? = null,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val metadata: String? = null,
    // -- 分支支持 (RikkaHub MessageNode 移植) --
    val branchGroupId: String? = null,
    val isHidden: Boolean = false,
    // -- token 统计 (使用量账本) --
    val promptTokens: Int? = null,
    val completionTokens: Int? = null
)
