package com.mckai.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mckai.app.data.character.CharacterCard
import com.mckai.app.data.character.CharacterCardRepository
import com.mckai.app.ui.components.AppleAvatar
import com.mckai.app.ui.components.AppleCard
import com.mckai.app.ui.components.AppleField
import com.mckai.app.ui.components.AppleNavBar
import com.mckai.app.ui.components.ApplePrimaryButton
import com.mckai.app.ui.components.AppleSectionHeader

/** 助手编辑：systemPrompt 模板 + 变量替换（RikkaHub Assistant promptVariables） */
@Composable
fun AssistantEditScreen(
    assistantId: Long,
    onBack: () -> Unit,
    viewModel: AssistantViewModel = viewModel()
) {
    val assistants by viewModel.assistants.collectAsState()
    val existing = assistants.firstOrNull { it.id == assistantId }

    // 数据来自异步 StateFlow：首次组合时为空列表，Room 流到达后需一次性回填
    var initialized by remember { mutableStateOf(existing != null) }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var avatar by remember { mutableStateOf(existing?.avatar ?: "") }
    var systemPrompt by remember { mutableStateOf(existing?.systemPrompt ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var toolsEnabled by remember { mutableStateOf(existing?.toolsEnabled ?: true) }
    var memoryEnabled by remember { mutableStateOf(existing?.memoryEnabled ?: false) }
    var showPresets by remember { mutableStateOf(false) }
    var cardMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // 角色卡导入（Tavern v2 JSON 或 PNG 尾部 JSON）：解析后回填表单
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@rememberLauncherForActivityResult
        val card = CharacterCardRepository.parseBytes(bytes)
        if (card != null) {
            val assistant = CharacterCardRepository.toAssistant(card)
            name = assistant.name
            systemPrompt = assistant.systemPrompt
            description = assistant.description ?: ""
            toolsEnabled = assistant.toolsEnabled
            memoryEnabled = false
            cardMessage = "已导入角色卡：${card.name}"
        } else {
            cardMessage = "角色卡解析失败（需 Tavern v2 JSON 或 PNG 角色卡）"
        }
    }
    // 角色卡导出：当前表单 → Tavern v2 JSON
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val card = CharacterCard(name = name, description = description, systemPrompt = systemPrompt)
        val json = CharacterCardRepository.toTavernJson(card)
        context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
        cardMessage = "已导出角色卡"
    }

    // 新建页（assistantId==0）不需要回填；编辑页等待数据加载后填充一次
    LaunchedEffect(existing) {
        if (assistantId > 0 && existing != null && !initialized) {
            name = existing.name
            avatar = existing.avatar ?: ""
            systemPrompt = existing.systemPrompt
            description = existing.description ?: ""
            toolsEnabled = existing.toolsEnabled
            memoryEnabled = existing.memoryEnabled
            initialized = true
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppleNavBar(
            title = if (assistantId > 0) "编辑助手" else "新建助手",
            onBack = onBack,
            actions = {
                IconButton(onClick = { importLauncher.launch("*/*") }) {
                    Icon(Icons.Filled.FileOpen, "导入角色卡", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { exportLauncher.launch("${name.ifBlank { "角色卡" }}.json") }) {
                    Icon(Icons.Filled.FileDownload, "导出角色卡", tint = MaterialTheme.colorScheme.primary)
                }
            }
        )

        cardMessage?.let { msg ->
            Text(
                msg,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppleAvatar(
                    name = name,
                    avatar = avatar,
                    size = 64.dp,
                    fontSize = 24.sp
                )
                Spacer(Modifier.width(16.dp))
                AppleField(
                    value = avatar,
                    onValueChange = { avatar = it },
                    label = "头像字符",
                    placeholder = "可填字符；留空显示名称首字母"
                )
            }
            Spacer(Modifier.height(16.dp))

            AppleSectionHeader("基本信息")
            AppleCard {
                Column(Modifier.padding(16.dp)) {
                    AppleField(
                        value = name,
                        onValueChange = { name = it },
                        label = "名称",
                        placeholder = "例如：代码评审专家"
                    )
                    Spacer(Modifier.height(12.dp))
                    AppleField(
                        value = description,
                        onValueChange = { description = it },
                        label = "简介",
                        placeholder = "一句话描述这个角色的能力",
                        minLines = 2
                    )
                }
            }

            AppleSectionHeader("系统提示词")
            AppleCard {
                Column(Modifier.padding(16.dp)) {
                    AppleField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        label = "System Prompt",
                        placeholder = "定义角色的行为、能力和输出格式...",
                        minLines = 8,
                        singleLine = false
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "变量：{{name}} {{device}} {{date}} 会在发送时替换",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AppleSectionHeader("能力")
            AppleCard {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Build, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("工具调用", modifier = Modifier.weight(1f), fontSize = 15.sp)
                        Switch(
                            checked = toolsEnabled,
                            onCheckedChange = { toolsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF34C759)
                            )
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Memory, null, tint = Color(0xFF34C759), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("记忆功能", modifier = Modifier.weight(1f), fontSize = 15.sp)
                        Switch(
                            checked = memoryEnabled,
                            onCheckedChange = { memoryEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF34C759)
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            ApplePrimaryButton(
                text = "保存",
                icon = Icons.Filled.Save,
                enabled = name.isNotBlank() && systemPrompt.isNotBlank(),
                onClick = {
                    viewModel.save(
                        AssistantForm(
                            id = assistantId,
                            name = name,
                            avatar = avatar,
                            systemPrompt = systemPrompt,
                            description = description,
                            toolsEnabled = toolsEnabled,
                            memoryEnabled = memoryEnabled,
                            isBuiltIn = existing?.isBuiltIn ?: false
                        )
                    )
                    onBack()
                }
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}