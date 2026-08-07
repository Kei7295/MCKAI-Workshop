package com.mckai.app.data.llm

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.catch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class LlmClient(private val client: OkHttpClient) {

    companion object {
        private const val TAG = "LlmClient"
        fun newDefaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        fun truncate(s: String, max: Int = 800): String =
            if (s.length <= max) s else s.take(max) + "..."

        fun fixJson(s: String): String {
            var t = s.trim()
            if (t.isEmpty()) return "{}"
            // Fix trailing comma before }
            t = t.replace(Regex(",\\s*}"), "}")
            // Fix trailing comma before ]
            t = t.replace(Regex(",\\s*]"), "]")
            // Try to find the JSON object/array
            val firstBrace = t.indexOfFirst { it == '{' || it == '[' }
            if (firstBrace > 0) t = t.substring(firstBrace)
            // Try to close unclosed braces
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
    private data class OpenAiChunk(val choices: List<OpenAiChoice> = emptyList())

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
    private data class GeminiReqPart(val text: String? = null, @SerialName("inlineData") val inlineData: GeminiInlineData? = null)
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
        client.newCall(request).execute().use { response ->
            Log.d(TAG, "Response: HTTP ${response.code}, CT=${response.header("Content-Type")}")
            val body = response.body ?: throw IOException("空的响应体")
            if (!response.isSuccessful) {
                val errText = try { body.string().take(400) } catch (_: Exception) { "" }
                throw IOException("请求失败 (HTTP ${response.code})：$errText")
            }
            val parser = SseParser(body.source())
            var eventCount = 0
            while (true) {
                val data = parser.nextEvent() ?: break
                if (data.isBlank()) continue
                val events = parsePayload(provider.type, data)
                events.forEach { emit(it); eventCount++ }
            }
            Log.d(TAG, "Stream complete: $eventCount events")
            if (eventCount == 0) emit(LlmEvent.Done(null))
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
        val content: JsonElement = if (m.multimodalContent != null && m.multimodalContent.size > 1) {
            JsonArray(m.multimodalContent.map { c ->
                buildJsonObject {
                    put("type", c.type)
                    c.text?.let { put("text", it) }
                    c.imageUrl?.let { put("image_url", buildJsonObject { put("url", it.url); put("detail", it.detail) }) }
                }
            })
        } else {
            JsonPrimitive(m.content)
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

    private fun toGeminiContents(messages: List<ChatMessage>): List<GeminiReqContent> {
        return messages.map { m ->
            val parts = mutableListOf<GeminiReqPart>()
            if (m.content.isNotBlank()) parts.add(GeminiReqPart(text = m.content))
            m.multimodalContent?.forEach { c ->
                c.imageUrl?.let { parts.add(GeminiReqPart(text = "[图片: ${it.url.take(100)}]")) }
                c.fileData?.let { parts.add(GeminiReqPart(text = "[文件: ${it.fileName ?: it.mimeType}]")) }
            }
            GeminiReqContent(role = if (m.role == "assistant") "model" else "user", parts = parts)
        }
    }

    private fun toClaudeMessage(m: ChatMessage): ClaudeReqMessage {
        val content: JsonElement = if (m.multimodalContent != null && m.multimodalContent.size > 1) {
            JsonArray(m.multimodalContent.map { c ->
                buildJsonObject {
                    put("type", JsonPrimitive("text"))
                    put("text", JsonPrimitive(c.text ?: "[附件]"))
                }
            })
        } else {
            JsonArray(listOf(buildJsonObject {
                put("type", JsonPrimitive("text"))
                put("text", JsonPrimitive(m.content))
            }))
        }
        return ClaudeReqMessage(role = m.role, content = content)
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
                    id = tc.id,
                    name = tc.function?.name,
                    argsDelta = tc.function?.arguments ?: ""
                ))
            }
        }
        return events
    }

    private fun parseGemini(data: String): List<LlmEvent> {
        val chunk = json.decodeFromString(GeminiChunk.serializer(), data)
        val events = mutableListOf<LlmEvent>()
        for (candidate in chunk.candidates) {
            candidate.content?.parts?.forEach { part ->
                part.text?.let { if (it.isNotEmpty()) events.add(LlmEvent.TextDelta(it)) }
                part.functionCall?.let { fc ->
                    val name = fc.jsonObject["name"]?.jsonPrimitive?.content
                    val args = fc.jsonObject["args"]?.toString() ?: "{}"
                    events.add(LlmEvent.ToolCallDelta(index = 0, id = "gemini_${System.currentTimeMillis()}", name = name, argsDelta = args))
                }
            }
        }
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
                        events.add(LlmEvent.ToolCallDelta(index = chunk.index, id = block.id, name = block.name, argsDelta = ""))
                    }
                }
            }
            "message_delta" -> {
                events.add(LlmEvent.Done(null))
            }
        }
        return events
    }
}
