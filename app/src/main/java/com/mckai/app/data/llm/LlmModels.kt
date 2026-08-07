package com.mckai.app.data.llm

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class MultimodalContent(
    val type: String,
    val text: String? = null,
    val imageUrl: ImageUrl? = null,
    val fileData: FileData? = null
) {
    @Serializable
    data class ImageUrl(val url: String, val detail: String = "auto")

    @Serializable
    data class FileData(val mimeType: String, val data: String, val fileName: String? = null)

    companion object {
        fun text(text: String) = MultimodalContent(type = "text", text = text)
        fun image(url: String, detail: String = "auto") =
            MultimodalContent(type = "image_url", imageUrl = ImageUrl(url, detail))
        fun imageBase64(mimeType: String, base64: String) =
            MultimodalContent(type = "image_url", imageUrl = ImageUrl("data:$mimeType;base64,$base64"))
        fun file(mimeType: String, data: String, fileName: String? = null) =
            MultimodalContent(type = "file", fileData = FileData(mimeType, data, fileName))
    }
}

data class ChatMessage(
    val role: String,
    val content: String = "",
    val multimodalContent: List<MultimodalContent>? = null,
    val toolCallId: String? = null,
    val toolCalls: List<ToolCallSpec>? = null
)

data class ToolCallSpec(
    val id: String,
    val name: String,
    val args: String
)

data class ToolDef(
    val name: String,
    val description: String,
    val parameters: JsonElement,
    val required: List<String> = emptyList()
)

sealed interface LlmEvent {
    data class TextDelta(val text: String) : LlmEvent
    data class ReasoningDelta(val text: String) : LlmEvent
    data class ToolCallDelta(val index: Int, val id: String?, val name: String?, val argsDelta: String) : LlmEvent
    data class Done(val usage: Usage?) : LlmEvent
    data class Error(val message: String) : LlmEvent
}

data class Usage(val promptTokens: Int = 0, val completionTokens: Int = 0, val totalTokens: Int = 0)
