package com.mckai.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 4,
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
        /**
         * v3 -> v4：三张表 schema 校正（数据保留，仅重建结构）：
         * - messages：parentId 自引用外键（CASCADE 删除分支消息）
         * - favorites：messageId 外键 CASCADE（消息删除时清理收藏）
         * - project_files：新增 (projectId, filePath) 唯一索引（upsert 幂等）
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // messages
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS messages_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        conversationId INTEGER NOT NULL,
                        role TEXT NOT NULL,
                        content TEXT NOT NULL,
                        parentId INTEGER,
                        reasoningContent TEXT,
                        toolCallId TEXT,
                        toolCallsJson TEXT,
                        isFavorite INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        metadata TEXT,
                        branchGroupId TEXT,
                        isHidden INTEGER NOT NULL,
                        promptTokens INTEGER,
                        completionTokens INTEGER,
                        FOREIGN KEY(conversationId) REFERENCES conversations(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(parentId) REFERENCES messages(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_conversationId ON messages_new(conversationId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_parentId ON messages_new(parentId)")
                db.execSQL("INSERT INTO messages_new SELECT * FROM messages")
                db.execSQL("DROP TABLE messages")
                db.execSQL("ALTER TABLE messages_new RENAME TO messages")

                // favorites
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS favorites_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        messageId INTEGER NOT NULL,
                        conversationId INTEGER NOT NULL,
                        note TEXT,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(messageId) REFERENCES messages(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_favorites_messageId ON favorites_new(messageId)")
                db.execSQL("INSERT INTO favorites_new SELECT * FROM favorites")
                db.execSQL("DROP TABLE favorites")
                db.execSQL("ALTER TABLE favorites_new RENAME TO favorites")

                // project_files
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS project_files_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        projectId INTEGER NOT NULL,
                        filePath TEXT NOT NULL,
                        fileName TEXT NOT NULL,
                        content TEXT NOT NULL,
                        isGenerated INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(projectId) REFERENCES projects(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_project_files_projectId ON project_files_new(projectId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_project_files_projectId_filePath ON project_files_new(projectId, filePath)")
                db.execSQL("INSERT INTO project_files_new SELECT * FROM project_files")
                db.execSQL("DROP TABLE project_files")
                db.execSQL("ALTER TABLE project_files_new RENAME TO project_files")
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "mckai.db")
                .addMigrations(MIGRATION_3_4)
                .build()
    }
}