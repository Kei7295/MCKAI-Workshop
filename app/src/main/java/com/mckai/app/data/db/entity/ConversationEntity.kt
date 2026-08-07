package com.mckai.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "新对话",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val branchParentId: Long? = null,
    val branchLabel: String? = null,
    val assistantId: Long? = null,
    val providerId: String? = null,
    val modelId: String? = null,
    val metadata: String? = null,
    val isFavorite: Boolean = false
)
