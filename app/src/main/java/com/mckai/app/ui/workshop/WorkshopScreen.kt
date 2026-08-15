package com.mckai.app.ui.workshop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mckai.app.domain.workshop.ModEdition
import com.mckai.app.ui.components.AppleActionSheet
import com.mckai.app.ui.components.AppleCard
import com.mckai.app.ui.components.AppleDestructiveRow
import com.mckai.app.ui.components.AppleField
import com.mckai.app.ui.components.AppleNavBar
import com.mckai.app.ui.components.ApplePrimaryButton
import com.mckai.app.ui.components.AppleRow
import com.mckai.app.ui.components.AppleSheetOption

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

@Composable
fun EditionStep(selected: ModEdition, onSelect: (ModEdition) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppleNavBar(title = "选择平台")
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(ModEdition.entries) { edition ->
                val isSelected = edition == selected
                AppleCard(modifier = Modifier.clickable { onSelect(edition) }) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.Build,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                edition.label,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                edition.description,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "默认版本: ${edition.defaultMcVersion}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

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
    val canStart = state.name.isNotBlank() && state.selectedProvider != null

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppleNavBar(title = "描述模组", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AppleCard {
                    AppleField(
                        value = state.name,
                        onValueChange = onNameChange,
                        label = "模组名称 *",
                        placeholder = "输入模组名称",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            item {
                AppleCard {
                    AppleField(
                        value = state.modId,
                        onValueChange = onModIdChange,
                        label = "Mod ID（可选）",
                        placeholder = "例如：mymod",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            item {
                AppleCard {
                    AppleField(
                        value = state.mcVersion,
                        onValueChange = onVersionChange,
                        label = "MC 版本",
                        placeholder = "例如：1.20.1",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            item {
                AppleCard {
                    AppleField(
                        value = state.description,
                        onValueChange = onDescChange,
                        label = "描述",
                        placeholder = "描述你的模组",
                        minLines = 2,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            item {
                AppleCard {
                    AppleField(
                        value = state.features,
                        onValueChange = onFeaturesChange,
                        label = "功能需求",
                        placeholder = "详细描述你想要的功能",
                        minLines = 3,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            item {
                AppleCard {
                    AppleRow(
                        title = state.selectedProvider?.name ?: "未选择",
                        subtitle = state.selectedProvider?.displayModel(),
                        icon = Icons.Filled.SmartToy,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = { showProviderPicker = true }
                    )
                }
            }
            item {
                ApplePrimaryButton(
                    text = "开始生成",
                    icon = Icons.Filled.PlayArrow,
                    enabled = canStart,
                    onClick = onStart
                )
            }
        }
    }

    if (showProviderPicker) {
        AppleActionSheet(
            title = "选择模型",
            options = buildList {
                state.providers.filter { it.enabled }.forEach { provider ->
                    provider.models.forEach { model ->
                        val isSelected =
                            state.selectedProvider?.id == provider.id && state.selectedProvider?.defaultModel == model
                        add(
                            AppleSheetOption(
                                label = "$model  ·  ${provider.name}",
                                bold = isSelected,
                                onClick = { onProviderChange(provider.copy(defaultModel = model)) }
                            )
                        )
                    }
                }
            },
            onDismiss = { showProviderPicker = false }
        )
    }
}

@Composable
fun GeneratingStep(state: WorkshopUiState, onCancel: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppleNavBar(title = "生成中")
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val progress = state.progress
            if (progress != null) {
                Text(progress.message, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress.progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "${progress.filesGenerated} 文件已生成",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                )
            }
            Spacer(Modifier.height(24.dp))
            AppleCard(modifier = Modifier.fillMaxWidth().weight(1f)) {
                LazyColumn(Modifier.padding(12.dp)) {
                    items(state.log.takeLast(50)) { line ->
                        Text(
                            line,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            AppleDestructiveRow("取消", onCancel)
        }
    }
}

@Composable
fun ResultStep(state: WorkshopUiState, onRetry: () -> Unit, onReset: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppleNavBar(title = "结果")
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        if (state.success) Color(0xFF34C759).copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (state.success) Icons.Filled.CheckCircle else Icons.Filled.Error,
                    null,
                    modifier = Modifier.size(40.dp),
                    tint = if (state.success) Color(0xFF34C759) else MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(state.resultMessage, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(20.dp))
            if (state.generatedFiles.isNotEmpty()) {
                AppleCard {
                    Column(Modifier.padding(16.dp)) {
                        Text("生成的文件", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Spacer(Modifier.height(8.dp))
                        state.generatedFiles.keys.forEach { path ->
                            Text("  $path", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            ApplePrimaryButton(
                text = "重新生成",
                icon = if (state.success) null else Icons.Filled.Refresh,
                onClick = onRetry
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onReset),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "返回开始",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
