package com.mckai.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mckai.app.data.llm.ProviderConfig
import com.mckai.app.data.llm.ProviderPresets
import com.mckai.app.data.llm.ProviderType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderEditScreen(
    providerId: String?,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val providers by viewModel.providers.collectAsState()
    val testResult by viewModel.testResult.collectAsState()

    val existing = providers.firstOrNull { it.id == providerId }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: ProviderType.OPENAI) }
    var baseUrl by remember { mutableStateOf(existing?.baseUrl ?: type.defaultBaseUrl) }
    var apiKey by remember { mutableStateOf(existing?.apiKey ?: "") }
    var modelsText by remember { mutableStateOf(existing?.models?.joinToString("\n") ?: "") }
    var defaultModel by remember { mutableStateOf(existing?.defaultModel ?: "") }
    var temperature by remember { mutableFloatStateOf(existing?.temperature ?: 0.7f) }
    var maxTokens by remember { mutableStateOf((existing?.maxTokens ?: 4096).toString()) }
    var showKey by remember { mutableStateOf(false) }
    var showPresets by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing != null) "编辑模型" else "添加模型") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "返回") } },
                actions = {
                    if (existing != null) {
                        IconButton(onClick = { viewModel.deleteProvider(existing.id); onBack() }) {
                            Icon(Icons.Filled.Delete, "删除")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Preset button
            OutlinedButton(onClick = { showPresets = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.AutoAwesome, null)
                Spacer(Modifier.width(8.dp))
                Text("使用预设")
            }

            // Type
            Text("类型", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProviderType.entries.forEach { t ->
                    FilterChip(selected = type == t, onClick = {
                        type = t
                        if (baseUrl.isBlank() || ProviderType.entries.any { it.defaultBaseUrl == baseUrl }) {
                            baseUrl = t.defaultBaseUrl
                        }
                    }, label = { Text(t.label) })
                }
            }

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it }, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(
                value = apiKey, onValueChange = { apiKey = it }, label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { IconButton(onClick = { showKey = !showKey }) { Icon(if (showKey) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null) } }
            )

            OutlinedTextField(value = modelsText, onValueChange = { modelsText = it }, label = { Text("模型列表（每行一个）") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            OutlinedTextField(value = defaultModel, onValueChange = { defaultModel = it }, label = { Text("默认模型") }, modifier = Modifier.fillMaxWidth())

            Text("温度: ${"%.2f".format(temperature)}", style = MaterialTheme.typography.labelLarge)
            Slider(value = temperature, onValueChange = { temperature = it }, valueRange = 0f..2f)

            OutlinedTextField(
                value = maxTokens, onValueChange = { maxTokens = it.filter { c -> c.isDigit() } },
                label = { Text("最大 Token") }, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(Modifier.height(8.dp))

            // Test button
            OutlinedButton(
                onClick = {
                    val testProvider = ProviderConfig(
                        name = name, type = type, baseUrl = baseUrl, apiKey = apiKey,
                        models = modelsText.lines().filter { it.isNotBlank() },
                        defaultModel = defaultModel, temperature = temperature, maxTokens = maxTokens.toIntOrNull() ?: 4096
                    )
                    viewModel.testConnection(testProvider)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.WifiFind, null)
                Spacer(Modifier.width(8.dp))
                Text("测试连接")
            }

            testResult?.let {
                Card(Modifier.fillMaxWidth()) { Text(it, Modifier.padding(12.dp)) }
            }

            // Save
            Button(
                onClick = {
                    val provider = ProviderConfig(
                        id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                        name = name.ifBlank { type.label },
                        type = type, baseUrl = baseUrl, apiKey = apiKey,
                        models = modelsText.lines().filter { it.isNotBlank() },
                        defaultModel = defaultModel.ifBlank { modelsText.lines().firstOrNull() ?: "" },
                        temperature = temperature, maxTokens = maxTokens.toIntOrNull() ?: 4096
                    )
                    viewModel.saveProvider(provider)
                    viewModel.clearTestResult()
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && apiKey.isNotBlank()
            ) {
                Icon(Icons.Filled.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("保存")
            }
        }
    }

    if (showPresets) {
        ModalBottomSheet(onDismissRequest = { showPresets = false }) {
            Column(Modifier.padding(16.dp)) {
                Text("选择预设", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                ProviderPresets.builtIn().forEach { preset ->
                    ListItem(
                        headlineContent = { Text(preset.name) },
                        supportingContent = { Text("${preset.type.label} | ${preset.models.joinToString(", ")}") },
                        modifier = Modifier.clickable {
                            name = preset.name
                            type = preset.type
                            baseUrl = preset.baseUrl
                            apiKey = preset.apiKey
                            modelsText = preset.models.joinToString("\n")
                            defaultModel = preset.defaultModel
                            showPresets = false
                        }
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
