package com.mckai.app.data.db.dao

import androidx.room.*
import com.mckai.app.data.db.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getById(id: Long): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE id = :id")
    fun observeById(id: Long): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE title LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun search(query: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun observeFavorites(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE branchParentId = :parentId")
    fun observeBranches(parentId: Long): Flow<List<ConversationEntity>>

    @Insert
    suspend fun insert(conversation: ConversationEntity): Long

    @Update
    suspend fun update(conversation: ConversationEntity)

    @Query("UPDATE conversations SET title = :title, updatedAt = :time WHERE id = :id")
    suspend fun rename(id: Long, title: String, time: Long = System.currentTimeMillis())

    @Query("UPDATE conversations SET isFavorite = :fav, updatedAt = :time WHERE id = :id")
    suspend fun setFavorite(id: Long, fav: Boolean, time: Long = System.currentTimeMillis())

    @Query("UPDATE conversations SET assistantId = :assistantId, updatedAt = :time WHERE id = :id")
    suspend fun setAssistant(id: Long, assistantId: Long?, time: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: Long)
}
