package com.mckai.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mckai.app.data.llm.ProviderConfig
import com.mckai.app.data.llm.ProviderPresets
import com.mckai.app.data.llm.ProviderType
import com.mckai.app.ui.components.AppleActionSheet
import com.mckai.app.ui.components.AppleCard
import com.mckai.app.ui.components.AppleField
import com.mckai.app.ui.components.AppleNavBar
import com.mckai.app.ui.components.ApplePrimaryButton
import com.mckai.app.ui.components.AppleSecondaryButton
import com.mckai.app.ui.components.AppleSectionHeader
import com.mckai.app.ui.components.AppleSegmented
import com.mckai.app.ui.components.AppleSheetOption

@Composable
fun ProviderEditScreen(
    providerId: String?,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val providers by viewModel.providers.collectAsState()
    val testResult by viewModel.testResult.collectAsState()

    val existing = providers.firstOrNull { it.id == providerId }
    // providers 来自异步 StateFlow，首次组合为空——用 initialized 防重复覆盖用户编辑
    var initialized by remember { mutableStateOf(existing != null) }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: ProviderType.OPENAI) }
    var baseUrl by remember { mutableStateOf(existing?.baseUrl ?: type.defaultBaseUrl) }
    var apiKey by remember { mutableStateOf(existing?.apiKey ?: "") }
    var apiKeysText by remember { mutableStateOf(existing?.apiKeys?.joinToString("\n") ?: "") }
    var modelsText by remember { mutableStateOf(existing?.models?.joinToString("\n") ?: "") }
    var defaultModel by remember { mutableStateOf(existing?.defaultModel ?: "") }
    var temperature by remember { mutableFloatStateOf(existing?.temperature ?: 0.7f) }
    var maxTokens by remember { mutableStateOf((existing?.maxTokens ?: 4096).toString()) }
    var showKey by remember { mutableStateOf(false) }
    var showPresets by remember { mutableStateOf(false) }

    LaunchedEffect(existing) {
        if (providerId != null && existing != null && !initialized) {
            name = existing.name
            type = existing.type
            baseUrl = existing.baseUrl
            apiKey = existing.apiKey
            apiKeysText = existing.apiKeys.joinToString("\n")
            modelsText = existing.models.joinToString("\n")
            defaultModel = existing.defaultModel
            temperature = existing.temperature
            maxTokens = existing.maxTokens.toString()
            initialized = true
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppleNavBar(
            title = if (existing != null) "编辑模型" else "添加模型",
            onBack = onBack,
            actions = {
                if (existing != null) {
                    IconButton(onClick = { viewModel.deleteProvider(existing.id); onBack() }) {
                        Icon(
                            Icons.Filled.Delete,
                            "删除",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Presets
            AppleSecondaryButton(
                text = "使用预设",
                icon = Icons.Filled.AutoAwesome,
                onClick = { showPresets = true }
            )

            // Type
            AppleSectionHeader("提供商类型")
            AppleSegmented(
                options = ProviderType.entries.map { it.name to it.label },
                selected = type.name,
                scrollable = true,
                onSelect = { v ->
                    type = ProviderType.entries.first { it.name == v }
                    if (baseUrl.isBlank() || ProviderType.entries.any { it.defaultBaseUrl == baseUrl }) {
                        baseUrl = type.defaultBaseUrl
                    }
                }
            )

            // Basic configuration group
            AppleSectionHeader("基本配置")
            AppleCard {
                Column(Modifier.padding(16.dp)) {
                    AppleField(
                        value = name,
                        onValueChange = { name = it },
                        label = "名称",
                        placeholder = "例如：OpenAI"
                    )
                    Spacer(Modifier.height(12.dp))
                    AppleField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = "Base URL",
                        placeholder = "https://api.openai.com/v1"
                    )
                    Spacer(Modifier.height(12.dp))
                    AppleField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = "API Key",
                        placeholder = "sk-...",
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
trailingIcon = {
                                IconButton(onClick = { showKey = !showKey }) {
                                    Icon(
                                        if (showKey) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                    Spacer(Modifier.height(12.dp))
                    AppleField(
                        value = apiKeysText,
                        onValueChange = { apiKeysText = it },
                        label = "备用 API Keys（每行一个，自动轮转）",
                        placeholder = "sk-xxx\nsk-yyy\nsk-zzz",
                        minLines = 2,
                        singleLine = false
                    )
                }
            }

            // Models group
            AppleSectionHeader("模型")
            AppleCard {
                Column(Modifier.padding(16.dp)) {
                    AppleField(
                        value = modelsText,
                        onValueChange = { modelsText = it },
                        label = "模型列表（每行一个）",
                        placeholder = "gpt-4.1\ngpt-4o",
                        minLines = 3,
                        singleLine = false
                    )
                    Spacer(Modifier.height(12.dp))
                    AppleField(
                        value = defaultModel,
                        onValueChange = { defaultModel = it },
                        label = "默认模型",
                        placeholder = "gpt-4.1"
                    )
                }
            }

            // Advanced group
            AppleSectionHeader("高级")
            AppleCard {
                Column(Modifier.padding(16.dp)) {
                    Text("温度: ${"%.2f".format(temperature)}", fontSize = 14.sp)
                    Slider(
                        value = temperature,
                        onValueChange = { temperature = it },
                        valueRange = 0f..2f
                    )
                    Spacer(Modifier.height(4.dp))
                    AppleField(
                        value = maxTokens,
                        onValueChange = { maxTokens = it.filter { c -> c.isDigit() } },
                        label = "最大 Token",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Test connection
            AppleSecondaryButton(
                text = "测试连接",
                icon = Icons.Filled.WifiFind,
                onClick = {
                    val testProvider = ProviderConfig(
                        name = name, type = type, baseUrl = baseUrl, apiKey = apiKey,
                        models = modelsText.lines().filter { it.isNotBlank() },
                        defaultModel = defaultModel, temperature = temperature, maxTokens = maxTokens.toIntOrNull() ?: 4096
                    )
                    viewModel.testConnection(testProvider)
                }
            )

            testResult?.let {
                Spacer(Modifier.height(12.dp))
                AppleCard {
                    Text(
                        it,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Save
            ApplePrimaryButton(
                text = "保存",
                icon = Icons.Filled.Save,
                enabled = name.isNotBlank() && apiKey.isNotBlank(),
                onClick = {
                    val provider = ProviderConfig(
                        id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                        name = name.ifBlank { type.label },
                        type = type, baseUrl = baseUrl, apiKey = apiKey,
                        apiKeys = apiKeysText.lines().map { it.trim() }.filter { it.isNotBlank() },
                        models = modelsText.lines().filter { it.isNotBlank() },
                        defaultModel = defaultModel.ifBlank { modelsText.lines().firstOrNull() ?: "" },
                        temperature = temperature, maxTokens = maxTokens.toIntOrNull() ?: 4096
                    )
                    viewModel.saveProvider(provider)
                    viewModel.clearTestResult()
                    onBack()
                }
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showPresets) {
        AppleActionSheet(
            title = "选择预设",
            options = ProviderPresets.builtIn().map { preset ->
                AppleSheetOption(
                    label = "${preset.name}  ·  ${preset.type.label}",
                    bold = existing?.id == preset.id,
                    onClick = {
                        name = preset.name
                        type = preset.type
                        baseUrl = preset.baseUrl
                        apiKey = preset.apiKey
                        modelsText = preset.models.joinToString("\n")
                        defaultModel = preset.defaultModel
                    }
                )
            },
            onDismiss = { showPresets = false }
        )
    }
}