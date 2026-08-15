package com.mckai.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mckai.app.MCKAIApp
import com.mckai.app.data.db.entity.AssistantEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AssistantForm(
    val id: Long = 0,
    val name: String = "",
    val avatar: String = "",
    val systemPrompt: String = "",
    val description: String = "",
    val providerId: String? = null,
    val modelId: String? = null,
    val temperature: Float? = null,
    val toolsEnabled: Boolean = true,
    val memoryEnabled: Boolean = false,
    val isBuiltIn: Boolean = false
)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as MCKAIApp).appContainer
    private val db = container.database

    val assistants: StateFlow<List<AssistantEntity>> = db.assistantDao()
        .observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeId = MutableStateFlow<Long?>(null)
    val activeId: StateFlow<Long?> = _activeId.asStateFlow()

    init {
        viewModelScope.launch {
            _activeId.value = container.settings.activeAssistantId.first().takeIf { it > 0 }
        }
    }

    fun save(form: AssistantForm) {
        viewModelScope.launch {
            // 写库不可被页面返回取消（onBack 会销毁本 VM 的 scope）
            withContext(kotlinx.coroutines.NonCancellable) {
                if (form.id > 0) {
                    val existing = db.assistantDao().getById(form.id)
                    db.assistantDao().update(
                        existing?.copy(
                            name = form.name,
                            avatar = form.avatar,
                            systemPrompt = form.systemPrompt,
                            description = form.description.ifBlank { null },
                            providerId = form.providerId,
                            modelId = form.modelId,
                            temperature = form.temperature,
                            toolsEnabled = form.toolsEnabled,
                            memoryEnabled = form.memoryEnabled
                        ) ?: AssistantEntity(
                            id = form.id, name = form.name, avatar = form.avatar,
                            systemPrompt = form.systemPrompt, description = form.description, isBuiltIn = false
                        )
                    )
                } else {
                    db.assistantDao().insert(
                        AssistantEntity(
                            name = form.name,
                            avatar = form.avatar,
                            systemPrompt = form.systemPrompt,
                            description = form.description.ifBlank { null },
                            providerId = form.providerId,
                            modelId = form.modelId,
                            temperature = form.temperature,
                            toolsEnabled = form.toolsEnabled,
                            memoryEnabled = form.memoryEnabled,
                            isBuiltIn = false,
                            sortOrder = 100
                        )
                    )
                }
            }
        }
    }

    fun delete(id: Long, isBuiltIn: Boolean) {
        if (isBuiltIn) return
        viewModelScope.launch {
            db.assistantDao().deleteById(id)
            if (_activeId.value == id) {
                container.settings.setActiveAssistant(0)
                _activeId.value = null
            }
        }
    }

    fun setActive(id: Long?) {
        viewModelScope.launch {
            container.settings.setActiveAssistant(id ?: 0L)
            _activeId.value = id
        }
    }
}