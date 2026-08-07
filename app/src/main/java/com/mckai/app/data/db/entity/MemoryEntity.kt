package com.mckai.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val summary: String? = null,
    val category: String = "general",
    val sourceConversationId: Long? = null,
    val importance: Float = 0.5f,
    val accessCount: Int = 0,
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val metadata: String? = null
)
