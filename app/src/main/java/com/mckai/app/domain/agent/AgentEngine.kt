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

        // 记忆统一并入 systemPrompt（OpenAI 兼容 API 要求 system 在首位）
        val effectiveConfig = config.copy(
            systemPrompt = config.systemPrompt +
                (config.memoryContext?.let { "\n\n相关记忆：\n$it" } ?: "")
        )

        when (mode) {
            AgentMode.CHAT, AgentMode.EXECUTE ->
                runToolLoop(effectiveConfig, userMessage, history, onProgress).collect { emit(it) }
            AgentMode.PLAN ->
                planMode(effectiveConfig, userMessage, history, onProgress).collect { emit(it) }
        }
    }

    private suspend fun classifyMode(input: String, config: AgentConfig): AgentMode {
        val lower = input.lowercase().trim()
        if (lower.startsWith("/chat")) return AgentMode.CHAT
        if (lower.startsWith("/plan")) return AgentMode.PLAN
        if (lower.startsWith("/exec")) return AgentMode.EXECUTE

        // 纯闲聊/短消息不进工具模式，避免误判（整词匹配，避免 "decode" 命中 "code"）
        if (lower.length <= 12) return AgentMode.CHAT

        val execKeywords = listOf("生成", "写代码", "创建", "实现", "开发", "编码", "修复", "调bug", "build", "create", "generate", "implement", "fix bug", "crash", "报错", "错误处理")
        val planKeywords = listOf("分析", "规划", "计划", "设计", "架构", "怎么做", "方案", "路线图")

        val words = lower.split(Regex("[^a-z0-9\u4e00-\u9fff]+")).filter { it.isNotBlank() }
        if (execKeywords.any { k -> lower.contains(k) }) return AgentMode.EXECUTE
        if (planKeywords.any { k -> lower.contains(k) }) return AgentMode.PLAN
        if (words.any { it == "code" || it == "bug" || it == "fix" }) return AgentMode.EXECUTE
        return AgentMode.CHAT
    }

    /**
     * 真正的工具循环：每轮 = 一次流式生成 + （如有工具调用）执行并回喂结果，
     * 直到模型不再请求工具或达到 maxRounds。
     * 每轮结束时必然 emit 一次 Done（无工具 / 错误 / 达到上限）。
     */
    private suspend fun runToolLoop(
        config: AgentConfig,
        userMessage: String,
        history: List<ChatMessage>,
        onProgress: (String) -> Unit
    ): Flow<AgentEvent> = flow {
        val messages = buildMessages(config, userMessage, history).toMutableList()
        val tools: List<ToolDef> = if (config.toolsEnabled) toolRegistry.getToolsForLlm() else emptyList()
        var round = 0

        while (round < config.maxRounds) {
            val acc = StreamAccumulator()
            var errored = false
            var doneReceived = false

            llmClient.stream(config.provider, config.systemPrompt, messages, tools).collect { event ->
                acc.onEvent(event)
                when (event) {
                    is LlmEvent.TextDelta -> emit(AgentEvent.TextDelta(event.text))
                    is LlmEvent.ReasoningDelta -> emit(AgentEvent.ReasoningDelta(event.text))
                    is LlmEvent.Error -> {
                        errored = true
                        emit(AgentEvent.Error(event.message))
                    }
                    is LlmEvent.Done -> doneReceived = true
                    else -> Unit
                }
            }
            if (errored) {
                emit(AgentEvent.Done)
                break
            }

            val pending = acc.pendingToolCalls()
            if (pending.isEmpty() || tools.isEmpty() || !doneReceived) {
                emit(AgentEvent.Done)
                break
            }

            round++
            onProgress("执行工具调用（第 $round 轮）...")
            emit(AgentEvent.ToolExecutionStart(pending.map { it.name }))
            val results = executeToolCallsSync(pending)
            results.forEach { (name, result) ->
                emit(AgentEvent.ToolResult(name, result))
            }
            // 回喂：assistant 工具调用声明 + 各工具结果
            messages.add(ChatMessage(role = "assistant", content = acc.textContent(), toolCalls = pending))
            results.forEachIndexed { i, (_, result) ->
                messages.add(ChatMessage(role = "tool", content = result, toolCallId = pending[i].id))
            }
            acc.clearPending()
        }

        if (round >= config.maxRounds) emit(AgentEvent.Done)
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

        val fullSystem = config.systemPrompt + "\n\n$planPrompt"
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
        // 记忆并入 systemPrompt（上游）而不是作为中间的 system 消息
        msgs.add(ChatMessage(role = "user", content = userMessage))
        return msgs
    }
}

sealed interface AgentEvent {
    data class TextDelta(val text: String) : AgentEvent
    data class ReasoningDelta(val text: String) : AgentEvent
    data class ToolExecutionStart(val names: List<String>) : AgentEvent
    data class ToolResult(val name: String, val result: String) : AgentEvent
    /** 工作台产物：完整文件清单（内容随事件携带） */
    data class Files(val files: Map<String, String>) : AgentEvent
    data class Error(val message: String) : AgentEvent
    object Done : AgentEvent
}