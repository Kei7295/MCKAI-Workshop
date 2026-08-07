package com.mckai.app.data.llm

import okio.BufferedSource

class SseParser(private val source: BufferedSource) {
    private val dataLines = mutableListOf<String>()
    private var isSse = true
    private var firstLineChecked = false

    fun nextEvent(): String? {
        while (true) {
            val line = source.readUtf8Line() ?: return flush()
            if (!firstLineChecked) {
                firstLineChecked = true
                isSse = line.startsWith("data:") || line.startsWith(":") || line.startsWith("event:")
                if (!isSse) {
                    if (line.isNotBlank()) return line.trim()
                    continue
                }
            }
            if (!isSse) {
                if (line.isNotBlank()) return line.trim()
                continue
            }
            when {
                line.isEmpty() -> { val d = flush(); if (d != null) return d }
                line.startsWith("data:") -> dataLines.add(line.removePrefix("data:").trimStart())
                line.startsWith(":") -> { }
                line.startsWith("event:") -> { }
                line.startsWith("id:") -> { }
                line.startsWith("retry:") -> { }
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
