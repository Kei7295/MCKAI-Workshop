package com.mckai.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mckai.app.data.db.dao.*
import com.mckai.app.data.db.entity.*

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        MessageAttachmentEntity::class,
        AssistantEntity::class,
        MemoryEntity::class,
        WorkflowEntity::class,
        ToolPackageEntity::class,
        FavoriteEntity::class,
        ProjectEntity::class,
        ProjectFileEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun messageAttachmentDao(): MessageAttachmentDao
    abstract fun assistantDao(): AssistantDao
    abstract fun memoryDao(): MemoryDao
    abstract fun workflowDao(): WorkflowDao
    abstract fun toolPackageDao(): ToolPackageDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun projectDao(): ProjectDao
    abstract fun projectFileDao(): ProjectFileDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "mckai.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
