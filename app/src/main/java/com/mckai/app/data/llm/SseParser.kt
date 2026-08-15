package com.mckai.app.data.llm

import okio.BufferedSource

/**
 * SSE 解析器。
 * 判定策略：不以首行定生死——部分网关会先发注释/空行。
 * 一旦出现 `data:` 行进入 SSE 模式；否则按"每行一个原始事件"退化处理。
 */
class SseParser(private val source: BufferedSource) {
    private val dataLines = mutableListOf<String>()
    private var sseConfirmed = false

    fun nextEvent(): String? {
        while (true) {
            val line = source.readUtf8Line() ?: return flush()
            if (!sseConfirmed) {
                if (line.startsWith(":")) continue          // 注释/心跳
                if (line.startsWith("event:") || line.startsWith("id:") || line.startsWith("retry:")) {
                    sseConfirmed = true
                    continue
                }
                if (line.startsWith("data:")) {
                    sseConfirmed = true
                    dataLines.add(line.removePrefix("data:").trimStart())
                    continue
                }
                // 非 SSE：逐行返回原始内容（旧逻辑：首行探测）
                if (line.isNotBlank()) return line.trim()
                continue
            }
            when {
                line.isEmpty() -> { val d = flush(); if (d != null) return d }
                line.startsWith("data:") -> dataLines.add(line.removePrefix("data:").trimStart())
                line.startsWith(":") -> { /* comment: ignore */ }
                line.startsWith("event:") -> { /* event name: ignored */ }
                line.startsWith("id:") -> { /* id: ignored */ }
                line.startsWith("retry:") -> { /* retry: ignored */ }
            }
        }
    }

    private fun flush(): String? {
        if (dataLines.isEmpty()) return null
        val data = dataLines.joinToString("\n")
        dataLines.clear()
        return data
    }
}