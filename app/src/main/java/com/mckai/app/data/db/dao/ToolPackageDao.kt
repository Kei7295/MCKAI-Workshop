package com.mckai.app.data.db.dao

import androidx.room.*
import com.mckai.app.data.db.entity.ToolPackageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ToolPackageDao {
    @Query("SELECT * FROM tool_packages ORDER BY name ASC")
    fun observeAll(): Flow<List<ToolPackageEntity>>

    @Query("SELECT * FROM tool_packages WHERE id = :id")
    suspend fun getById(id: String): ToolPackageEntity?

    @Query("SELECT * FROM tool_packages WHERE isEnabled = 1")
    fun observeEnabled(): Flow<List<ToolPackageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pkg: ToolPackageEntity)

    @Delete
    suspend fun delete(pkg: ToolPackageEntity)

    @Query("DELETE FROM tool_packages WHERE id = :id")
    suspend fun deleteById(id: String)
}
