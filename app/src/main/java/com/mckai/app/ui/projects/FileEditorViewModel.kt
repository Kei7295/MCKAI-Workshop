package com.mckai.app.ui.projects

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mckai.app.MCKAIApp
import com.mckai.app.data.db.entity.ProjectFileEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FileEditorState(val file: ProjectFileEntity? = null, val content: String = "", val saved: Boolean = true)

class FileEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as MCKAIApp).appContainer.database
    private val _state = MutableStateFlow(FileEditorState())
    val state: StateFlow<FileEditorState> = _state.asStateFlow()

    fun load(fileId: Long) {
        viewModelScope.launch {
            val file = db.projectFileDao().getById(fileId)
            _state.update { it.copy(file = file, content = file?.content ?: "", saved = true) }
        }
    }

    fun updateContent(content: String) { _state.update { it.copy(content = content, saved = false) } }

    fun save() {
        val file = _state.value.file ?: return
        viewModelScope.launch {
            db.projectFileDao().updateContent(file.id, _state.value.content)
            _state.update { it.copy(saved = true) }
        }
    }
}
