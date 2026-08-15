package com.mckai.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mckai.app.MCKAIApp
import com.mckai.app.data.llm.ProviderConfig
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as MCKAIApp).appContainer
    private val settings = container.settings
    private val llmClient = container.llmClient

    val providers: StateFlow<List<ProviderConfig>> = settings.providers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val themeMode: StateFlow<String> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val memoryEnabled: StateFlow<Boolean> = settings.memoryEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val autoApproveSensitive: StateFlow<Boolean> = settings.autoApproveSensitive
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult: StateFlow<String?> = _testResult.asStateFlow()

    fun setTheme(mode: String) { viewModelScope.launch { settings.setThemeMode(mode) } }
    fun setMemoryEnabled(enabled: Boolean) { viewModelScope.launch { settings.setMemoryEnabled(enabled) } }
    fun setAutoApproveSensitive(enabled: Boolean) { viewModelScope.launch { settings.setAutoApproveSensitive(enabled) } }

    fun saveProvider(provider: ProviderConfig) {
        // NonCancellable：页面保存后立即 onBack 也不能中断写库
        viewModelScope.launch {
            withContext(kotlinx.coroutines.NonCancellable) { settings.upsertProvider(provider) }
        }
    }

    fun deleteProvider(id: String) {
        viewModelScope.launch {
            withContext(kotlinx.coroutines.NonCancellable) { settings.deleteProvider(id) }
        }
    }

    fun testConnection(provider: ProviderConfig) {
        viewModelScope.launch {
            _testResult.value = "测试中..."
            val result = llmClient.testConnection(provider)
            _testResult.value = result.fold(
                onSuccess = { "连接成功：$it" },
                onFailure = { "连接失败：${it.message}" }
            )
        }
    }

    fun clearTestResult() { _testResult.value = null }
}
