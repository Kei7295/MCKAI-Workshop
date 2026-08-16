package com.mckai.app.ui.projects

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mckai.app.MCKAIApp
import com.mckai.app.data.db.entity.ProjectEntity
import com.mckai.app.data.templates.ProjectTemplateRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProjectsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as MCKAIApp).appContainer.database
    private val templateRepo = ProjectTemplateRepository(application, db)

    val projects: StateFlow<List<ProjectEntity>> = db.projectDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val templates: List<ProjectTemplateRepository.TemplateMeta> = templateRepo.listTemplates()

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch { db.projectDao().delete(project) }
    }

    /** 从模板创建项目，成功后回调新项目 id。 */
    fun createFromTemplate(templateId: String, name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = templateRepo.createProjectFromTemplate(templateId, name)
            onCreated(id)
        }
    }
}
