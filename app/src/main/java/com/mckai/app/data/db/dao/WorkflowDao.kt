package com.mckai.app.data.db.dao

import androidx.room.*
import com.mckai.app.data.db.entity.WorkflowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkflowDao {
    @Query("SELECT * FROM workflows ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<WorkflowEntity>>

    @Query("SELECT * FROM workflows WHERE id = :id")
    suspend fun getById(id: Long): WorkflowEntity?

    @Query("SELECT * FROM workflows WHERE id = :id")
    fun observeById(id: Long): Flow<WorkflowEntity?>

    @Query("SELECT * FROM workflows WHERE isEnabled = 1")
    fun observeEnabled(): Flow<List<WorkflowEntity>>

    @Insert
    suspend fun insert(workflow: WorkflowEntity): Long

    @Update
    suspend fun update(workflow: WorkflowEntity)

    @Delete
    suspend fun delete(workflow: WorkflowEntity)

    @Query("DELETE FROM workflows WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE workflows SET lastRunAt = :time, runCount = runCount + 1 WHERE id = :id")
    suspend fun recordRun(id: Long, time: Long = System.currentTimeMillis())
}
