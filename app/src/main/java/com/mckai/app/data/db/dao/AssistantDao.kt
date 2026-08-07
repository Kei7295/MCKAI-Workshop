package com.mckai.app.data.db.dao

import androidx.room.*
import com.mckai.app.data.db.entity.AssistantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssistantDao {
    @Query("SELECT * FROM assistants ORDER BY sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<AssistantEntity>>

    @Query("SELECT * FROM assistants WHERE id = :id")
    suspend fun getById(id: Long): AssistantEntity?

    @Query("SELECT * FROM assistants WHERE id = :id")
    fun observeById(id: Long): Flow<AssistantEntity?>

    @Query("SELECT * FROM assistants WHERE isBuiltIn = 1 ORDER BY sortOrder ASC")
    fun observeBuiltIn(): Flow<List<AssistantEntity>>

    @Insert
    suspend fun insert(assistant: AssistantEntity): Long

    @Update
    suspend fun update(assistant: AssistantEntity)

    @Delete
    suspend fun delete(assistant: AssistantEntity)

    @Query("DELETE FROM assistants WHERE id = :id")
    suspend fun deleteById(id: Long)
}
