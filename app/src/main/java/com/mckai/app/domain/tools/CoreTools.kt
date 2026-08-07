package com.mckai.app.domain.tools

import kotlinx.serialization.json.*
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

fun registerCoreTools(r: ToolRegistry) {
    r.register(ToolMetadata(
        name = "calculator",
        description = "计算数学表达式，支持加减乘除、括号、幂运算、三角函数等",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("expression", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("数学表达式，如 (3+5)*2 或 sin(3.14)"))
                })
            })
            put("required", buildJsonArray { add(JsonPrimitive("expression")) })
        },
        category = "core"
    ), handler = { args ->
        val expr = args["expression"]?.jsonPrimitive?.content ?: return@register "请提供 expression 参数"
        try {
            val result = evaluateExpression(expr)
            "$expr = $result"
        } catch (e: Exception) {
            "计算错误：${e.message}"
        }
    })

    r.register(ToolMetadata(
        name = "get_current_time",
        description = "获取当前日期和时间",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("format", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("日期格式，默认 yyyy-MM-dd HH:mm:ss"))
                })
            })
        },
        category = "core"
    ), handler = { args ->
        val format = args["format"]?.jsonPrimitive?.content ?: "yyyy-MM-dd HH:mm:ss"
        SimpleDateFormat(format, Locale.getDefault()).format(Date())
    })

    r.register(ToolMetadata(
        name = "generate_uuid",
        description = "生成 UUID",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {})
        },
        category = "core"
    ), handler = { _ ->
        UUID.randomUUID().toString()
    })

    r.register(ToolMetadata(
        name = "random_number",
        description = "生成指定范围内的随机数",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("min", buildJsonObject { put("type", JsonPrimitive("integer")); put("description", JsonPrimitive("最小值，默认0")) })
                put("max", buildJsonObject { put("type", JsonPrimitive("integer")); put("description", JsonPrimitive("最大值，默认100")) })
            })
        },
        category = "core"
    ), handler = { args ->
        val min = args["min"]?.jsonPrimitive?.int ?: 0
        val max = args["max"]?.jsonPrimitive?.int ?: 100
        Random.nextInt(min, max + 1).toString()
    })

    r.register(ToolMetadata(
        name = "format_json",
        description = "格式化 JSON 字符串，使其易于阅读",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("json", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("要格式化的 JSON 字符串")) })
            })
            put("required", buildJsonArray { add(JsonPrimitive("json")) })
        },
        category = "core"
    ), handler = { args ->
        val input = args["json"]?.jsonPrimitive?.content ?: return@register "请提供 json 参数"
        try {
            val obj = Json.parseToJsonElement(input)
            Json.encodeToString(JsonObject.serializer(), obj as? JsonObject ?: buildJsonObject { put("value", obj) })
        } catch (e: Exception) {
            "JSON 格式化失败：${e.message}"
        }
    })

    r.register(ToolMetadata(
        name = "encode_base64",
        description = "将文本编码为 Base64",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("text", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("要编码的文本")) })
            })
            put("required", buildJsonArray { add(JsonPrimitive("text")) })
        },
        category = "core"
    ), handler = { args ->
        val text = args["text"]?.jsonPrimitive?.content ?: return@register "请提供 text 参数"
        android.util.Base64.encodeToString(text.toByteArray(), android.util.Base64.NO_WRAP)
    })

    r.register(ToolMetadata(
        name = "decode_base64",
        description = "将 Base64 解码为文本",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("encoded", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Base64 编码的字符串")) })
            })
            put("required", buildJsonArray { add(JsonPrimitive("encoded")) })
        },
        category = "core"
    ), handler = { args ->
        val encoded = args["encoded"]?.jsonPrimitive?.content ?: return@register "请提供 encoded 参数"
        try {
            String(android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP))
        } catch (e: Exception) {
            "Base64 解码失败：${e.message}"
        }
    })

    r.register(ToolMetadata(
        name = "hash_text",
        description = "计算文本的哈希值（SHA-256 或 MD5）",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("text", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("要哈希的文本")) })
                put("algorithm", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("算法：SHA-256 或 MD5，默认 SHA-256")) })
            })
            put("required", buildJsonArray { add(JsonPrimitive("text")) })
        },
        category = "core"
    ), handler = { args ->
        val text = args["text"]?.jsonPrimitive?.content ?: return@register "请提供 text 参数"
        val algo = args["algorithm"]?.jsonPrimitive?.content ?: "SHA-256"
        val digest = MessageDigest.getInstance(algo)
        digest.digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
    })

    r.register(ToolMetadata(
        name = "url_encode",
        description = "URL 编码",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("text", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("要编码的文本")) })
            })
            put("required", buildJsonArray { add(JsonPrimitive("text")) })
        },
        category = "core"
    ), handler = { args ->
        val text = args["text"]?.jsonPrimitive?.content ?: return@register "请提供 text 参数"
        java.net.URLEncoder.encode(text, "UTF-8")
    })

    r.register(ToolMetadata(
        name = "url_decode",
        description = "URL 解码",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("text", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("要解码的文本")) })
            })
            put("required", buildJsonArray { add(JsonPrimitive("text")) })
        },
        category = "core"
    ), handler = { args ->
        val text = args["text"]?.jsonPrimitive?.content ?: return@register "请提供 text 参数"
        java.net.URLDecoder.decode(text, "UTF-8")
    })

    r.register(ToolMetadata(
        name = "get_system_info",
        description = "获取设备系统信息（型号、系统版本、可用内存等）",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {})
        },
        category = "system"
    ), handler = { _ ->
        buildString {
            appendLine("型号: ${android.os.Build.MODEL}")
            appendLine("品牌: ${android.os.Build.BRAND}")
            appendLine("系统版本: Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            appendLine("设备: ${android.os.Build.DEVICE}")
        }.trim()
    })
}

