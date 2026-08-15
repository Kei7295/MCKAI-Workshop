package com.mckai.app.domain.workshop

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ModExporter {

    /** 最小 1x1 透明 PNG，避免空文件被 Minecraft 判为损坏资源。 */
    private val TINY_PNG = byteArrayOf(
        0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0D.toByte(), 0x49.toByte(), 0x48.toByte(), 0x44.toByte(), 0x52.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(),
        0x08.toByte(), 0x06.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x1F.toByte(), 0x15.toByte(), 0xC4.toByte(),
        0x89.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0A.toByte(), 0x49.toByte(), 0x44.toByte(), 0x41.toByte(),
        0x54.toByte(), 0x78.toByte(), 0x9C.toByte(), 0x63.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x05.toByte(), 0x00.toByte(), 0x01.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x2D.toByte(), 0xB4.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x49.toByte(), 0x45.toByte(), 0x4E.toByte(), 0x44.toByte(),
        0xAE.toByte(), 0x42.toByte(), 0x60.toByte(), 0x82.toByte()
    )

    fun exportZip(spec: ModSpec, files: Map<String, String>): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zos ->
            // Add scaffolding files
            val scaffolding = generateScaffolding(spec)
            var skipped = 0
            for ((path, content) in files + scaffolding) {
                val entryName = sanitizeEntryPath(path, spec.name)
                if (entryName == null) {
                    skipped++
                    continue
                }
                zos.putNextEntry(ZipEntry(entryName))
                if (entryName.endsWith(".png") && content.isEmpty()) {
                    zos.write(TINY_PNG)
                } else {
                    zos.write(content.toByteArray(Charsets.UTF_8))
                }
                zos.closeEntry()
            }
            if (skipped > 0) {
                zos.putNextEntry(ZipEntry("_skipped_entries.txt"))
                zos.write("因路径非法跳过的条目数：$skipped（LLM 输出含 ../ 或绝对路径）".toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }
        return bos.toByteArray()
    }

    /** 路径穿越防护：规范化相对路径，拒绝 .. 与绝对路径。 */
    private fun sanitizeEntryPath(path: String, fallbackName: String): String? {
        val normalized = path.replace('\\', '/').trim('/')
        if (normalized.isEmpty()) return null
        val parts = normalized.split('/')
        if (parts.any { it == ".." || it == "." }) return null
        if (normalized.startsWith("/")) return null
        return normalized
    }

    private fun generateScaffolding(spec: ModSpec): Map<String, String> = when (spec.edition) {
        ModEdition.JAVA_FABRIC -> mapOf(
            "gradle.properties" to fabricGradleProperties(spec),
            "build.gradle" to fabricBuildGradle(spec),
            "settings.gradle" to "rootProject.name = \"${spec.name.lowercase().replace(" ", "-")}\"\n",
            "src/main/resources/fabric.mod.json" to fabricModJson(spec),
            "src/main/resources/assets/${spec.modId.ifBlank { spec.name.lowercase().replace(" ", "_") }}/icon.png" to ""
        )
        ModEdition.JAVA_FORGE -> mapOf(
            "gradle.properties" to "org.gradle.jvmargs=-Xmx3G\nminecraft_version=1.20.1\nforge_version=47.3.0\n",
            "build.gradle" to "// Forge build.gradle\nplugins { id 'net.minecraftforge.gradle' version '[6.0.16,6.2)' }\n",
            "src/main/resources/META-INF/mods.toml" to "modLoader=\"javafml\"\nloaderVersion=\"[47,)\"\n"
        )
        ModEdition.JAVA_NEOFORGE -> mapOf(
            "gradle.properties" to "org.gradle.jvmargs=-Xmx3G\nminecraft_version=1.21.1\nneoforge_version=21.1.0\n",
            "build.gradle" to "// NeoForge build.gradle\n"
        )
        ModEdition.BEDROCK -> mapOf(
            "manifest.json" to bedrockManifest(spec),
            "pack_manifest.json" to bedrockPackManifest(spec)
        )
        ModEdition.NETEASE -> mapOf(
            "mod.json" to neteaseModJson(spec),
            "main.py" to "# NetEase mod entry point\nprint('Mod loaded')\n"
        )
    }

    /** 所有写进 JSON 的字符串统一转义，防止名称含引号/换行产出损坏 JSON。 */
    private fun jstr(s: String): String = buildString {
        append('"')
        for (c in s) {
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '\b' -> append("\\b")
                else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
            }
        }
        append('"')
    }

    private fun fabricGradleProperties(spec: ModSpec) = """
        |org.gradle.jvmargs=-Xmx2G
        |minecraft_version=${spec.mcVersion}
        |yarn_mappings=${spec.mcVersion}+build.1
        |loader_version=0.16.10
        |fabric_version=0.116.0+${spec.mcVersion}
        |loom_version=1.17.12
    """.trimMargin()

    private fun fabricBuildGradle(spec: ModSpec) = """
        |plugins {
        |    id 'fabric-loom' version '${'$'}loom_version'
        |    id 'maven-publish'
        |}
        |version = "1.0.0"
        |group = "${spec.packageName}"
        |repositories { }
        |dependencies {
        |    minecraft "com.mojang:minecraft:${'$'}minecraft_version"
        |    mappings "net.fabricmc:yarn:${'$'}yarn_mappings:v2"
        |    modImplementation "net.fabricmc:fabric-loader:${'$'}loader_version"
        |    modImplementation "net.fabricmc.fabric-api:fabric-api:${'$'}fabric_version"
        |}
    """.trimMargin()

    private fun fabricModJson(spec: ModSpec): String {
        val modId = spec.modId.ifBlank { spec.name.lowercase().replace(" ", "_") }
        return """
            |{
            |  "schemaVersion": 1,
            |  "id": ${jstr(modId)},
            |  "version": "1.0.0",
            |  "name": ${jstr(spec.name)},
            |  "description": ${jstr(spec.description)},
            |  "authors": [${jstr(spec.author)}],
            |  "contact": {},
            |  "license": "MIT",
            |  "icon": "assets/$modId/icon.png",
            |  "environment": "client",
            |  "entrypoints": {
            |    "client": [${jstr(spec.packageName + "." + modId.replace("_", "").replace("-", "") + "Client")}]
            |  },
            |  "depends": {
            |    "fabricloader": ">=0.16.0",
            |    "fabric-api": "*",
            |    "minecraft": ${jstr(spec.mcVersion)}
            |  }
            |}
        """.trimMargin()
    }

    private fun bedrockManifest(spec: ModSpec, moduleType: String = "data") = """
        |{
        |  "format_version": 2,
        |  "header": {
        |    "name": ${jstr(spec.name)},
        |    "description": ${jstr(spec.description)},
        |    "uuid": "${java.util.UUID.randomUUID()}",
        |    "version": [1, 0, 0],
        |    "min_engine_version": [1, 21, 0]
        |  },
        |  "modules": [
        |    {
        |      "type": ${jstr(moduleType)},
        |      "uuid": "${java.util.UUID.randomUUID()}",
        |      "version": [1, 0, 0]
        |    }
        |  ]
        |}
    """.trimMargin()

    private fun bedrockPackManifest(spec: ModSpec) = bedrockManifest(spec, moduleType = "resources")

    private fun neteaseModJson(spec: ModSpec) = """
        |{
        |  "format_version": 1,
        |  "name": ${jstr(spec.name)},
        |  "description": ${jstr(spec.description)},
        |  "version": "1.0.0",
        |  "entry": "main.py"
        |}
    """.trimMargin()
}