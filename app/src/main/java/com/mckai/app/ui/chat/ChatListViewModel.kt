package com.mckai.app.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mckai.app.MCKAIApp
import com.mckai.app.data.backup.ConversationBackup
import com.mckai.app.data.db.entity.ConversationEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatListViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as MCKAIApp).appContainer.database

    val conversations: StateFlow<List<ConversationEntity>> = db.conversationDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 会话导出/导入结果消息（null=空闲）。 */
    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage.asStateFlow()

    fun createConversation(callback: (Long) -> Unit) {
        viewModelScope.launch {
            val id = db.conversationDao().insert(ConversationEntity())
            callback(id)
        }
    }

    fun deleteConversation(conv: ConversationEntity) {
        viewModelScope.launch { db.conversationDao().delete(conv) }
    }

    fun renameConversation(id: Long, title: String) {
        viewModelScope.launch { db.conversationDao().rename(id, title) }
    }

    /** 导出单个会话为备份 JSON，通过回调交给 UI 写入文件。 */
    fun exportConversation(convId: Long, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val conv = db.conversationDao().getById(convId) ?: run { onResult(null); return@launch }
            val messages = db.messageDao().getByConversation(convId)
            onResult(ConversationBackup.export(conv, messages))
        }
    }

    /** 导入备份 JSON，成功返回新会话 id。 */
    fun importConversation(jsonText: String) {
        viewModelScope.launch {
            val result = ConversationBackup.import(jsonText, db.conversationDao(), db.messageDao())
            _backupMessage.value = result.fold(
                onSuccess = { "已导入对话" },
                onFailure = { "导入失败：${it.message}" }
            )
        }
    }

    fun clearBackupMessage() {
        _backupMessage.value = null
    }
}