package com.mckai.app.ui.projects

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mckai.app.MCKAIApp
import com.mckai.app.data.db.entity.ProjectEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProjectsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as MCKAIApp).appContainer.database

    val projects: StateFlow<List<ProjectEntity>> = db.projectDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch { db.projectDao().delete(project) }
    }
}
