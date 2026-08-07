package com.mckai.app

import android.app.Application
import com.mckai.app.data.db.AppDatabase
import com.mckai.app.data.db.ProjectRepository
import com.mckai.app.data.llm.LlmClient
import com.mckai.app.data.repo.SettingsRepository
import com.mckai.app.domain.tools.ToolRegistry

class MCKAIApp : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}

class AppContainer(context: android.content.Context) {
    val database: AppDatabase = AppDatabase.build(context)
    val repo: ProjectRepository = ProjectRepository(database)
    val settings: SettingsRepository = SettingsRepository(context)
    val llmClient: LlmClient = LlmClient(LlmClient.newDefaultClient())
    val toolRegistry: ToolRegistry = ToolRegistry.buildDefault()
}
