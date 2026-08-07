package com.mckai.app.ui.workshop

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mckai.app.domain.workshop.ModEdition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkshopScreen(viewModel: WorkshopViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    when (state.step) {
        WorkshopStep.EDITION -> EditionStep(
            selected = state.edition,
            onSelect = { viewModel.selectEdition(it); viewModel.goToDescribe() }
        )
        WorkshopStep.DESCRIBE -> DescribeStep(
            state = state,
            onNameChange = viewModel::updateName,
            onVersionChange = viewModel::updateMcVersion,
            onDescChange = viewModel::updateDescription,
            onFeaturesChange = viewModel::updateFeatures,
            onModIdChange = viewModel::updateModId,
            onProviderChange = viewModel::selectProvider,
            onStart = viewModel::start,
            onBack = viewModel::backToEdition
        )
        WorkshopStep.GENERATING -> GeneratingStep(
            state = state,
            onCancel = viewModel::cancel
        )
        WorkshopStep.RESULT -> ResultStep(
            state = state,
            onRetry = viewModel::reset,
            onReset = viewModel::reset
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditionStep(selected: ModEdition, onSelect: (ModEdition) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("选择平台") }) }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(ModEdition.entries) { edition ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(edition) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (edition == selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(edition.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(edition.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("默认版本: ${edition.defaultMcVersion}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DescribeStep(
    state: WorkshopUiState,
    onNameChange: (String) -> Unit,
    onVersionChange: (String) -> Unit,
    onDescChange: (String) -> Unit,
    onFeaturesChange: (String) -> Unit,
    onModIdChange: (String) -> Unit,
    onProviderChange: (com.mckai.app.data.llm.ProviderConfig) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit
) {
    var showProviderPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("描述模组") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "返回") } }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text("模组名称 *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = state.modId,
                    onValueChange = onModIdChange,
                    label = { Text("Mod ID（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = state.mcVersion,
                    onValueChange = onVersionChange,
                    label = { Text("MC 版本") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = state.description,
                    onValueChange = onDescChange,
                    label = { Text("描述") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
            item {
                OutlinedTextField(
                    value = state.features,
                    onValueChange = onFeaturesChange,
                    label = { Text("功能需求（详细描述你想要的功能）") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
            item {
                Text("AI 模型", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                val selectedProvider = state.selectedProvider
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().clickable { showProviderPicker = true }
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.SmartToy, null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(selectedProvider?.name ?: "未选择", fontWeight = FontWeight.Bold)
                            Text(selectedProvider?.displayModel() ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Filled.ArrowDropDown, null)
                    }
                }
            }
            item {
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.name.isNotBlank() && state.selectedProvider != null
                ) {
                    Icon(Icons.Filled.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("开始生成")
                }
            }
        }
    }

    if (showProviderPicker) {
        ModalBottomSheet(onDismissRequest = { showProviderPicker = false }) {
            Column(Modifier.padding(16.dp)) {
                Text("选择模型", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                state.providers.filter { it.enabled }.forEach { provider ->
                    provider.models.forEach { model ->
                        ListItem(
                            headlineContent = { Text(model) },
                            supportingContent = { Text(provider.name) },
                            modifier = Modifier.clickable {
                                onProviderChange(provider.copy(defaultModel = model))
                                showProviderPicker = false
                            }
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratingStep(state: WorkshopUiState, onCancel: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("生成中") }) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val progress = state.progress
            if (progress != null) {
                Text(progress.message, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${progress.filesGenerated} 文件已生成",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(24.dp))
            // Log display
            Card(Modifier.fillMaxWidth().weight(1f)) {
                LazyColumn(Modifier.padding(12.dp)) {
                    items(state.log.takeLast(50)) { line ->
                        Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text("取消")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultStep(state: WorkshopUiState, onRetry: () -> Unit, onReset: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("结果") }) }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                if (state.success) Icons.Filled.CheckCircle else Icons.Filled.Error,
                null,
                Modifier.size(64.dp),
                tint = if (state.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(16.dp))
            Text(state.resultMessage, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            if (state.generatedFiles.isNotEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("生成的文件：", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        state.generatedFiles.keys.forEach { path ->
                            Text("• $path", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("重新生成") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text("返回开始") }
        }
    }
}
