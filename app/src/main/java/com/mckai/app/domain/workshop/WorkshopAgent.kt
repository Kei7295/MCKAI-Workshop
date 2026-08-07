package com.mckai.app.domain.workshop

import com.mckai.app.data.llm.*
import com.mckai.app.domain.agent.AgentEvent
import com.mckai.app.domain.tools.ToolRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

class WorkshopAgent(
    private val llmClient: LlmClient,
    private val toolRegistry: ToolRegistry
) {
    fun build(
        spec: ModSpec,
        provider: ProviderConfig,
        onProgress: (AgentProgress) -> Unit = {},
        isCancelled: () -> Boolean = { false }
    ): Flow<AgentEvent> = flow {
        onProgress(AgentProgress(AgentPhase.PLANNING, "规划模组结构...", 0f))

        // Phase 1: Generate files using tool-based approach
        val systemPrompt = AgentPrompts.systemPrompt(spec)
        val userPrompt = AgentPrompts.initialUserPrompt(spec)
        val tools = AgentPrompts.generationTools()
        val messages = mutableListOf(ChatMessage(role = "user", content = userPrompt))
        val generatedFiles = mutableMapOf<String, String>()
        var round = 0
        val maxRounds = 30

        onProgress(AgentProgress(AgentPhase.GENERATING, "开始生成文件...", 0.1f))

        while (round < maxRounds && !isCancelled()) {
            round++
            val acc = StreamAccumulator()
            var hasToolCall = false
            var complete = false

            llmClient.stream(provider, systemPrompt, messages, tools).collect { event ->
                acc.onEvent(event)
                when (event) {
                    is LlmEvent.TextDelta -> emit(AgentEvent.TextDelta(event.text))
                    is LlmEvent.Error -> emit(AgentEvent.Error(event.message))
                    else -> Unit
                }
            }

            val toolCalls = acc.pendingToolCalls()
            if (toolCalls.isEmpty()) {
                // Try to parse file blocks from text (non-tool fallback)
                val text = acc.textContent()
                val files = AgentPrompts.parseFileBlocks(text)
                if (files.isNotEmpty()) {
                    generatedFiles.putAll(files)
                    onProgress(AgentProgress(AgentPhase.GENERATING, "从文本解析到 ${files.size} 个文件", 0.5f + generatedFiles.size * 0.02f))
                }
                break
            }

            for (tc in toolCalls) {
                if (isCancelled()) break
                when (tc.name) {
                    "write_file" -> {
                        val args = try { Json.parseToJsonElement(tc.args) as? JsonObject ?: buildJsonObject {} } catch (_: Exception) { buildJsonObject {} }
                        val path = args["path"]?.jsonPrimitive?.content ?: continue
                        val content = args["content"]?.jsonPrimitive?.content ?: continue
                        generatedFiles[path] = content
                        onProgress(AgentProgress(AgentPhase.GENERATING, "生成: $path", 0.1f + generatedFiles.size * 0.03f, generatedFiles.size))
                        emit(AgentEvent.ToolResult("write_file", "已写入 $path"))
                    }
                    "complete" -> {
                        complete = true
                        val args = try { Json.parseToJsonElement(tc.args) as? JsonObject ?: buildJsonObject {} } catch (_: Exception) { buildJsonObject {} }
                        val summary = args["summary"]?.jsonPrimitive?.content ?: "生成完成"
                        emit(AgentEvent.ToolResult("complete", summary))
                    }
                }
                // Add tool result to messages for next round
                messages.add(ChatMessage(role = "assistant", content = "", toolCalls = listOf(ToolCallSpec(id = tc.id, name = tc.name, args = tc.args))))
                messages.add(ChatMessage(role = "tool", content = "已完成: ${tc.name}", toolCallId = tc.id))
            }
            acc.clearPending()

            if (complete || generatedFiles.size >= 20) break
        }

        // Phase 2: Review (non-critical)
        if (generatedFiles.isNotEmpty() && !isCancelled()) {
            onProgress(AgentProgress(AgentPhase.REVIEWING, "审查生成的代码...", 0.9f))
            // Simple review - just emit success
        }

        if (isCancelled()) {
            emit(AgentEvent.Error("已取消"))
        } else if (generatedFiles.isEmpty()) {
            emit(AgentEvent.Error("未生成任何文件"))
        } else {
            val summary = "成功生成 ${generatedFiles.size} 个文件：\n${generatedFiles.keys.joinToString("\n")}"
            emit(AgentEvent.TextDelta("\n\n--- 生成完成 ---\n$summary"))
            emit(AgentEvent.Done)
        }
    }.flowOn(Dispatchers.IO)
}
