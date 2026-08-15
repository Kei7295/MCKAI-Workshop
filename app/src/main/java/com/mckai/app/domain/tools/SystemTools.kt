package com.mckai.app.domain.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.BatteryManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.serialization.json.*

/**
 * 系统工具：剪贴板读写、通知、设备状态。
 * 对应 Operit 系统工具集，权限等级 ADMIN 的会在执行前弹确认（见 ChatViewModel）。
 */
fun registerSystemTools(r: ToolRegistry, context: Context) {
    val appContext = context.applicationContext

    r.register(ToolMetadata(
        name = "clipboard_read",
        description = "读取剪贴板内容",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {})
        },
        permission = ToolPermission.ADMIN,
        category = "system"
    ), handler = { _ ->
        val cm = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val text = cm?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
        text?.takeIf { it.isNotBlank() } ?: "（剪贴板为空）"
    })

    r.register(ToolMetadata(
        name = "clipboard_write",
        description = "写入文本到剪贴板",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("text", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("要写入剪贴板的文本")) })
            })
            put("required", buildJsonArray { add(JsonPrimitive("text")) })
        },
        permission = ToolPermission.ADMIN,
        category = "system"
    ), handler = { args ->
        val text = args["text"]?.jsonPrimitive?.content ?: return@register "请提供 text 参数"
        val cm = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("MCKAI", text))
        "已写入剪贴板（${text.length} 字符）"
    })

    r.register(ToolMetadata(
        name = "send_notification",
        description = "发送系统通知（标题+内容）",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {
                put("title", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("通知标题")) })
                put("content", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("通知内容")) })
            })
            put("required", buildJsonArray { add(JsonPrimitive("title")); add(JsonPrimitive("content")) })
        },
        category = "system"
    ), handler = { args ->
        val title = args["title"]?.jsonPrimitive?.content ?: "MCKAI"
        val content = args["content"]?.jsonPrimitive?.content ?: ""
        // Toast 必须在主线程 Looper 上调用（工具可能在 IO 线程执行）
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(appContext, "$title: $content", android.widget.Toast.LENGTH_LONG).show()
        }
        "通知已发送"
    })

    r.register(ToolMetadata(
        name = "get_device_status",
        description = "获取设备状态：电量、网络类型、屏幕亮度",
        parameters = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("properties", buildJsonObject {})
        },
        category = "system"
    ), handler = { _ ->
        buildString {
            val bm = appContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            appendLine("电量: ${level?.let { "$it%" } ?: "未知"}")
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val type = try {
                val network = cm?.activeNetwork
                val caps = network?.let { cm.getNetworkCapabilities(it) }
                when {
                    caps == null -> "无网络"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "蜂窝网络"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "以太网"
                    else -> "其他"
                }
            } catch (_: SecurityException) {
                "未知（无网络权限）"
            }
            appendLine("网络: $type")
            appendLine("设备: ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})")
        }.trim()
    })
}