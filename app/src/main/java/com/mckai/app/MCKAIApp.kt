package com.mckai.app

import android.app.Application
import com.mckai.app.data.db.AppDatabase
import com.mckai.app.data.db.ProjectRepository
import com.mckai.app.data.llm.LlmClient
import com.mckai.app.data.repo.SettingsRepository
import com.mckai.app.domain.tools.ToolRegistry
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MCKAIApp : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        appContainer.seedAssistants()
    }
}

class AppContainer(context: android.content.Context) {
    val database: AppDatabase = AppDatabase.build(context)
    val repo: ProjectRepository = ProjectRepository(database)
    val settings: SettingsRepository = SettingsRepository(context)
    val llmClient: LlmClient = LlmClient(LlmClient.newDefaultClient())
    val toolRegistry: ToolRegistry = ToolRegistry.buildDefault(context, database)

    /** 首次启动写入内置助手种子（幂等，IO 线程，不阻塞首帧） */
    fun seedAssistants() {
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val existing = database.assistantDao().observeAll().firstOrNull() ?: return@launch
            if (existing.isEmpty()) {
                com.mckai.app.domain.assistant.BuiltInAssistants.all.forEach { database.assistantDao().insert(it) }
            } else if (existing.none { it.isBuiltIn }) {
                com.mckai.app.domain.assistant.BuiltInAssistants.all.forEach { database.assistantDao().insert(it) }
            }
        }
    }
}
