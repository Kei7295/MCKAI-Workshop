package com.mckai.app.domain.workshop

import com.mckai.app.data.llm.ToolDef
import kotlinx.serialization.json.*

object AgentPrompts {
    const val COMMON_RULES = """
## 规则
1. 只输出文件内容，不要解释
2. 用 <<<FILE path="relative/path">> 和 <<<END>>> 包裹每个文件
3. 生成完整可编译的代码，不要省略
4. 使用标准 Minecraft/Fabric API
5. 所有文件用 UTF-8 编码
6. 最后输出 <<<SUMMARY>>> 块总结做了什么
"""

    fun platformInstructions(spec: ModSpec): String = when (spec.edition) {
        ModEdition.JAVA_FABRIC -> """
            |平台：Fabric
            |MC 版本：${spec.mcVersion}
            |需要的文件：
            |- build.gradle (使用 fabric-loom)
            |- gradle.properties
            |- settings.gradle
            |- src/main/resources/fabric.mod.json
            |- src/main/java/.../${spec.name.replace(" ", "")}.java (主类，实现 ClientModInitializer)
            |- 其他功能文件
            |默认包名：${spec.packageName}
            |Mod ID：${spec.modId.ifBlank { spec.name.lowercase().replace(" ", "_") }}
        """.trimMargin()
        ModEdition.JAVA_FORGE -> "平台：Forge | MC 版本：${spec.mcVersion} | 主类用 @Mod 注解"
        ModEdition.JAVA_NEOFORGE -> "平台：NeoForge | MC 版本：${spec.mcVersion} | 使用 @EventBusSubscriber"
        ModEdition.BEDROCK -> "平台：Bedrock | 版本：${spec.mcVersion} | 生成 manifest.json + 脚本"
        ModEdition.NETEASE -> "平台：网易 | 版本：${spec.mcVersion} | 生成 mod.json + Python 入口"
    }

    fun systemPrompt(spec: ModSpec): String = """
        |你是一个专业的 Minecraft 模组开发 AI。根据用户需求生成完整的模组项目文件。
        |
        |${COMMON_RULES}
        |
        |${platformInstructions(spec)}
        |
        |模组信息：
        |名称：${spec.name}
        |描述：${spec.description}
        |功能：${spec.features}
    """.trimMargin()

    fun initialUserPrompt(spec: ModSpec): String =
        "请为我的 Minecraft 模组生成完整项目文件。\n\n模组名称：${spec.name}\n平台：${spec.edition.label}\n版本：${spec.mcVersion}\n描述：${spec.description}\n功能需求：${spec.features}"

    fun nonToolSystemPrompt(spec: ModSpec): String = """
        |你是一个专业的 Minecraft 模组开发 AI。请按以下格式输出文件：
        |
        |<<<FILE path="相对路径">
        |文件内容
        |<<<END>>>
        |
        |<<<FILE path="另一个文件路径">
        |另一个文件内容
        |<<<END>>>
        |
        |<<<SUMMARY>
        |总结说明
        |<<<END>>>
        |
        |${platformInstructions(spec)}
    """.trimMargin()

    fun reviewPrompt(): String =
        "请审查以下生成的模组文件，指出问题并给出修复建议。如果没问题请说 'LGTM'。"

    fun generationTools(): List<ToolDef> = listOf(
        ToolDef(
            name = "write_file",
            description = "写入一个文件到项目",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                put("properties", buildJsonObject {
                    put("path", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("文件相对路径")) })
                    put("content", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("文件内容")) })
                })
                put("required", buildJsonArray { add(JsonPrimitive("path")); add(JsonPrimitive("content")) })
            }
        ),
        ToolDef(
            name = "complete",
            description = "声明所有文件已生成完毕",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                put("properties", buildJsonObject {
                    put("summary", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("项目总结")) })
                })
                put("required", buildJsonArray { add(JsonPrimitive("summary")) })
            }
        )
    )

    fun reviewTools(): List<ToolDef> = listOf(
        ToolDef(
            name = "approve",
            description = "批准生成的文件",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                put("properties", buildJsonObject {
                    put("comments", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("审查意见")) })
                })
            }
        ),
        ToolDef(
            name = "suggest_fix",
            description = "建议修复方案",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                put("properties", buildJsonObject {
                    put("file", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("文件路径")) })
                    put("issue", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("问题描述")) })
                    put("fix", buildJsonObject { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("修复方案")) })
                })
                put("required", buildJsonArray { add(JsonPrimitive("file")); add(JsonPrimitive("issue")); add(JsonPrimitive("fix")) })
            }
        )
    )

    fun parseFileBlocks(text: String): Map<String, String> {
        val files = mutableMapOf<String, String>()
        val pattern = Regex("""<<<FILE path="(.+?)">>(.*?)<<<END>>>""", RegexOption.DOT_MATCHES_ALL)
        pattern.findAll(text).forEach { match ->
            files[match.groupValues[1]] = match.groupValues[2].trim()
        }
        return files
    }

    fun parseSummary(text: String): String {
        val pattern = Regex("""<<<SUMMARY>>>(.*?)<<<END>>>""", RegexOption.DOT_MATCHES_ALL)
        return pattern.find(text)?.groupValues?.get(1)?.trim() ?: text.take(200)
    }
}
