package com.mckai.app.data.db.dao

import androidx.room.*
import com.mckai.app.data.db.entity.ProjectFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectFileDao {
    @Query("SELECT * FROM project_files WHERE projectId = :projectId ORDER BY filePath ASC")
    fun observeByProject(projectId: Long): Flow<List<ProjectFileEntity>>

    @Query("SELECT * FROM project_files WHERE projectId = :projectId ORDER BY filePath ASC")
    suspend fun getByProject(projectId: Long): List<ProjectFileEntity>

    @Query("SELECT * FROM project_files WHERE id = :id")
    suspend fun getById(id: Long): ProjectFileEntity?

    @Query("SELECT * FROM project_files WHERE projectId = :projectId AND filePath = :path")
    suspend fun getByPath(projectId: Long, path: String): ProjectFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(file: ProjectFileEntity): Long

    @Insert
    suspend fun insertAll(files: List<ProjectFileEntity>)

    @Update
    suspend fun update(file: ProjectFileEntity)

    @Delete
    suspend fun delete(file: ProjectFileEntity)

    @Query("DELETE FROM project_files WHERE projectId = :projectId")
    suspend fun deleteByProject(projectId: Long)

    @Query("UPDATE project_files SET content = :content WHERE id = :id")
    suspend fun updateContent(id: Long, content: String)
}
