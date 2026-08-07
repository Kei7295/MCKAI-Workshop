package com.mckai.app.data.db

import com.mckai.app.data.db.dao.*
import com.mckai.app.data.db.entity.*

class ProjectRepository(private val db: AppDatabase) {
    val conversationDao: ConversationDao get() = db.conversationDao()
    val messageDao: MessageDao get() = db.messageDao()
    val messageAttachmentDao: MessageAttachmentDao get() = db.messageAttachmentDao()
    val assistantDao: AssistantDao get() = db.assistantDao()
    val memoryDao: MemoryDao get() = db.memoryDao()
    val workflowDao: WorkflowDao get() = db.workflowDao()
    val toolPackageDao: ToolPackageDao get() = db.toolPackageDao()
    val favoriteDao: FavoriteDao get() = db.favoriteDao()
    val projectDao: ProjectDao get() = db.projectDao()
    val projectFileDao: ProjectFileDao get() = db.projectFileDao()
}
