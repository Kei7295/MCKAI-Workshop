package com.mckai.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workflows")
data class WorkflowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val triggerType: String = "manual",
    val triggerConfig: String? = null,
    val isEnabled: Boolean = true,
    val nodesJson: String? = null,
    val edgesJson: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastRunAt: Long? = null,
    val runCount: Int = 0
)
