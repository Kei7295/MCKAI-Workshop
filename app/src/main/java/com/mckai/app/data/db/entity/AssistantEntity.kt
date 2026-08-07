package com.mckai.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assistants")
data class AssistantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val avatar: String? = null,
    val systemPrompt: String,
    val description: String? = null,
    val providerId: String? = null,
    val modelId: String? = null,
    val temperature: Float? = null,
    val maxTokens: Int? = null,
    val toolsEnabled: Boolean = true,
    val memoryEnabled: Boolean = false,
    val promptVariables: String? = null,
    val isBuiltIn: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
