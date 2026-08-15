package com.mckai.app.ui.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mckai.app.MCKAIApp
import com.mckai.app.data.db.entity.ConversationEntity
import com.mckai.app.data.db.entity.MessageEntity
import com.mckai.app.data.llm.*
import com.mckai.app.domain.tools.ToolPermission
import com.mckai.app.domain.tools.ToolRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val availableTools: List<String> = emptyList(),
    // 流式工具调用可视化
    val activeToolCalls: List<String> = emptyList(),
    // token 统计
    val sessionTokens: Int = 0,
    // 敏感工具确认
    val pendingToolApproval: PendingToolApproval? = null,
    val autoApproveSensitive: Boolean = false
)

data class PendingToolApproval(val toolName: String, val argsSummary: String)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as MCKAIApp).appContainer
    private val db = container.database
    private val settings = container.settings
    private val llmClient = container.llmClient
    private val toolRegistry = container.toolRegistry

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var currentMessages = mutableListOf<MessageEntity>()
    private val json = Json { ignoreUnknownKeys = true }
    private var approvalGate: CompletableDeferred<Boolean>? = null
    private var generationJob: Job? = null
    private var conversationObservationJob: Job? = null

    init {
        viewModelScope.launch {
            settings.providers.collect { providers ->
                val lastId = settings.lastProviderId.first()
                val selected = providers.firstOrNull { it.id == lastId } ?: providers.firstOrNull { it.enabled }
                _state.update { it.copy(providers = providers, selectedProvider = selected) }
            }
        }
        viewModelScope.launch {
            settings.autoApproveSensitive.collect { v ->
                _state.update { it.copy(autoApproveSensitive = v) }
            }
        }
        _state.update { it.copy(availableTools = toolRegistry.getAllMetadata().map { t -> t.name }) }
    }

    fun loadConversation(convId: Long?) {
        if (convId == null) {
            conversationObservationJob?.cancel()
            conversationObservationJob = null
            _state.update { it.copy(conversationId = null, messages = emptyList(), streamingText = "", sessionTokens = 0) }
            currentMessages.clear()
            return
        }
        // 上一个会话的监听必须取消，防止旧会话的 DB 变更灌入新会话 UI
        conversationObservationJob?.cancel()
        conversationObservationJob = viewModelScope.launch {
            _state.update { it.copy(conversationId = convId) }
            db.messageDao().observeByConversation(convId).collect { msgs ->
                currentMessages = msgs.toMutableList()
                val total = msgs.sumOf { (it.promptTokens ?: 0) + (it.completionTokens ?: 0) }
                _state.update { it.copy(messages = msgs, sessionTokens = total) }
            }
        }
    }

    /** @return 是否真正开始生成（false 表示被拒绝，UI 不应清空输入框） */
    fun send(text: String): Boolean {
        if (text.isBlank() || _state.value.isGenerating) return false
        val provider = _state.value.selectedProvider ?: return false
        // 同步置位，杜绝两次快速点击都通过 isGenerating 检查的竞态
        _state.update { it.copy(isGenerating = true, error = null) }

        generationJob = viewModelScope.launch {
            try {
                var convId = _state.value.conversationId
                if (convId == null) {
                    convId = db.conversationDao().insert(ConversationEntity(title = text.take(30)))
                    _state.update { it.copy(conversationId = convId) }
                }

                val userMsg = MessageEntity(conversationId = convId, role = "user", content = text)
                val msgId = db.messageDao().insert(userMsg)
                currentMessages.add(userMsg.copy(id = msgId))
                _state.update { it.copy(messages = currentMessages.toList(), streamingText = "") }

                respond(convId, provider)
            } finally {
                if (generationJob?.isActive == true) generationJob = null
            }
        }
        return true
    }

    /**
     * 重新生成指定 assistant 消息：隐藏旧回复，生成新分支。
     * branchGroupId 相同的消息构成一个候选组。
     */
    fun regenerate(assistantMsgId: Long) {
        if (_state.value.isGenerating) return
        val provider = _state.value.selectedProvider ?: return
        _state.update { it.copy(isGenerating = true, streamingText = "", reasoningText = "", error = null) }
        generationJob = viewModelScope.launch {
            try {
                val target = currentMessages.firstOrNull { it.id == assistantMsgId && it.role == "assistant" } ?: run {
                    _state.update { it.copy(isGenerating = false) }
                    return@launch
                }
                val convId = target.conversationId
                // 隐藏旧回复
                val hidden = target.copy(isHidden = true)
                db.messageDao().update(hidden)
                val idx = currentMessages.indexOfFirst { it.id == target.id }
                if (idx >= 0) currentMessages[idx] = hidden
                _state.update { it.copy(messages = currentMessages.toList()) }
                // 生成新分支：优先复用已有组，否则以旧消息 id 为组
                val groupId = target.branchGroupId ?: "reg_${target.id}"
                respond(convId, provider, branchGroupId = groupId)
            } finally {
                if (generationJob?.isActive == true) generationJob = null
            }
        }
    }

    /** 切换候选组内选中的分支（仅在无生成时可用） */
    fun switchBranch(targetMsgId: Long) {
        if (_state.value.isGenerating) return
        viewModelScope.launch {
            val target = currentMessages.firstOrNull { it.id == targetMsgId } ?: return@launch
            val group = target.branchGroupId ?: return@launch
            val groupMsgs = currentMessages.filter { it.branchGroupId == group && !it.isHidden }
            if (groupMsgs.size < 2) return@launch
            // 隐藏当前选中，显示目标
            val visible = target.copy(isHidden = false)
            db.messageDao().update(visible)
            groupMsgs.filter { it.id != targetMsgId }.forEach { db.messageDao().update(it.copy(isHidden = true)) }
            currentMessages = currentMessages.toMutableList().apply {
                val i = indexOfFirst { it.id == targetMsgId }
                if (i >= 0) this[i] = visible
            }
            _state.update { it.copy(messages = currentMessages.toList()) }
        }
    }

    fun deleteMessage(msgId: Long) {
        viewModelScope.launch {
            val msg = currentMessages.firstOrNull { it.id == msgId } ?: return@launch
            db.messageDao().delete(msg)
            currentMessages.removeAll { it.id == msgId }
            _state.update { it.copy(messages = currentMessages.toList()) }
        }
    }

