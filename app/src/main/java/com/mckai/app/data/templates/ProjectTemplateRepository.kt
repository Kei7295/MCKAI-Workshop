package com.mckai.app.data.templates

import android.content.Context
import com.mckai.app.data.db.AppDatabase
import com.mckai.app.data.db.entity.ProjectEntity
import com.mckai.app.data.db.entity.ProjectFileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * 项目模板仓库（移植自 Operit WorkspaceUtils.copyTemplateFiles 的思路）：
 * assets/templates/ 下的 9 种项目骨架（android/flutter/go/java/node/office/python/typescript/web），
 * 创建项目时递归复制进 project_files 表。
 */
class ProjectTemplateRepository(
    private val context: Context,
    private val db: AppDatabase
) {
    data class TemplateMeta(val id: String, val label: String, val fileCount: Int)

    fun listTemplates(): List<TemplateMeta> = runCatching {
        context.assets.list(TEMPLATES_ROOT)?.filter { it.isNotBlank() }?.map { dir ->
            TemplateMeta(dir, labelOf(dir), countFiles(dir))
        } ?: emptyList()
    }.getOrDefault(emptyList())

    private fun countFiles(dir: String): Int {
        var count = 0
        fun walk(path: String) {
            context.assets.list(path)?.forEach { name ->
                val full = "$path/$name"
                if (context.assets.list(full)?.isNotEmpty() == true) walk(full) else count++
            }
        }
        walk("$TEMPLATES_ROOT/$dir")
        return count
    }

    suspend fun createProjectFromTemplate(templateId: String, projectName: String): Long =
        withContext(Dispatchers.IO) {
            val projectId = db.projectDao().insert(
                ProjectEntity(
                    name = projectName,
                    edition = "Template",
                    mcVersion = "",
                    description = "从 $templateId 模板创建",
                    modId = projectName.lowercase().replace(Regex("[^a-z0-9_]"), "_"),
                    packageName = null,
                    author = null
                )
            )
            val files = mutableListOf<ProjectFileEntity>()
            fun walk(path: String) {
                context.assets.list(path)?.forEach { name ->
                    val full = "$path/$name"
                    if (context.assets.list(full)?.isNotEmpty() == true) {
                        walk(full)
                    } else {
                        val content = runCatching {
                            context.assets.open(full).bufferedReader(Charsets.UTF_8).use { it.readText() }
                        }.getOrElse { "" }
                        val relative = full.removePrefix("$TEMPLATES_ROOT/")
                        files += ProjectFileEntity(
                            projectId = projectId,
                            filePath = relative,
                            fileName = name,
                            content = content,
                            isGenerated = true
                        )
                    }
                }
            }
            walk("$TEMPLATES_ROOT/$templateId")
            if (files.isNotEmpty()) {
                db.projectFileDao().insertAll(files)
            }
            projectId
        }

    private fun labelOf(id: String): String = when (id) {
        "android" -> "Android (Compose)"
        "flutter" -> "Flutter"
        "go" -> "Go"
        "java" -> "Java (Gradle)"
        "node" -> "Node.js"
        "office" -> "Office 文档"
        "python" -> "Python"
        "typescript" -> "TypeScript"
        "web" -> "Web 静态页"
        else -> id
    }

    companion object {
        private const val TEMPLATES_ROOT = "templates"
    }
}