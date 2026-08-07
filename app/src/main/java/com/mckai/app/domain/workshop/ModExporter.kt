package com.mckai.app.domain.workshop

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ModExporter {
    fun exportZip(spec: ModSpec, files: Map<String, String>): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zos ->
            // Add scaffolding files
            val scaffolding = generateScaffolding(spec)
            (files + scaffolding).forEach { (path, content) ->
                zos.putNextEntry(ZipEntry(path))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }
        return bos.toByteArray()
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
            |  "id": "$modId",
            |  "version": "1.0.0",
            |  "name": "${spec.name}",
            |  "description": "${spec.description}",
            |  "authors": ["${spec.author}"],
            |  "contact": {},
            |  "license": "MIT",
            |  "icon": "assets/$modId/icon.png",
            |  "environment": "client",
            |  "entrypoints": {
            |    "client": ["${spec.packageName}.${modId.replace("_", "").replace("-", "")}Client"]
            |  },
            |  "depends": {
            |    "fabricloader": ">=0.16.0",
            |    "fabric-api": "*",
            |    "minecraft": "${spec.mcVersion}"
            |  }
            |}
        """.trimMargin()
    }

    private fun bedrockManifest(spec: ModSpec) = """
        |{
        |  "format_version": 2,
        |  "header": {
        |    "name": "${spec.name}",
        |    "description": "${spec.description}",
        |    "uuid": "${java.util.UUID.randomUUID()}",
        |    "version": [1, 0, 0],
        |    "min_engine_version": [1, 21, 0]
        |  },
        |  "modules": [
        |    {
        |      "type": "data",
        |      "uuid": "${java.util.UUID.randomUUID()}",
        |      "version": [1, 0, 0]
        |    }
        |  ]
        |}
    """.trimMargin()

    private fun bedrockPackManifest(spec: ModSpec) = bedrockManifest(spec).replace("data", "resources")

    private fun neteaseModJson(spec: ModSpec) = """
        |{
        |  "format_version": 1,
        |  "name": "${spec.name}",
        |  "description": "${spec.description}",
        |  "version": "1.0.0",
        |  "entry": "main.py"
        |}
    """.trimMargin()
}