/** 单次回复流程：多 key 轮转 → 流式 → 工具循环 → 存档（含 token 账本） */
    private suspend fun respond(convId: Long, provider: ProviderConfig, branchGroupId: String? = null) {
        val effective = rotateKey(provider)
        val history = currentMessages
            .filter { !it.isHidden }
            .map { ChatMessage(role = it.role, content = it.content) }
        val systemPrompt = resolveSystemPrompt()

        val thisJob = generationJob
        try {
            val acc = StreamAccumulator()
            val fullText = StringBuilder()
            val reasoning = StringBuilder()
            var lastUsage: Usage? = null
            val toolCallsStack = mutableListOf<ToolCallSpec>()
            val activeTools = mutableListOf<String>()
            val agentTools: List<ToolDef> = if (_state.value.toolsEnabled) toolRegistry.getToolsForLlm() else emptyList()

            llmClient.stream(effective, systemPrompt, history, agentTools).collect { event ->
                if (thisJob?.isCancelled == true) return@collect
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
                    is LlmEvent.ToolCallDelta -> {
                        if (!event.name.isNullOrBlank()) {
                            activeTools.add(event.name)
                            _state.update { it.copy(activeToolCalls = activeTools.toList()) }
                        }
                    }
                    is LlmEvent.Error -> {
                        _state.update { it.copy(error = event.message, isGenerating = false, activeToolCalls = emptyList()) }
                        return@collect
                    }
                    is LlmEvent.Done -> {
                        lastUsage = event.usage
                        val toolCalls = acc.pendingToolCalls()
                        if (toolCalls.isNotEmpty() && _state.value.toolsEnabled) {
                            toolCallsStack += toolCalls
                            executeToolLoop(convId, effective, history, toolCalls, fullText, acc, activeTools, branchGroupId, systemPrompt)
                        } else {
                            saveAssistantMessage(convId, fullText.toString(), reasoning.toString(), lastUsage, emptyList(), branchGroupId)
                            _state.update { it.copy(isGenerating = false, streamingText = "", reasoningText = "", activeToolCalls = emptyList()) }
                        }
                    }
                    else -> Unit
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // 用户停止：丢弃半截流式内容
            _state.update { it.copy(isGenerating = false, streamingText = "", reasoningText = "", activeToolCalls = emptyList(), pendingToolApproval = null) }
            throw e
        } catch (e: Exception) {
            // 非事件形式的异常（如 JSON 序列化失败）不得击穿 VM 协程
            Log.e("ChatViewModel", "respond failed", e)
            _state.update { it.copy(error = e.message ?: "生成失败", isGenerating = false, streamingText = "", reasoningText = "", activeToolCalls = emptyList()) }
        }
    }

    private suspend fun executeToolLoop(
        convId: Long,
        provider: ProviderConfig,
        history: List<ChatMessage>,
        initialTools: List<ToolCallSpec>,
        fullText: StringBuilder,
        acc: StreamAccumulator,
        activeTools: MutableList<String>,
        branchGroupId: String?,
        systemPrompt: String
    ) {
        var toolQueue = initialTools
        var round = 0
        val messages = history.toMutableList()
        var lastUsage: Usage? = null
        val toolCallHistory = mutableListOf<ToolCallSpec>()
        val clarifications = mutableListOf<String>()

        while (toolQueue.isNotEmpty() && round < 10) {
            round++
            for (tc in toolQueue) {
                val args = try {
                    Json.parseToJsonElement(tc.args) as? JsonObject ?: buildJsonObject {}
                } catch (_: Exception) { buildJsonObject {} }
                // 权限门：ADMIN 需确认（除非自动批准），ROOT 一律拒绝
                val meta = toolRegistry.getMetadata(tc.name)
                val needApproval = meta?.permission == ToolPermission.ADMIN && !_state.value.autoApproveSensitive
                val rootDenied = meta?.permission == ToolPermission.ROOT
                val result: String = when {
                    rootDenied -> "工具 ${tc.name} 需要 ROOT 权限，已跳过。"
                    needApproval -> {
                        val argsSummary = args.entries.joinToString(", ") { (k, v) -> "$k=${v.toString().take(40)}" }
                        val gate = CompletableDeferred<Boolean>()
                        approvalGate = gate
                        _state.update { it.copy(pendingToolApproval = PendingToolApproval(tc.name, argsSummary)) }
                        val approved = gate.await()
                        _state.update { it.copy(pendingToolApproval = null) }
                        if (approved) toolRegistry.execute(tc.name, args, allowSensitive = true)
                        else "用户取消了工具调用：${tc.name}"
                    }
                    else -> toolRegistry.execute(tc.name, args)
                }
                // 澄清结果可视化：推进最终消息展示
                if (tc.name == "ask_clarification") {
                    val argsSummary = args.entries.joinToString(" ") { (k, v) -> v.toString().trim('"') }
                    clarifications += argsSummary
                }
                messages.add(ChatMessage(role = "assistant", content = "", toolCalls = listOf(tc)))
                messages.add(ChatMessage(role = "tool", content = result, toolCallId = tc.id))
                activeTools.add(tc.name)
                _state.update { it.copy(activeToolCalls = activeTools.toList()) }
            }
            toolCallHistory += toolQueue
            acc.clearPending()

            val agentTools2: List<ToolDef> = if (_state.value.toolsEnabled) toolRegistry.getToolsForLlm() else emptyList()
            llmClient.stream(provider, systemPrompt, messages, agentTools2).collect { event ->
                acc.onEvent(event)
                when (event) {
                    is LlmEvent.TextDelta -> {
                        fullText.append(event.text)
                        _state.update { it.copy(streamingText = fullText.toString()) }
                    }
                    is LlmEvent.ToolCallDelta -> {
                        if (!event.name.isNullOrBlank()) {
                            activeTools.add(event.name)
                            _state.update { it.copy(activeToolCalls = activeTools.toList()) }
                        }
                    }
                    is LlmEvent.Done -> { lastUsage = event.usage }
                    else -> Unit
                }
            }
            toolQueue = acc.pendingToolCalls()
        }

        val finalContent = if (clarifications.isNotEmpty()) {
            buildString {
                appendLine("### 需要澄清")
                clarifications.forEach { appendLine("- $it") }
                appendLine()
                append(fullText)
            }
        } else fullText.toString()
        saveAssistantMessage(convId, finalContent, "", lastUsage, toolCallHistory, branchGroupId)
        _state.update { it.copy(isGenerating = false, streamingText = "", reasoningText = "", activeToolCalls = emptyList()) }
    }

    private suspend fun saveAssistantMessage(
        convId: Long,
        content: String,
        reasoning: String,
        usage: Usage?,
        toolCalls: List<ToolCallSpec>,
        branchGroupId: String?
    ) {
        if (content.isBlank()) return
        val toolsJson = if (toolCalls.isNotEmpty()) {
            runCatching { json.encodeToString(toolCalls) }.getOrNull()
        } else null
        val msg = MessageEntity(
            conversationId = convId,
            role = "assistant",
            content = content,
            reasoningContent = reasoning.ifBlank { null },
            toolCallsJson = toolsJson,
            promptTokens = usage?.promptTokens,
            completionTokens = usage?.completionTokens,
            branchGroupId = branchGroupId
        )
        val id = db.messageDao().insert(msg)
        currentMessages.add(msg.copy(id = id))
        val total = currentMessages.sumOf { (it.promptTokens ?: 0) + (it.completionTokens ?: 0) }
        _state.update { it.copy(messages = currentMessages.toList(), sessionTokens = total) }
        db.conversationDao().rename(convId, currentMessages.firstOrNull { it.role == "user" }?.content?.take(30) ?: "新对话")
    }

    /** 多 API key 轮转：每次请求前推进 index 并持久化 */
    private suspend fun rotateKey(provider: ProviderConfig): ProviderConfig {
        if (provider.apiKeys.size < 2) return provider
        val rotated = provider.copy(currentKeyIndex = (provider.currentKeyIndex + 1) % provider.apiKeys.size)
        settings.upsertProvider(rotated)
        return rotated
    }

    /** 解析当前激活助手的 systemPrompt，替换 {{变量}} */
    private suspend fun resolveSystemPrompt(): String {
        val activeId = settings.activeAssistantId.first()
        val assistant = if (activeId > 0) db.assistantDao().getById(activeId) else null
        val base = assistant?.systemPrompt
            ?: "你是 MCKAI 模组工坊 AI 助手，帮助用户开发 Minecraft 模组。"
        val device = android.os.Build.MODEL
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return base
            .replace("{{name}}", assistant?.name ?: "MCKAI 助手")
            .replace("{{device}}", device)
            .replace("{{date}}", date)
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
        approvalGate?.complete(false)
        approvalGate = null
        // 取消在途生成：流式 collect、工具批准 await 都会随协程取消而退出
        generationJob?.cancel()
        generationJob = null
        _state.update { it.copy(isGenerating = false, activeToolCalls = emptyList(), pendingToolApproval = null, streamingText = "", reasoningText = "") }
    }

    fun approveTool() {
        approvalGate?.complete(true)
        approvalGate = null
    }

    fun rejectTool() {
        approvalGate?.complete(false)
        approvalGate = null
    }

    fun setAutoApproveSensitive(enabled: Boolean) {
        viewModelScope.launch { settings.setAutoApproveSensitive(enabled) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun clearSession() {
        loadConversation(null)
    }
}