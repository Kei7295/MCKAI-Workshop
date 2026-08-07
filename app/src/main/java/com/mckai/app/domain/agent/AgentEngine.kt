package com.mckai.app.domain.agent

import com.mckai.app.data.llm.*
import com.mckai.app.domain.tools.ToolRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.*

enum class AgentMode { CHAT, PLAN, EXECUTE }

class AgentEngine(
    private val llmClient: LlmClient,
    private val toolRegistry: ToolRegistry
) {
    data class AgentConfig(
        val provider: ProviderConfig,
        val systemPrompt: String,
        val toolsEnabled: Boolean = true,
        val maxRounds: Int = 40,
        val memoryContext: String? = null
    )

    fun run(
        config: AgentConfig,
        userMessage: String,
        history: List<ChatMessage> = emptyList(),
        onProgress: (String) -> Unit = {}
    ): Flow<AgentEvent> = flow {
        val mode = classifyMode(userMessage, config)
        onProgress("模式：$mode")

        when (mode) {
            AgentMode.CHAT -> chatMode(config, userMessage, history).collect { emit(it) }
            AgentMode.PLAN -> planMode(config, userMessage, history, onProgress).collect { emit(it) }
            AgentMode.EXECUTE -> executeMode(config, userMessage, history, onProgress).collect { emit(it) }
        }
    }

    private suspend fun classifyMode(input: String, config: AgentConfig): AgentMode {
        val lower = input.lowercase()
        if (lower.startsWith("/chat")) return AgentMode.CHAT
        if (lower.startsWith("/plan")) return AgentMode.PLAN
        if (lower.startsWith("/exec")) return AgentMode.EXECUTE

        val keywords = listOf("生成", "写", "创建", "实现", "开发", "编码", "build", "create", "generate", "implement", "code", "fix", "bug", "错误", "崩溃", "crash")
        if (keywords.any { lower.contains(it) }) return AgentMode.EXECUTE

        val planKeywords = listOf("分析", "规划", "计划", "设计", "架构", "分析一下", "怎么做", "方案")
        if (planKeywords.any { lower.contains(it) }) return AgentMode.PLAN

        return AgentMode.CHAT
    }

    private suspend fun chatMode(config: AgentConfig, userMessage: String, history: List<ChatMessage>): Flow<AgentEvent> = flow {
        val messages = buildMessages(config, userMessage, history)
        val tools: List<com.mckai.app.data.llm.ToolDef> = if (config.toolsEnabled) toolRegistry.getToolsForLlm() else emptyList()
        val acc = StreamAccumulator()

        llmClient.stream(config.provider, config.systemPrompt, messages, tools).collect { event ->
            acc.onEvent(event)
            when (event) {
                is LlmEvent.TextDelta -> emit(AgentEvent.TextDelta(event.text))
                is LlmEvent.ReasoningDelta -> emit(AgentEvent.ReasoningDelta(event.text))
                is LlmEvent.Error -> emit(AgentEvent.Error(event.message))
                is LlmEvent.Done -> {
                    if (acc.pendingToolCalls().isNotEmpty()) {
                        executeToolCalls(config, acc, messages, userMessage).collect { emit(it) }
                    }
                    emit(AgentEvent.Done)
                }
                else -> Unit
            }
        }
    }

    private suspend fun planMode(config: AgentConfig, userMessage: String, history: List<ChatMessage>, onProgress: (String) -> Unit): Flow<AgentEvent> = flow {
        val planPrompt = """
            |你是一个专业的 Minecraft 模组开发规划师。请分析用户的请求并输出结构化计划。
            |输出格式：
            |## 目标
            |简要描述要做什么
            |
            |## 步骤
            |1. 第一步...
            |2. 第二步...
            |...
            |
            |## 需要的文件
            |- 文件路径和用途
            |
            |## 注意事项
            |潜在问题和解决方案
        """.trimMargin()

        val fullSystem = config.systemPrompt + "\n\n$planPrompt" +
            (config.memoryContext?.let { "\n\n相关记忆：\n$it" } ?: "")
        val messages = buildMessages(config, userMessage, history)

        llmClient.stream(config.provider, fullSystem, messages).collect { event ->
            when (event) {
                is LlmEvent.TextDelta -> emit(AgentEvent.TextDelta(event.text))
                is LlmEvent.Error -> emit(AgentEvent.Error(event.message))
                is LlmEvent.Done -> emit(AgentEvent.Done)
                else -> Unit
            }
        }
    }

    private suspend fun executeMode(config: AgentConfig, userMessage: String, history: List<ChatMessage>, onProgress: (String) -> Unit): Flow<AgentEvent> = flow {
        val messages = buildMessages(config, userMessage, history)
        val tools: List<com.mckai.app.data.llm.ToolDef> = if (config.toolsEnabled) toolRegistry.getToolsForLlm() else emptyList()
        val acc = StreamAccumulator()
        var round = 0

        llmClient.stream(config.provider, config.systemPrompt, messages, tools).collect { event ->
            acc.onEvent(event)
            when (event) {
                is LlmEvent.TextDelta -> emit(AgentEvent.TextDelta(event.text))
                is LlmEvent.ReasoningDelta -> emit(AgentEvent.ReasoningDelta(event.text))
                is LlmEvent.Error -> emit(AgentEvent.Error(event.message))
                is LlmEvent.Done -> {
                    val pendingTools = acc.pendingToolCalls()
                    if (pendingTools.isNotEmpty() && round < config.maxRounds) {
                        round++
                        onProgress("执行工具调用（第 $round 轮）...")
                        emit(AgentEvent.ToolExecutionStart(pendingTools.map { it.name }))
                        val results = executeToolCallsSync(pendingTools)
                        results.forEach { (name, result) ->
                            emit(AgentEvent.ToolResult(name, result))
                        }
                        acc.clearPending()
                        // Continue the loop with tool results
                    } else {
                        emit(AgentEvent.Done)
                    }
                }
                else -> Unit
            }
        }
    }

    private suspend fun executeToolCalls(config: AgentConfig, acc: StreamAccumulator, messages: List<ChatMessage>, originalInput: String): Flow<AgentEvent> = flow {
        val pendingTools = acc.pendingToolCalls()
        if (pendingTools.isEmpty()) return@flow

        emit(AgentEvent.ToolExecutionStart(pendingTools.map { it.name }))
        val toolResults = executeToolCallsSync(pendingTools)
        toolResults.forEach { (name, result) ->
            emit(AgentEvent.ToolResult(name, result))
        }
    }

    private suspend fun executeToolCallsSync(tools: List<ToolCallSpec>): List<Pair<String, String>> {
        return tools.map { spec ->
            val args = try {
                Json.parseToJsonElement(spec.args) as? JsonObject ?: buildJsonObject {}
            } catch (_: Exception) { buildJsonObject {} }
            val result = toolRegistry.execute(spec.name, args)
            spec.name to result
        }
    }

    private fun buildMessages(config: AgentConfig, userMessage: String, history: List<ChatMessage>): List<ChatMessage> {
        val msgs = history.toMutableList()
        config.memoryContext?.let {
            msgs.add(ChatMessage(role = "system", content = "相关记忆：\n$it"))
        }
        msgs.add(ChatMessage(role = "user", content = userMessage))
        return msgs
    }
}

sealed interface AgentEvent {
    data class TextDelta(val text: String) : AgentEvent
    data class ReasoningDelta(val text: String) : AgentEvent
    data class ToolExecutionStart(val names: List<String>) : AgentEvent
    data class ToolResult(val name: String, val result: String) : AgentEvent
    data class Error(val message: String) : AgentEvent
    object Done : AgentEvent
}
