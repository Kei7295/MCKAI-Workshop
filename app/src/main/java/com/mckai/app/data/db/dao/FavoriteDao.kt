package com.mckai.app.data.db.dao

import androidx.room.*
import com.mckai.app.data.db.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE messageId = :messageId")
    suspend fun getByMessage(messageId: Long): FavoriteEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE messageId = :messageId)")
    fun observeIsFavorite(messageId: Long): Flow<Boolean>

    @Insert
    suspend fun insert(favorite: FavoriteEntity): Long

    @Delete
    suspend fun delete(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE messageId = :messageId")
    suspend fun deleteByMessage(messageId: Long)
}
