package com.mckai.app.ui.projects

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mckai.app.MCKAIApp
import com.mckai.app.data.db.entity.ProjectEntity
import com.mckai.app.data.db.entity.ProjectFileEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProjectDetailState(
    val project: ProjectEntity? = null,
    val files: List<ProjectFileEntity> = emptyList()
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
}
