package com.mckai.app.data.db.dao

import androidx.room.*
import com.mckai.app.data.db.entity.MessageAttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageAttachmentDao {
    @Query("SELECT * FROM message_attachments WHERE messageId = :messageId")
    fun observeByMessage(messageId: Long): Flow<List<MessageAttachmentEntity>>

    @Query("SELECT * FROM message_attachments WHERE messageId = :messageId")
    suspend fun getByMessage(messageId: Long): List<MessageAttachmentEntity>

    @Insert
    suspend fun insert(attachment: MessageAttachmentEntity): Long

    @Insert
    suspend fun insertAll(attachments: List<MessageAttachmentEntity>)

    @Delete
    suspend fun delete(attachment: MessageAttachmentEntity)

    @Query("DELETE FROM message_attachments WHERE messageId = :messageId")
    suspend fun deleteByMessage(messageId: Long)
}
