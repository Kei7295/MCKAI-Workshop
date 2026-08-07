package com.mckai.app.data.db.dao

import androidx.room.*
import com.mckai.app.data.db.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY createdAt ASC")
    fun observeByConversation(convId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY createdAt ASC")
    suspend fun getByConversation(convId: Long): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getById(id: Long): MessageEntity?

    @Query("SELECT * FROM messages WHERE id = :id")
    fun observeById(id: Long): Flow<MessageEntity?>

    @Query("SELECT * FROM messages WHERE parentId = :parentId")
    fun observeChildren(parentId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE content LIKE '%' || :query || '%' ORDER BY createdAt DESC LIMIT :limit")
    suspend fun search(query: String, limit: Int = 50): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun observeFavorites(): Flow<List<MessageEntity>>

    @Insert
    suspend fun insert(message: MessageEntity): Long

    @Insert
    suspend fun insertAll(messages: List<MessageEntity>)

    @Update
    suspend fun update(message: MessageEntity)

    @Query("UPDATE messages SET content = :content WHERE id = :id")
    suspend fun updateContent(id: Long, content: String)

    @Query("UPDATE messages SET isFavorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: Long, fav: Boolean)

    @Delete
    suspend fun delete(message: MessageEntity)

    @Query("DELETE FROM messages WHERE conversationId = :convId")
    suspend fun deleteByConversation(convId: Long)

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :convId")
    suspend fun countByConversation(convId: Long): Int
}
