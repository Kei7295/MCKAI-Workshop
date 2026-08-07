package com.mckai.app.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mckai.app.MCKAIApp
import com.mckai.app.data.db.entity.ConversationEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatListViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as MCKAIApp).appContainer.database

    val conversations: StateFlow<List<ConversationEntity>> = db.conversationDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
}
