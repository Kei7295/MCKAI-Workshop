package com.mckai.app.ui.workshop

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mckai.app.MCKAIApp
import com.mckai.app.data.db.entity.ProjectEntity
import com.mckai.app.data.db.entity.ProjectFileEntity
import com.mckai.app.data.llm.ProviderConfig
import com.mckai.app.domain.agent.AgentEvent
import com.mckai.app.domain.workshop.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class WorkshopStep { EDITION, DESCRIBE, GENERATING, RESULT }

data class WorkshopUiState(
    val step: WorkshopStep = WorkshopStep.EDITION,
    val edition: ModEdition = ModEdition.JAVA_FABRIC,
    val name: String = "",
    val mcVersion: String = ModEdition.JAVA_FABRIC.defaultMcVersion,
    val description: String = "",
    val features: String = "",
    val modId: String = "",
    val selectedProvider: ProviderConfig? = null,
    val isGenerating: Boolean = false,
    val progress: AgentProgress? = null,
    val generatedFiles: Map<String, String> = emptyMap(),
    val resultMessage: String = "",
    val success: Boolean = false,
    val providers: List<ProviderConfig> = emptyList(),
    val log: List<String> = emptyList()
)

class WorkshopViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as MCKAIApp).appContainer
    private val db = container.database
    private val settings = container.settings
    private val llmClient = container.llmClient
    private val toolRegistry = container.toolRegistry

    private val _state = MutableStateFlow(WorkshopUiState())
    val state: StateFlow<WorkshopUiState> = _state.asStateFlow()

    private var cancelFlag = false
    private var buildJob: kotlinx.coroutines.Job? = null
    private var generatedFiles: Map<String, String> = emptyMap()

    init {
        viewModelScope.launch {
            settings.providers.collect { providers ->
                val lastId = settings.lastProviderId.first()
                val selected = providers.firstOrNull { it.id == lastId } ?: providers.firstOrNull { it.enabled }
                _state.update { it.copy(providers = providers, selectedProvider = selected) }
            }
        }
    }

    fun selectEdition(edition: ModEdition) {
        _state.update { it.copy(edition = edition, mcVersion = edition.defaultMcVersion) }
    }

    fun updateName(name: String) { _state.update { it.copy(name = name) } }
    fun updateMcVersion(v: String) { _state.update { it.copy(mcVersion = v) } }
    fun updateDescription(d: String) { _state.update { it.copy(description = d) } }
    fun updateFeatures(f: String) { _state.update { it.copy(features = f) } }
    fun updateModId(id: String) { _state.update { it.copy(modId = id) } }
    fun selectProvider(p: ProviderConfig) { _state.update { it.copy(selectedProvider = p) } }

    fun goToDescribe() { _state.update { it.copy(step = WorkshopStep.DESCRIBE) } }
    fun backToEdition() { _state.update { it.copy(step = WorkshopStep.EDITION) } }

    fun start() {
        val s = _state.value
        if (s.name.isBlank() || s.selectedProvider == null) return

        val spec = ModSpec(
            name = s.name,
            edition = s.edition,
            mcVersion = s.mcVersion,
            description = s.description,
            features = s.features,
            modId = s.modId
        )

        _state.update { it.copy(step = WorkshopStep.GENERATING, isGenerating = true, log = emptyList(), generatedFiles = emptyMap()) }
        cancelFlag = false
        generatedFiles = emptyMap()

        buildJob = viewModelScope.launch {
            val agent = WorkshopAgent(llmClient, toolRegistry)
            try {
                agent.build(spec, s.selectedProvider!!,
                    onProgress = { progress ->
                        _state.update { it.copy(progress = progress, log = it.log + progress.message) }
                    },
                    isCancelled = { cancelFlag }
                ).collect { event ->
                    when (event) {
                        is AgentEvent.TextDelta -> _state.update { it.copy(log = it.log + event.text.take(200)) }
                        is AgentEvent.ToolResult -> _state.update { it.copy(log = it.log + "[工具] ${event.name}: ${event.result.take(100)}") }
                        is AgentEvent.Files -> generatedFiles = event.files
                        is AgentEvent.Error -> _state.update { it.copy(log = it.log + "错误: ${event.message}") }
                        is AgentEvent.Done -> {
                            val files = generatedFiles
                            val success = files.isNotEmpty()
                            _state.update {
                                it.copy(
                                    step = WorkshopStep.RESULT,
                                    isGenerating = false,
                                    generatedFiles = files,
                                    success = success,
                                    resultMessage = if (success) "成功生成 ${files.size} 个文件" else "生成完成（无文件）"
                                )
                            }
                            if (success) saveProject(spec, files)
                        }
                        else -> Unit
                    }
                }
            } finally {
                if (buildJob?.isActive == true) buildJob = null
                _state.update { it.copy(isGenerating = false) }
            }
        }
    }

    fun cancel() {
        cancelFlag = true
        // 真正取消生成协程：flow 的清理与后续事件随之终止
        buildJob?.cancel()
        buildJob = null
        _state.update { it.copy(isGenerating = false, step = WorkshopStep.RESULT, resultMessage = "已取消", success = false) }
    }

    fun reset() {
        _state.update {
            WorkshopUiState(
                providers = it.providers,
                selectedProvider = it.selectedProvider
            )
        }
    }

    private suspend fun saveProject(spec: ModSpec, files: Map<String, String>) {
        val projectId = db.projectDao().insert(
            ProjectEntity(
                name = spec.name,
                edition = spec.edition.name,
                mcVersion = spec.mcVersion,
                modId = spec.modId,
                description = spec.description
            )
        )
        files.forEach { (path, content) ->
            db.projectFileDao().upsert(
                ProjectFileEntity(
                    projectId = projectId,
                    filePath = path,
                    fileName = path.substringAfterLast("/"),
                    content = content
                )
            )
        }
    }
}
