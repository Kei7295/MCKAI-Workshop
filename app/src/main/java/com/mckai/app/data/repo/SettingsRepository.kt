package com.mckai.app.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mckai.app.data.llm.ProviderConfig
import com.mckai.app.data.llm.ProviderPresets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("mckai_settings")

class SettingsRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val providersKey = stringPreferencesKey("providers")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val lastProviderIdKey = stringPreferencesKey("last_provider_id")
    private val lastModelKey = stringPreferencesKey("last_model")
    private val isFirstRunKey = booleanPreferencesKey("is_first_run")
    private val activeAssistantIdKey = longPreferencesKey("active_assistant_id")
    private val memoryEnabledKey = booleanPreferencesKey("memory_enabled")
    private val toolsEnabledKey = booleanPreferencesKey("tools_enabled")

    val providers: Flow<List<ProviderConfig>> = context.dataStore.data.map { prefs ->
        prefs[providersKey]?.let { raw ->
            try { json.decodeFromString<List<ProviderConfig>>(raw) } catch (_: Exception) { ProviderPresets.builtIn() }
        } ?: ProviderPresets.builtIn()
    }

    val themeMode: Flow<String> = context.dataStore.data.map { it[themeModeKey] ?: "system" }
    val lastProviderId: Flow<String> = context.dataStore.data.map { it[lastProviderIdKey] ?: "" }
    val lastModel: Flow<String> = context.dataStore.data.map { it[lastModelKey] ?: "" }
    val isFirstRun: Flow<Boolean> = context.dataStore.data.map { it[isFirstRunKey] ?: true }
    val activeAssistantId: Flow<Long> = context.dataStore.data.map { it[activeAssistantIdKey] ?: 0L }
    val memoryEnabled: Flow<Boolean> = context.dataStore.data.map { it[memoryEnabledKey] ?: false }
    val toolsEnabled: Flow<Boolean> = context.dataStore.data.map { it[toolsEnabledKey] ?: true }

    suspend fun saveProviders(list: List<ProviderConfig>) {
        context.dataStore.edit { it[providersKey] = json.encodeToString(list) }
    }

    suspend fun upsertProvider(provider: ProviderConfig) {
        val current = providers.firstOrNull() ?: ProviderPresets.builtIn()
        val idx = current.indexOfFirst { it.id == provider.id }
        val updated = if (idx >= 0) current.toMutableList().apply { set(idx, provider) } else current + provider
        saveProviders(updated)
    }

    suspend fun deleteProvider(id: String) {
        val current = providers.firstOrNull() ?: return
        saveProviders(current.filter { it.id != id })
    }

    suspend fun firstEnabledProvider(): ProviderConfig? =
        providers.firstOrNull()?.firstOrNull { it.enabled }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[themeModeKey] = mode }
    }

    suspend fun setLastProvider(id: String, model: String) {
        context.dataStore.edit {
            it[lastProviderIdKey] = id
            it[lastModelKey] = model
        }
    }

    suspend fun markNotFirstRun() {
        context.dataStore.edit { it[isFirstRunKey] = false }
    }

    suspend fun setActiveAssistant(id: Long) {
        context.dataStore.edit { it[activeAssistantIdKey] = id }
    }

    suspend fun setMemoryEnabled(enabled: Boolean) {
        context.dataStore.edit { it[memoryEnabledKey] = enabled }
    }

    suspend fun setToolsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[toolsEnabledKey] = enabled }
    }
}
