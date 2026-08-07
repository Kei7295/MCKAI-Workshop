package com.mckai.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onEditProvider: (String) -> Unit,
    onNewProvider: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val providers by viewModel.providers.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val memoryEnabled by viewModel.memoryEnabled.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("设置") }) }
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Theme
            item {
                Text("外观", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色").forEach { (mode, label) ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { viewModel.setTheme(mode) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            // Providers
            item {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("AI 模型", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = onNewProvider) { Icon(Icons.Filled.Add, "添加") }
                }
            }
            items(providers, key = { it.id }) { provider ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onEditProvider(provider.id) }
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(provider.name, style = MaterialTheme.typography.bodyLarge)
                            Text("${provider.type.label} | ${provider.displayModel()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Filled.ChevronRight, null)
                    }
                }
            }

            // Memory
            item {
                Spacer(Modifier.height(8.dp))
                Text("功能", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("记忆功能", modifier = Modifier.weight(1f))
                    Switch(checked = memoryEnabled, onCheckedChange = { viewModel.setMemoryEnabled(it) })
                }
            }

            // About
            item {
                Spacer(Modifier.height(16.dp))
                Text("关于", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("MCKAI 模组工坊 v1.0.0", style = MaterialTheme.typography.bodyMedium)
                Text("融合 RikkaHub + Operit + ModCrafting 的 AI 模组开发助手", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