private fun evaluateExpression(expr: String): Double {
    val tokens = tokenize(expr)
    val (result, _) = parseExpr(tokens, 0)
    return result
}

private fun tokenize(expr: String): List<String> {
    val tokens = mutableListOf<String>()
    var i = 0
    while (i < expr.length) {
        when {
            expr[i].isWhitespace() -> i++
            expr[i] in "+-*/^()," -> { tokens.add(expr[i].toString()); i++ }
            expr[i].isDigit() || expr[i] == '.' -> {
                val start = i
                while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) i++
                tokens.add(expr.substring(start, i))
            }
            expr[i].isLetter() -> {
                val start = i
                while (i < expr.length && expr[i].isLetter()) i++
                tokens.add(expr.substring(start, i))
            }
            else -> i++
        }
    }
    return tokens
}

private fun parseExpr(tokens: List<String>, pos: Int): Pair<Double, Int> {
    var (left, p) = parseTerm(tokens, pos)
    while (p < tokens.size && tokens[p] in listOf("+", "-")) {
        val op = tokens[p]
        val (right, p2) = parseTerm(tokens, p + 1)
        left = if (op == "+") left + right else left - right
        p = p2
    }
    return left to p
}

private fun parseTerm(tokens: List<String>, pos: Int): Pair<Double, Int> {
    var (left, p) = parsePower(tokens, pos)
    while (p < tokens.size && tokens[p] in listOf("*", "/")) {
        val op = tokens[p]
        val (right, p2) = parsePower(tokens, p + 1)
        left = if (op == "*") left * right else left / right
        p = p2
    }
    return left to p
}

private fun parsePower(tokens: List<String>, pos: Int): Pair<Double, Int> {
    val (base, p) = parseUnary(tokens, pos)
    if (p < tokens.size && tokens[p] == "^") {
        val (exp, p2) = parseUnary(tokens, p + 1)
        return Math.pow(base, exp) to p2
    }
    return base to p
}

private fun parseUnary(tokens: List<String>, pos: Int): Pair<Double, Int> {
    if (pos < tokens.size && tokens[pos] == "-") {
        val (v, p) = parseAtom(tokens, pos + 1)
        return -v to p
    }
    return parseAtom(tokens, pos)
}

private fun parseAtom(tokens: List<String>, pos: Int): Pair<Double, Int> {
    if (pos >= tokens.size) return 0.0 to pos
    val token = tokens[pos]
    when {
        token == "(" -> {
            val (v, p) = parseExpr(tokens, pos + 1)
            return v to (if (p < tokens.size && tokens[p] == ")") p + 1 else p)
        }
        token.toDoubleOrNull() != null -> return token.toDouble() to pos + 1
        token == "sin" || token == "cos" || token == "tan" || token == "sqrt" || token == "ln" || token == "log" -> {
            val (arg, p) = parseAtom(tokens, pos + 1)
            val v = when (token) {
                "sin" -> Math.sin(Math.toRadians(arg))
                "cos" -> Math.cos(Math.toRadians(arg))
                "tan" -> Math.tan(Math.toRadians(arg))
                "sqrt" -> Math.sqrt(arg)
                "ln" -> Math.log(arg)
                "log" -> Math.log10(arg)
                else -> arg
            }
            return v to p
        }
        token == "pi" -> return Math.PI to pos + 1
        token == "e" -> return Math.E to pos + 1
    }
    return 0.0 to pos + 1
}
