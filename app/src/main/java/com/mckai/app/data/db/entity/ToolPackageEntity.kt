package com.mckai.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tool_packages")
data class ToolPackageEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val version: String = "1.0.0",
    val author: String? = null,
    val type: String = "builtin",
    val toolsJson: String,
    val isEnabled: Boolean = true,
    val filePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
