package com.mckai.app.domain.workshop

enum class ModEdition(
    val label: String,
    val platform: String,
    val defaultMcVersion: String,
    val description: String
) {
    JAVA_FABRIC("Java Fabric", "fabric", "1.21.4", "Fabric 轻量级模组加载器"),
    JAVA_FORGE("Java Forge", "forge", "1.20.1", "Forge 经典模组加载器"),
    JAVA_NEOFORGE("Java NeoForge", "neoforge", "1.21.1", "NeoForge 新一代 Forge"),
    BEDROCK("Bedrock 基岩版", "bedrock", "1.21", "基岩版 Add-on/Behavior Pack"),
    NETEASE("NetEase 网易版", "netease", "1.20.10", "网易中国版模组");

    companion object {
        fun default() = JAVA_FABRIC
    }
}

data class ModSpec(
    val name: String,
    val edition: ModEdition,
    val mcVersion: String = edition.defaultMcVersion,
    val description: String = "",
    val features: String = "",
    val modId: String = "",
    val packageName: String = "com.example.mod",
    val author: String = "MCKAI"
)

enum class AgentPhase { PLANNING, GENERATING, REVIEWING, DONE, FAILED }

data class AgentProgress(
    val phase: AgentPhase,
    val message: String,
    val progress: Float = 0f,
    val filesGenerated: Int = 0,
    val totalFiles: Int = 0
)

sealed interface AgentResult {
    data class Success(val files: Map<String, String>, val summary: String) : AgentResult
    data class Failure(val error: String) : AgentResult
}
