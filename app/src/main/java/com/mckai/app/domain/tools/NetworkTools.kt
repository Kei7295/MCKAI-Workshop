package com.mckai.app.domain.tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

private val httpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .build()

/** 只允许 http/https，防止 file://、intent:// 等协议与内网探测被模型滥用。 */
private fun validateHttpUrl(raw: String): String? {
    val url = runCatching { java.net.URI(raw) }.getOrNull() ?: return "URL 格式无效：$raw"
    val scheme = url.scheme?.lowercase()
    if (scheme != "http" && scheme != "https") return "仅支持 http/https 协议：$raw"
    return null
}

fun registerNetworkTools(r: ToolRegistry) {
    r.register(ToolMetadata(
        name = "http_request",
        description = "发送 HTTP 请求并返回响应",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("url", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("请求 URL")) })
                put("method", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("HTTP 方法：GET/POST/PUT/DELETE，默认 GET")) })
                put("headers", buildJsonObject { put("type", JsonPrimitive("object")); put("description", JsonPrimitive("请求头 JSON")) })
                put("body", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("请求体")) })
            })
            put("required", buildJsonArray { add(JsonPrimitive("url")) })
        },
        category = "network"
    ), handler = { args ->
        val url = args["url"]?.jsonPrimitive?.content ?: return@register "请提供 url 参数"
        validateHttpUrl(url)?.let { return@register it }
        val method = args["method"]?.jsonPrimitive?.content?.uppercase() ?: "GET"
        val body = args["body"]?.jsonPrimitive?.content
        withContext(Dispatchers.IO) {
            try {
                val reqBuilder = Request.Builder().url(url)
                args["headers"]?.jsonObject?.forEach { (k, v) ->
                    reqBuilder.addHeader(k, v.jsonPrimitive.content)
                }
                when (method) {
                    "POST" -> reqBuilder.post(body?.toByteArray()?.toRequestBody("application/json; charset=utf-8".toMediaType()) ?: ByteArray(0).toRequestBody())
                    "PUT" -> reqBuilder.put(body?.toByteArray()?.toRequestBody("application/json; charset=utf-8".toMediaType()) ?: ByteArray(0).toRequestBody())
                    "DELETE" -> reqBuilder.delete(body?.toByteArray()?.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    else -> reqBuilder.get()
                }
                httpClient.newCall(reqBuilder.build()).execute().use { response ->
                    val text = response.body?.string() ?: "(空响应)"
                    val limited = if (text.length > 10000) text.take(10000) + "\n...(已截断)" else text
                    "HTTP ${response.code}\n$limited"
                }
            } catch (e: Exception) {
                "请求失败：${e.message}"
            }
        }
    })

    r.register(ToolMetadata(
        name = "fetch_url",
        description = "获取 URL 的内容（简单 GET 请求）",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("url", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("要获取的 URL")) })
            })
            put("required", buildJsonArray { add(JsonPrimitive("url")) })
        },
        category = "network"
    ), handler = { args ->
        val url = args["url"]?.jsonPrimitive?.content ?: return@register "请提供 url 参数"
        validateHttpUrl(url)?.let { return@register it }
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                httpClient.newCall(request).execute().use { response ->
                    val text = response.body?.string() ?: "(空响应)"
                    if (text.length > 15000) text.take(15000) + "\n...(已截断)" else text
                }
            } catch (e: Exception) {
                "获取失败：${e.message}"
            }
        }
    })

    r.register(ToolMetadata(
        name = "web_search",
        description = "搜索网页（通过 DuckDuckGo Lite）",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("query", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("搜索关键词")) })
                put("num_results", buildJsonObject { put("type", JsonPrimitive("integer")); put("description", JsonPrimitive("返回结果数量，默认5，最大10")) })
            })
            put("required", buildJsonArray { add(JsonPrimitive("query")) })
        },
        category = "network"
    ), handler = { args ->
        val query = args["query"]?.jsonPrimitive?.content ?: return@register "请提供 query 参数"
        val numResults = (args["num_results"]?.jsonPrimitive?.intOrNull ?: 5).coerceIn(1, 10)
        withContext(Dispatchers.IO) {
            try {
                val url = "https://lite.duckduckgo.com/lite/?q=${URLEncoder.encode(query, "UTF-8")}"
                val request = Request.Builder().url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) MCKAI/1.0")
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    val html = response.body?.string() ?: "(空响应)"
                    // Simple extraction of search results
                    val results = mutableListOf<String>()
                    val linkPattern = Regex("""<a[^>]+class="result-link"[^>]*>(.*?)</a>""")
                    val snippetPattern = Regex("""<td[^>]*class="result-snippet"[^>]*>(.*?)</td>""")
                    val links = linkPattern.findAll(html).map { it.groupValues[1].replace(Regex("<[^>]+>"), "") }.toList()
                    val snippets = snippetPattern.findAll(html).map { it.groupValues[1].replace(Regex("<[^>]+>"), "") }.toList()
                    for (i in links.indices.take(numResults)) {
                        results.add("${i + 1}. ${links[i]}")
                        if (i < snippets.size) results.add("   ${snippets[i]}")
                    }
                    if (results.isEmpty()) "未找到搜索结果"
                    else results.joinToString("\n")
                }
            } catch (e: Exception) {
                "搜索失败：${e.message}"
            }
        }
    })
}