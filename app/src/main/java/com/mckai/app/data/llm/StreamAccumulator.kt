package com.mckai.app.data.llm

import java.util.concurrent.atomic.AtomicLong

class StreamAccumulator {
    class PendingTool {
        var id: String? = null
        var name: String? = null
        val args = StringBuilder()
    }

    private val text = StringBuilder()
    private val reasoning = StringBuilder()
    private val pending = mutableMapOf<Int, PendingTool>()
    private val fallbackIdSeq = AtomicLong(0)

    fun onEvent(event: LlmEvent) {
        when (event) {
            is LlmEvent.TextDelta -> text.append(event.text)
            is LlmEvent.ReasoningDelta -> reasoning.append(event.text)
            is LlmEvent.ToolCallDelta -> {
                val tool = pending.getOrPut(event.index) { PendingTool() }
                if (!event.id.isNullOrBlank()) tool.id = event.id
                if (!event.name.isNullOrBlank()) tool.name = event.name
                if (event.argsDelta.isNotBlank()) tool.args.append(event.argsDelta)
            }
            else -> Unit
        }
    }

    fun textContent(): String = text.toString()
    fun reasoningContent(): String = reasoning.toString()

    fun pendingToolCalls(): List<ToolCallSpec> =
        pending.values.mapNotNull { p ->
            val name = p.name ?: return@mapNotNull null
            ToolCallSpec(
                id = p.id ?: "call_${fallbackIdSeq.incrementAndGet()}",
                name = name,
                args = LlmClient.fixJson(p.args.toString())
            )
        }

    fun clearPending() { pending.clear() }

    fun reset() {
        pending.clear()
        text.clear()
        reasoning.clear()
    }
}