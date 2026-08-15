package com.mckai.app.data.llm

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.job
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

class LlmClient(private val client: OkHttpClient) {

    companion object {
        private const val TAG = "LlmClient"
        private val toolCallIdSeq = AtomicLong(0)

        fun newDefaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        fun nextToolCallId(prefix: String = "call_"): String =
            "$prefix${toolCallIdSeq.incrementAndGet()}"

        fun truncate(s: String, max: Int = 800): String =
            if (s.length <= max) s else s.take(max) + "..."

        /** 修复流式截断的 JSON：补尾逗号、截取首个对象/数组、闭合括号、闭合字符串引号。 */
        fun fixJson(s: String): String {
            var t = s.trim()
            if (t.isEmpty()) return "{}"
            // Fix trailing comma before } or ]
            t = t.replace(Regex(",\\s*}"), "}")
            t = t.replace(Regex(",\\s*]"), "]")
            // Try to find the JSON object/array
            val firstBrace = t.indexOfFirst { it == '{' || it == '[' }
            if (firstBrace > 0) t = t.substring(firstBrace)
            // Close unterminated string literal (odd unescaped quotes)
            var quoteCount = 0
            var i = 0
            while (i < t.length) {
                when {
                    t[i] == '\\' -> i += 2
                    t[i] == '"' -> { quoteCount++; i++ }
                    else -> i++
                }
            }
            if (quoteCount % 2 == 1) t += "\""
            // Close unclosed objects/arrays
            val openBraces = t.count { it == '{' }
            val closeBraces = t.count { it == '}' }
            val openBrackets = t.count { it == '[' }
            val closeBrackets = t.count { it == ']' }
            repeat(openBrackets - closeBrackets) { t += "]" }
            repeat(openBraces - closeBraces) { t += "}" }
            return t
        }
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    // ================================================================ Response DTOs

    @Serializable
    private data class OpenAiFunctionDelta(val name: String? = null, val arguments: String? = null)
    @Serializable
    private data class OpenAiToolCallDelta(val index: Int = 0, val id: String? = null, val function: OpenAiFunctionDelta? = null)
    @Serializable
    private data class OpenAiDelta(
        val content: String? = null,
        @SerialName("reasoning_content") val reasoningContent: String? = null,
        @SerialName("tool_calls") val toolCalls: List<OpenAiToolCallDelta>? = null
    )
    @Serializable
    private data class OpenAiChoice(
        val index: Int = 0,
        val delta: OpenAiDelta = OpenAiDelta(),
        @SerialName("finish_reason") val finishReason: String? = null
    )
    @Serializable
    private data class OpenAiUsage(
        @SerialName("prompt_tokens") val promptTokens: Int = 0,
        @SerialName("completion_tokens") val completionTokens: Int = 0,
        @SerialName("total_tokens") val totalTokens: Int = 0
    )
    @Serializable
    private data class OpenAiChunk(
        val choices: List<OpenAiChoice> = emptyList(),
        val usage: OpenAiUsage? = null
    )

    @Serializable
    private data class GeminiPart(val text: String? = null, @SerialName("functionCall") val functionCall: JsonElement? = null)
    @Serializable
    private data class GeminiContent(val parts: List<GeminiPart> = emptyList())
    @Serializable
    private data class GeminiCandidate(val content: GeminiContent? = null, @SerialName("finishReason") val finishReason: String? = null)
    @Serializable
    private data class GeminiChunk(val candidates: List<GeminiCandidate> = emptyList())

    @Serializable
    private data class ClaudeDelta(
        val type: String? = null,
        val text: String? = null,
        @SerialName("partial_json") val partialJson: String? = null
    )
    @Serializable
    private data class ClaudeToolBlock(val type: String? = null, val id: String? = null, val name: String? = null, val input: JsonElement? = null)
    @Serializable
    private data class ClaudeChunk(
        val type: String = "",
        val index: Int = 0,
        @SerialName("content_block") val contentBlock: ClaudeToolBlock? = null,
        val delta: ClaudeDelta? = null
    )

    // ================================================================ Request DTOs

    @Serializable
    private data class OpenAiReqMessage(
        val role: String,
        val content: JsonElement? = null,
        @SerialName("tool_call_id") val toolCallId: String? = null,
        @SerialName("tool_calls") val toolCalls: JsonElement? = null
    )
    @Serializable
    private data class OpenAiFunctionDef(val name: String, val description: String, val parameters: JsonElement)
    @Serializable
    private data class OpenAiReqTool(val type: String = "function", val function: OpenAiFunctionDef)
    @Serializable
    private data class OpenAiRequest(
        val model: String,
        val messages: List<OpenAiReqMessage>,
        val temperature: Float? = null,
        @SerialName("max_tokens") val maxTokens: Int? = null,
        val stream: Boolean = true,
        val tools: List<OpenAiReqTool>? = null
    )

    @Serializable
    private data class GeminiReqPart(val text: String? = null, @SerialName("inlineData") val inlineData: GeminiInlineData? = null, @SerialName("functionCall") val functionCall: JsonElement? = null, @SerialName("functionResponse") val functionResponse: GemFunctionResponse? = null)
    @Serializable
    private data class GemFunctionResponse(val name: String, val response: JsonElement)
    @Serializable
    private data class GeminiInlineData(val mimeType: String, val data: String)
    @Serializable
    private data class GeminiReqContent(val role: String, val parts: List<GeminiReqPart>)
    @Serializable
    private data class GeminiFunctionDef(val name: String, val description: String, val parameters: JsonElement)
    @Serializable
    private data class GeminiReqTool(val functionDeclarations: List<GeminiFunctionDef>)
    @Serializable
    private data class GeminiConfig(val temperature: Float? = null, @SerialName("maxOutputTokens") val maxOutputTokens: Int? = null)
    @Serializable
    private data class GeminiRequest(
        val contents: List<GeminiReqContent>,
        @SerialName("systemInstruction") val systemInstruction: GeminiReqContent? = null,
        @SerialName("generationConfig") val generationConfig: GeminiConfig? = null,
        val tools: List<GeminiReqTool>? = null
    )

    @Serializable
    private data class ClaudeReqTool(val name: String, val description: String, @SerialName("input_schema") val inputSchema: JsonElement)
    @Serializable
    private data class ClaudeContent(val type: String = "text", val text: String)
    @Serializable
    private data class ClaudeReqMessage(val role: String, val content: JsonElement)
    @Serializable
    private data class ClaudeRequest(
        val model: String,
        @SerialName("max_tokens") val maxTokens: Int,
        val system: String? = null,
        val messages: List<ClaudeReqMessage>,
        val stream: Boolean = true,
        val tools: List<ClaudeReqTool>? = null
    )

    // ================================================================ Public API

    fun stream(
        provider: ProviderConfig,
        systemPrompt: String,
        messages: List<ChatMessage>,
        tools: List<ToolDef> = emptyList()
    ): Flow<LlmEvent> = flow {
        val request = buildRequest(provider, systemPrompt, messages, tools)
        Log.d(TAG, "Request: ${request.method} ${request.url}")
        val call = client.newCall(request)
        currentCoroutineContext().job.invokeOnCompletion { cause ->
            if (cause != null) call.cancel()
        }
        var eventCount = 0
        var doneEmitted = false
        var lastUsage: Usage? = null

        suspend fun emitDone() {
            if (!doneEmitted) {
                doneEmitted = true
                emit(LlmEvent.Done(lastUsage))
            }
        }

        try {
            call.execute().use { response ->
                Log.d(TAG, "Response: HTTP ${response.code}, CT=${response.header("Content-Type")}")
                val body = response.body ?: throw IOException("空的响应体")
                if (!response.isSuccessful) {
                    val errText = try { body.string().take(400) } catch (_: Exception) { "" }
                    throw IOException("请求失败 (HTTP ${response.code})：$errText")
                }
                val parser = SseParser(body.source())
                while (true) {
                    val data = parser.nextEvent() ?: break
                    if (data.isBlank()) continue
                    val events = parsePayload(provider.type, data)
                    for (event in events) {
                        if (event is LlmEvent.Done) {
                            lastUsage = event.usage
                            emitDone()
                        } else {
                            emit(event)
                            eventCount++
                        }
                        if (event is LlmEvent.Error) { /* keep streaming; final emitDone below */ }
                    }
                }
            }
            Log.d(TAG, "Stream complete: $eventCount events")
        } finally {
            emitDone()
        }
    }.flowOn(Dispatchers.IO).catch { e ->
        Log.e(TAG, "Stream error: ${e.message}", e)
        emit(LlmEvent.Error(e.message ?: "未知网络错误"))
    }

    suspend fun testConnection(provider: ProviderConfig): Result<String> {
        return try {
            val msg = ChatMessage(role = "user", content = "Hi")
            val events = mutableListOf<LlmEvent>()
            stream(provider, "Reply with only: OK", listOf(msg)).collect { events.add(it) }
            val text = events.filterIsInstance<LlmEvent.TextDelta>().joinToString("") { it.text }
            if (text.isNotBlank()) Result.success(text.take(100))
            else {
                val error = events.filterIsInstance<LlmEvent.Error>().firstOrNull()
                Result.failure(Exception(error?.message ?: "未收到回复"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ================================================================ Request building

    private fun buildRequest(
        provider: ProviderConfig,
        systemPrompt: String,
        messages: List<ChatMessage>,
        tools: List<ToolDef>
    ): Request {
        val builder = Request.Builder()
        val apiKey = provider.effectiveApiKey()
        val jsonBody: String
        when (provider.type) {
            ProviderType.OPENAI, ProviderType.OPENROUTER, ProviderType.OLLAMA, ProviderType.CUSTOM -> {
                builder.url(provider.baseUrl.trimEnd('/') + "/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                provider.customHeaders.forEach { (k, v) -> builder.addHeader(k, v) }
                if (provider.type == ProviderType.OPENROUTER) {
                    builder.addHeader("HTTP-Referer", "https://mckai.app")
                    builder.addHeader("X-Title", "MCKAI")
                }
                val sysMsg = if (systemPrompt.isNotBlank()) listOf(OpenAiReqMessage(role = "system", content = JsonPrimitive(systemPrompt))) else emptyList()
                val openAiTools = if (tools.isNotEmpty()) tools.map { t ->
                    OpenAiReqTool(function = OpenAiFunctionDef(name = t.name, description = t.description, parameters = t.parameters))
                } else null
                jsonBody = json.encodeToString(
                    OpenAiRequest.serializer(),
                    OpenAiRequest(
                        model = provider.displayModel(),
                        messages = sysMsg + messages.map { toOpenAiMessage(it) },
                        temperature = if (isReasoningModel(provider.displayModel())) null else provider.temperature,
                        maxTokens = provider.maxTokens,
                        tools = openAiTools
                    )
                )
            }
            ProviderType.GEMINI -> {
                builder.url(provider.baseUrl.trimEnd('/') + "/v1beta/models/${provider.displayModel()}:streamGenerateContent?alt=sse")
                    .addHeader("x-goog-api-key", apiKey)
                    .addHeader("Content-Type", "application/json")
                val geminiTools = if (tools.isNotEmpty()) tools.map { t ->
                    GeminiReqTool(functionDeclarations = listOf(GeminiFunctionDef(name = t.name, description = t.description, parameters = t.parameters)))
                } else null
                val contents = toGeminiContents(messages)
                jsonBody = json.encodeToString(
                    GeminiRequest.serializer(),
                    GeminiRequest(
                        contents = contents,
                        systemInstruction = if (systemPrompt.isNotBlank()) GeminiReqContent(role = "user", parts = listOf(GeminiReqPart(text = systemPrompt))) else null,
                        generationConfig = GeminiConfig(temperature = provider.temperature, maxOutputTokens = provider.maxTokens),
                        tools = geminiTools
                    )
                )
            }
            ProviderType.CLAUDE -> {
                builder.url(provider.baseUrl.trimEnd('/') + "/v1/messages")
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("Content-Type", "application/json")
                provider.customHeaders.forEach { (k, v) -> builder.addHeader(k, v) }
                val claudeTools = if (tools.isNotEmpty()) tools.map { t ->
                    ClaudeReqTool(name = t.name, description = t.description, inputSchema = t.parameters)
                } else null
                jsonBody = json.encodeToString(
                    ClaudeRequest.serializer(),
                    ClaudeRequest(
                        model = provider.displayModel(),
                        maxTokens = provider.maxTokens,
                        system = systemPrompt.ifBlank { null },
                        messages = messages.map { toClaudeMessage(it) },
                        tools = claudeTools
                    )
                )
            }
        }
        builder.post(jsonBody.toRequestBody("application/json".toMediaType()))
        return builder.build()
    }

    // ================================================================ Message conversion

    private fun isReasoningModel(model: String): Boolean {
        val lower = model.lowercase()
        return lower.contains("o3") || lower.contains("o4") || lower.contains("deepseek-r1") ||
            lower.contains("qwq") || lower.contains("reasoner")
    }

    private fun toOpenAiMessage(m: ChatMessage): OpenAiReqMessage {
        val content: JsonElement = if (m.multimodalContent != null && m.multimodalContent.isNotEmpty()) {
            JsonArray(m.multimodalContent.map { c ->
                buildJsonObject {
                    put("type", c.type)
                    c.text?.let { put("text", it) }
                    c.imageUrl?.let { put("image_url", buildJsonObject { put("url", it.url); put("detail", it.detail) }) }
                    c.fileData?.let {
                        put("type", "file")
                        put("file", buildJsonObject { put("mime_type", it.mimeType); put("file_data", it.data) })
                    }
                }
            })
        } else if (m.content.isNotBlank()) {
            JsonPrimitive(m.content)
        } else {
            // 空内容消息（纯工具调用/工具结果），OpenAI 要求 content 为 null 时 role=tool 需要 tool_call_id
            JsonPrimitive("")
        }
        val toolCalls: JsonElement? = m.toolCalls?.let { calls ->
            JsonArray(calls.map { tc ->
                buildJsonObject {
                    put("id", tc.id)
                    put("type", JsonPrimitive("function"))
                    put("function", buildJsonObject {
                        put("name", tc.name)
                        put("arguments", tc.args)
                    })
                }
            })
        }
        return OpenAiReqMessage(role = m.role, content = content, toolCallId = m.toolCallId, toolCalls = toolCalls)
    }

    /** Gemini：assistant 的 toolCalls → functionCall parts；role=tool → functionResponse parts。 */
    private fun toGeminiContents(messages: List<ChatMessage>): List<GeminiReqContent> {
        // 预扫描 id→name（functionResponse 需要函数名，而 tool 消息只有 call id）
        val idToName = HashMap<String, String>()
        messages.forEach { m -> m.toolCalls?.forEach { tc -> idToName[tc.id] = tc.name } }

        val result = ArrayList<GeminiReqContent>(messages.size)
        for (m in messages) {
            val parts = mutableListOf<GeminiReqPart>()
            if (m.content.isNotBlank() && m.toolCalls == null) {
                parts.add(GeminiReqPart(text = m.content))
            }
            if (m.toolCalls != null) {
                for (tc in m.toolCalls) {
                    val args = try { Json.parseToJsonElement(tc.args) } catch (_: Exception) { JsonObject(emptyMap()) }
                    parts.add(GeminiReqPart(
                        functionCall = buildJsonObject {
                            put("name", tc.name)
                            put("args", args)
                        }
                    ))
                }
            }
            if (m.role == "tool") {
                val name = idToName[m.toolCallId] ?: "unknown_tool"
                parts.add(GeminiReqPart(
                    functionResponse = GemFunctionResponse(
                        name = name,
                        response = buildJsonObject { put("result", m.content) }
                    )
                ))
            }
            m.multimodalContent?.forEach { c ->
                c.imageUrl?.let {
                    val data = it.url.removePrefix("data:").substringBefore(";")
                    val b64 = it.url.substringAfter(",", "")
                    if (b64.isNotBlank() && !b64.startsWith("http")) {
                        val mime = if (data.contains("/")) data else "image/png"
                        parts.add(GeminiReqPart(inlineData = GeminiInlineData(mimeType = mime, data = b64)))
                    } else {
                        parts.add(GeminiReqPart(text = "[图片: ${it.url.take(100)}]"))
                    }
                }
                c.fileData?.let { fd ->
                    parts.add(GeminiReqPart(inlineData = GeminiInlineData(mimeType = fd.mimeType, data = fd.data)))
                }
            }
            if (parts.isEmpty()) parts.add(GeminiReqPart(text = ""))
            result.add(GeminiReqContent(role = if (m.role == "assistant") "model" else "user", parts = parts))
        }
        return result
    }

    private fun toClaudeMessage(m: ChatMessage): ClaudeReqMessage {
        val blocks = mutableListOf<JsonElement>()
        if (m.multimodalContent != null && m.multimodalContent.isNotEmpty()) {
            m.multimodalContent.forEach { c ->
                c.imageUrl?.let {
                    val b64 = it.url.substringAfter(",", "")
                    if (b64.isNotBlank() && !b64.startsWith("http")) {
                        val mime = it.url.removePrefix("data:").substringBefore(";").ifBlank { "image/png" }
                        blocks.add(buildJsonObject {
                            put("type", "image")
                            put("source", buildJsonObject {
                                put("type", "base64")
                                put("media_type", mime)
                                put("data", b64)
                            })
                        })
                    }
                }
                c.text?.let { blocks.add(buildJsonObject { put("type", "text"); put("text", it) }) }
            }
        } else {
            blocks.add(buildJsonObject { put("type", "text"); put("text", m.content) })
        }
        if (m.toolCalls != null) {
            for (tc in m.toolCalls) {
                blocks.add(buildJsonObject {
                    put("type", "tool_use")
                    put("id", tc.id)
                    put("name", tc.name)
                    put("input", try { Json.parseToJsonElement(tc.args) } catch (_: Exception) { buildJsonObject {} })
                })
            }
        }
        if (m.role == "tool") {
            blocks.add(buildJsonObject {
                put("type", "tool_result")
                put("tool_use_id", m.toolCallId ?: "")
                put("content", m.content)
            })
        }
        return ClaudeReqMessage(role = m.role, content = JsonArray(blocks))
    }

    // ================================================================ Payload parsing

    private fun parsePayload(type: ProviderType, data: String): List<LlmEvent> {
        return try {
            when (type) {
                ProviderType.OPENAI, ProviderType.OPENROUTER, ProviderType.OLLAMA, ProviderType.CUSTOM -> parseOpenAi(data)
                ProviderType.GEMINI -> parseGemini(data)
                ProviderType.CLAUDE -> parseClaude(data)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Parse error (${type.name}): ${e.message}, data=${data.take(200)}")
            emptyList()
        }
    }

    private fun parseOpenAi(data: String): List<LlmEvent> {
        if (data.trim() == "[DONE]") return listOf(LlmEvent.Done(null))
        val chunk = json.decodeFromString(OpenAiChunk.serializer(), data)
        val events = mutableListOf<LlmEvent>()
        for (choice in chunk.choices) {
            choice.delta.content?.let { if (it.isNotEmpty()) events.add(LlmEvent.TextDelta(it)) }
            choice.delta.reasoningContent?.let { if (it.isNotEmpty()) events.add(LlmEvent.ReasoningDelta(it)) }
            choice.delta.toolCalls?.forEach { tc ->
                events.add(LlmEvent.ToolCallDelta(
                    index = tc.index,
                    id = tc.id ?: nextToolCallId(),
                    name = tc.function?.name,
                    argsDelta = tc.function?.arguments ?: ""
                ))
            }
        }
        // 部分服务端在 [DONE] 前的最后 chunk 携带 usage
        if (chunk.usage != null) {
            events.add(LlmEvent.Done(Usage(
                promptTokens = chunk.usage.promptTokens,
                completionTokens = chunk.usage.completionTokens,
                totalTokens = chunk.usage.totalTokens
            )))
        }
        return events
    }

    private fun parseGemini(data: String): List<LlmEvent> {
        val chunk = json.decodeFromString(GeminiChunk.serializer(), data)
        val events = mutableListOf<LlmEvent>()
        var finished = false
        for (candidate in chunk.candidates) {
            if (!candidate.finishReason.isNullOrBlank() && candidate.finishReason != "SAFETY" && candidate.finishReason != "RECITATION") {
                finished = true
            }
            candidate.content?.parts?.forEach { part ->
                part.text?.let { if (it.isNotEmpty()) events.add(LlmEvent.TextDelta(it)) }
                part.functionCall?.let { fc ->
                    val name = fc.jsonObject["name"]?.jsonPrimitive?.content
                    val args = fc.jsonObject["args"]?.toString() ?: "{}"
                    events.add(LlmEvent.ToolCallDelta(index = 0, id = nextToolCallId("gemini_"), name = name, argsDelta = args))
                }
            }
        }
        if (finished) events.add(LlmEvent.Done(null))
        return events
    }

    private fun parseClaude(data: String): List<LlmEvent> {
        val chunk = json.decodeFromString(ClaudeChunk.serializer(), data)
        val events = mutableListOf<LlmEvent>()
        when (chunk.type) {
            "content_block_delta" -> {
                chunk.delta?.text?.let { if (it.isNotEmpty()) events.add(LlmEvent.TextDelta(it)) }
                chunk.delta?.partialJson?.let { if (it.isNotEmpty()) events.add(LlmEvent.ToolCallDelta(index = chunk.index, id = null, name = null, argsDelta = it)) }
            }
            "content_block_start" -> {
                chunk.contentBlock?.let { block ->
                    if (block.type == "tool_use") {
                        events.add(LlmEvent.ToolCallDelta(index = chunk.index, id = block.id ?: nextToolCallId("claude_"), name = block.name, argsDelta = ""))
                    }
                }
            }
            "message_delta", "message_stop" -> {
                events.add(LlmEvent.Done(null))
            }
        }
        return events
    }
}