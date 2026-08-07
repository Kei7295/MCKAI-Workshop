package com.mckai.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val edition: String,
    val mcVersion: String,
    val modId: String? = null,
    val description: String? = null,
    val packageName: String? = null,
    val author: String? = null,
    val version: String = "1.0.0",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
