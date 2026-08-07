package com.mckai.app.data.db.dao

import androidx.room.*
import com.mckai.app.data.db.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY importance DESC, lastAccessedAt DESC")
    fun observeAll(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE id = :id")
    suspend fun getById(id: Long): MemoryEntity?

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY importance DESC")
    fun observeByCategory(category: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE content LIKE '%' || :query || '%' ORDER BY importance DESC LIMIT :limit")
    suspend fun search(query: String, limit: Int = 20): List<MemoryEntity>

    @Query("SELECT * FROM memories ORDER BY importance DESC LIMIT :limit")
    suspend fun getTop(limit: Int = 20): List<MemoryEntity>

    @Insert
    suspend fun insert(memory: MemoryEntity): Long

    @Update
    suspend fun update(memory: MemoryEntity)

    @Query("UPDATE memories SET accessCount = accessCount + 1, lastAccessedAt = :time WHERE id = :id")
    suspend fun touch(id: Long, time: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM memories")
    suspend fun count(): Int
}
