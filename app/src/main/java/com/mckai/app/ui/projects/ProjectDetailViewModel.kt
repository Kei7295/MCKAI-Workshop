package com.mckai.app.ui.projects

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mckai.app.MCKAIApp
import com.mckai.app.data.db.entity.ProjectEntity
import com.mckai.app.data.db.entity.ProjectFileEntity
import com.mckai.app.domain.workshop.template.LangMerger
import com.mckai.app.domain.workshop.template.ModTemplateType
import com.mckai.app.domain.workshop.template.TemplateEngine
import com.mckai.app.domain.workshop.template.TemplateParams
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProjectDetailState(
    val project: ProjectEntity? = null,
    val files: List<ProjectFileEntity> = emptyList(),
    val message: String? = null
)

class ProjectDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as MCKAIApp).appContainer.database
    private val _state = MutableStateFlow(ProjectDetailState())
    val state: StateFlow<ProjectDetailState> = _state.asStateFlow()

    fun load(projectId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(project = db.projectDao().getById(projectId)) }
            db.projectFileDao().observeByProject(projectId).collect { files ->
                _state.update { it.copy(files = files) }
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    /**
     * 模板生成（UI 通道，对应 AI 工具的 fabric_template_generate）：
     * 生成文件集写入项目，lang 条目合并进已有 zh_cn.json（不覆盖）。
     */
    fun generateTemplate(type: ModTemplateType, className: String, displayName: String) {
        val project = _state.value.project ?: return
        viewModelScope.launch {
            try {
                val modId = project.modId?.ifBlank { null } ?: project.name.lowercase().replace(" ", "_")
                val p = TemplateParams(
                    type = type,
                    modId = modId,
                    className = className,
                    displayName = displayName
                )
                val result = TemplateEngine.generate(p)
                val dao = db.projectFileDao()
                val files = result.files.toMutableMap()
                // lang 合并：优先合并进已有 zh_cn.json
                val langPath = "src/main/resources/assets/$modId/lang/zh_cn.json"
                if (result.langEntries.isNotEmpty()) {
                    val existing = dao.getByPath(project.id, langPath)?.content
                    files[langPath] = LangMerger.merge(existing, result.langEntries)
                }
                files.forEach { (path, content) ->
                    dao.upsert(
                        ProjectFileEntity(
                            projectId = project.id,
                            filePath = path,
                            fileName = path.substringAfterLast('/'),
                            content = content,
                            isGenerated = true
                        )
                    )
                }
                _state.update { it.copy(message = "已生成 ${result.files.size} 个文件：${result.summary}") }
            } catch (e: Exception) {
                Log.e("ProjectDetailVM", "generateTemplate failed", e)
                _state.update { it.copy(message = "生成失败：${e.message}") }
            }
        }
    }
}
