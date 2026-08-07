package com.mckai.app.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mckai.app.MCKAIApp
import com.mckai.app.data.db.entity.ConversationEntity
import com.mckai.app.data.db.entity.MessageEntity
import com.mckai.app.data.llm.*
import com.mckai.app.domain.agent.AgentEngine
import com.mckai.app.domain.agent.AgentEvent
import com.mckai.app.domain.tools.ToolRegistry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive

data class ChatUiState(
    val conversationId: Long? = null,
    val messages: List<MessageEntity> = emptyList(),
    val isGenerating: Boolean = false,
    val streamingText: String = "",
    val reasoningText: String = "",
    val error: String? = null,
    val providers: List<ProviderConfig> = emptyList(),
    val selectedProvider: ProviderConfig? = null,
    val toolsEnabled: Boolean = true,
    val availableTools: List<String> = emptyList()
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as MCKAIApp).appContainer
    private val db = container.database
    private val settings = container.settings
    private val llmClient = container.llmClient
    private val toolRegistry = container.toolRegistry

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var currentMessages = mutableListOf<MessageEntity>()

    init {
        viewModelScope.launch {
            settings.providers.collect { providers ->
                val lastId = settings.lastProviderId.first()
                val selected = providers.firstOrNull { it.id == lastId } ?: providers.firstOrNull { it.enabled }
                _state.update { it.copy(providers = providers, selectedProvider = selected) }
            }
        }
        _state.update { it.copy(availableTools = toolRegistry.getAllMetadata().map { t -> t.name }) }
    }

    fun loadConversation(convId: Long?) {
        if (convId == null) {
            _state.update { it.copy(conversationId = null, messages = emptyList(), streamingText = "") }
            currentMessages.clear()
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(conversationId = convId) }
            db.messageDao().observeByConversation(convId).collect { msgs ->
                currentMessages = msgs.toMutableList()
                _state.update { it.copy(messages = msgs) }
            }
        }
    }

    fun send(text: String) {
        if (text.isBlank() || _state.value.isGenerating) return
        val provider = _state.value.selectedProvider ?: return

        viewModelScope.launch {
            var convId = _state.value.conversationId
            if (convId == null) {
                convId = db.conversationDao().insert(ConversationEntity(title = text.take(30)))
                _state.update { it.copy(conversationId = convId) }
            }

            val userMsg = MessageEntity(conversationId = convId, role = "user", content = text)
            val msgId = db.messageDao().insert(userMsg)
            currentMessages.add(userMsg.copy(id = msgId))
            _state.update { it.copy(messages = currentMessages.toList(), isGenerating = true, streamingText = "", error = null) }

            val history = currentMessages.dropLast(1).map { ChatMessage(role = it.role, content = it.content) }
            val acc = StreamAccumulator()
            val fullText = StringBuilder()
            val reasoning = StringBuilder()
            val agentTools: List<ToolDef> = if (_state.value.toolsEnabled) toolRegistry.getToolsForLlm() else emptyList()

            llmClient.stream(provider, "你是 MCKAI 模组工坊 AI 助手，帮助用户开发 Minecraft 模组。", history + ChatMessage(role = "user", content = text), agentTools).collect { event ->
                acc.onEvent(event)
                when (event) {
                    is LlmEvent.TextDelta -> {
                        fullText.append(event.text)
                        _state.update { it.copy(streamingText = fullText.toString()) }
                    }
                    is LlmEvent.ReasoningDelta -> {
                        reasoning.append(event.text)
                        _state.update { it.copy(reasoningText = reasoning.toString()) }
                    }
                    is LlmEvent.Error -> {
                        _state.update { it.copy(error = event.message, isGenerating = false) }
                    }
                    is LlmEvent.Done -> {
                        val toolCalls = acc.pendingToolCalls()
                        if (toolCalls.isNotEmpty() && _state.value.toolsEnabled) {
                            // Execute tools and continue
                            executeToolLoop(convId, provider, history + ChatMessage(role = "user", content = text), toolCalls, fullText, acc)
                        } else {
                            saveAssistantMessage(convId, fullText.toString(), reasoning.toString())
                            _state.update { it.copy(isGenerating = false, streamingText = "", reasoningText = "") }
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private suspend fun executeToolLoop(
        convId: Long,
        provider: ProviderConfig,
        history: List<ChatMessage>,
        initialTools: List<ToolCallSpec>,
        fullText: StringBuilder,
        acc: StreamAccumulator
    ) {
        var toolQueue = initialTools
        var round = 0
        val messages = history.toMutableList()

        while (toolQueue.isNotEmpty() && round < 10) {
            round++
            // Execute tools
            for (tc in toolQueue) {
                val args = try {
                    Json.parseToJsonElement(tc.args) as? JsonObject ?: buildJsonObject {}
                } catch (_: Exception) { buildJsonObject {} }
                val result = toolRegistry.execute(tc.name, args)
                messages.add(ChatMessage(role = "assistant", content = "", toolCalls = listOf(tc)))
                messages.add(ChatMessage(role = "tool", content = result, toolCallId = tc.id))
            }
            acc.clearPending()

            // Continue streaming
            val agentTools2: List<ToolDef> = if (_state.value.toolsEnabled) toolRegistry.getToolsForLlm() else emptyList()
            llmClient.stream(provider, "你是 MCKAI 模组工坊 AI 助手。", messages, agentTools2).collect { event ->
                acc.onEvent(event)
                when (event) {
                    is LlmEvent.TextDelta -> {
                        fullText.append(event.text)
                        _state.update { it.copy(streamingText = fullText.toString()) }
                    }
                    is LlmEvent.Done -> { }
                    else -> Unit
                }
            }
            toolQueue = acc.pendingToolCalls()
        }

        saveAssistantMessage(convId, fullText.toString(), "")
        _state.update { it.copy(isGenerating = false, streamingText = "", reasoningText = "") }
    }

    private suspend fun saveAssistantMessage(convId: Long, content: String, reasoning: String) {
        if (content.isBlank()) return
        val msg = MessageEntity(
            conversationId = convId,
            role = "assistant",
            content = content,
            reasoningContent = reasoning.ifBlank { null }
        )
        val id = db.messageDao().insert(msg)
        currentMessages.add(msg.copy(id = id))
        _state.update { it.copy(messages = currentMessages.toList()) }
        db.conversationDao().rename(convId, currentMessages.firstOrNull { it.role == "user" }?.content?.take(30) ?: "新对话")
    }

    fun selectProvider(provider: ProviderConfig) {
        _state.update { it.copy(selectedProvider = provider) }
        viewModelScope.launch { settings.setLastProvider(provider.id, provider.defaultModel) }
    }

    fun toggleTools() {
        _state.update { it.copy(toolsEnabled = !it.toolsEnabled) }
        viewModelScope.launch { settings.setToolsEnabled(_state.value.toolsEnabled) }
    }

    fun stop() {
        _state.update { it.copy(isGenerating = false) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
