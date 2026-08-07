package com.mckai.app.data.llm

import kotlinx.serialization.Serializable

enum class ProviderType(val label: String, val defaultBaseUrl: String, val supportsTools: Boolean) {
    OPENAI("OpenAI", "https://api.openai.com/v1", true),
    GEMINI("Gemini", "https://generativelanguage.googleapis.com", true),
    CLAUDE("Claude", "https://api.anthropic.com", true),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1", true),
    OLLAMA("Ollama (本地)", "http://localhost:11434/v1", true),
    CUSTOM("自定义 OpenAI 兼容", "", true);

    val docHint: String get() = when (this) {
        OPENAI -> "支持 GPT-4o, GPT-4.1, o3 等模型"
        GEMINI -> "支持 Gemini 2.5 Pro/Flash 等模型"
        CLAUDE -> "支持 Claude Opus 4, Sonnet 4 等模型"
        OPENROUTER -> "统一网关，支持 200+ 模型"
        OLLAMA -> "本地部署，无需 API Key"
        CUSTOM -> "任何 OpenAI 兼容 API（如 vLLM, LM Studio）"
    }
}

data class ProviderConfig(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val type: ProviderType = ProviderType.OPENAI,
    val baseUrl: String = type.defaultBaseUrl,
    val apiKey: String = "",
    val models: List<String> = emptyList(),
    val defaultModel: String = "",
    val enabled: Boolean = true,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 4096,
    val customHeaders: Map<String, String> = emptyMap(),
    val apiKeys: List<String> = emptyList(),
    val currentKeyIndex: Int = 0
) {
    fun displayModel(): String = defaultModel.ifBlank { models.firstOrNull() ?: "" }
    fun effectiveApiKey(): String = if (apiKeys.isNotEmpty()) apiKeys[currentKeyIndex % apiKeys.size] else apiKey
    fun nextApiKey(): String {
        if (apiKeys.isEmpty()) return apiKey
        val next = (currentKeyIndex + 1) % apiKeys.size
        return apiKeys[next]
    }
}

object ProviderPresets {
    fun builtIn(): List<ProviderConfig> = listOf(
        ProviderConfig(
            id = "openai", name = "OpenAI",
            type = ProviderType.OPENAI,
            models = listOf("gpt-4.1", "gpt-4.1-mini", "gpt-4o", "gpt-4o-mini", "o3", "o4-mini"),
            defaultModel = "gpt-4.1"
        ),
        ProviderConfig(
            id = "deepseek", name = "DeepSeek",
            type = ProviderType.CUSTOM,
            baseUrl = "https://api.deepseek.com/v1",
            models = listOf("deepseek-chat", "deepseek-reasoner"),
            defaultModel = "deepseek-chat"
        ),
        ProviderConfig(
            id = "qwen", name = "通义千问",
            type = ProviderType.CUSTOM,
            baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
            models = listOf("qwen-max", "qwen-plus", "qwen-turbo", "qwen-long"),
            defaultModel = "qwen-max"
        ),
        ProviderConfig(
            id = "kimi", name = "Kimi (月之暗面)",
            type = ProviderType.CUSTOM,
            baseUrl = "https://api.moonshot.cn/v1",
            models = listOf("moonshot-v1-auto", "moonshot-v1-32k", "moonshot-v1-128k"),
            defaultModel = "moonshot-v1-auto"
        ),
        ProviderConfig(
            id = "doubao", name = "豆包",
            type = ProviderType.CUSTOM,
            baseUrl = "https://ark.cn-beijing.volces.com/api/v3",
            models = listOf("doubao-pro-256k", "doubao-pro-32k", "doubao-lite-32k"),
            defaultModel = "doubao-pro-256k"
        ),
        ProviderConfig(
            id = "ollama", name = "Ollama 本地",
            type = ProviderType.OLLAMA,
            baseUrl = "http://localhost:11434/v1",
            apiKey = "ollama",
            models = listOf("llama3.1", "qwen2.5", "deepseek-r1", "codellama", "mistral"),
            defaultModel = "llama3.1"
        ),
        ProviderConfig(
            id = "gemini", name = "Gemini",
            type = ProviderType.GEMINI,
            models = listOf("gemini-2.5-pro", "gemini-2.5-flash", "gemini-2.0-flash"),
            defaultModel = "gemini-2.5-flash"
        ),
        ProviderConfig(
            id = "claude", name = "Claude",
            type = ProviderType.CLAUDE,
            models = listOf("claude-opus-4-20250514", "claude-sonnet-4-20250514", "claude-3-5-haiku-20241022"),
            defaultModel = "claude-sonnet-4-20250514"
        ),
        ProviderConfig(
            id = "openrouter", name = "OpenRouter",
            type = ProviderType.OPENROUTER,
            baseUrl = "https://openrouter.ai/api/v1",
            models = listOf("openai/gpt-4.1", "anthropic/claude-sonnet-4", "google/gemini-2.5-flash", "deepseek/deepseek-chat"),
            defaultModel = "openai/gpt-4.1"
        )
    )
}
