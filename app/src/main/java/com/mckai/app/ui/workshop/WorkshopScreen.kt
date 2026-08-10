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
        topBar = {
            TopAppBar(
                title = { Text("选择平台", fontWeight = FontWeight.SemiBold, fontSize = 17.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(ModEdition.entries) { edition ->
                val isSelected = edition == selected
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(edition) }
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                edition.label,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                edition.description,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "默认版本: ${edition.defaultMcVersion}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
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
                title = { Text("描述模组", fontWeight = FontWeight.SemiBold, fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ChevronLeft, "返回", modifier = Modifier.size(28.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AppleFormCard {
                    AppleFormField(label = "模组名称 *") {
                        OutlinedTextField(
                            value = state.name,
                            onValueChange = onNameChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("输入模组名称") },
                            colors = appleTextFieldColors(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                        )
                    }
                }
            }
            item {
                AppleFormCard {
                    AppleFormField(label = "Mod ID（可选）") {
                        OutlinedTextField(
                            value = state.modId,
                            onValueChange = onModIdChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("例如：mymod") },
                            colors = appleTextFieldColors(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                        )
                    }
                }
            }
            item {
                AppleFormCard {
                    AppleFormField(label = "MC 版本") {
                        OutlinedTextField(
                            value = state.mcVersion,
                            onValueChange = onVersionChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("例如：1.20.1") },
                            colors = appleTextFieldColors(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                        )
                    }
                }
            }
            item {
                AppleFormCard {
                    AppleFormField(label = "描述") {
                        OutlinedTextField(
                            value = state.description,
                            onValueChange = onDescChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("描述你的模组") },
                            minLines = 2,
                            colors = appleTextFieldColors(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                        )
                    }
                }
            }
            item {
                AppleFormCard {
                    AppleFormField(label = "功能需求") {
                        OutlinedTextField(
                            value = state.features,
                            onValueChange = onFeaturesChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("详细描述你想要的功能") },
                            minLines = 3,
                            colors = appleTextFieldColors(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                        )
                    }
                }
            }
            item {
                AppleFormCard {
                    AppleFormField(label = "AI 模型") {
                        val selectedProvider = state.selectedProvider
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showProviderPicker = true }
                        ) {
                            Row(
                                Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.SmartToy,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        selectedProvider?.name ?: "未选择",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 16.sp
                                    )
                                    if (selectedProvider != null) {
                                        Text(
                                            selectedProvider.displayModel(),
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Filled.ChevronRight,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (state.name.isNotBlank() && state.selectedProvider != null)
                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = state.name.isNotBlank() && state.selectedProvider != null) { onStart() }
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            null,
                            tint = if (state.name.isNotBlank() && state.selectedProvider != null)
                                Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "开始生成",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            color = if (state.name.isNotBlank() && state.selectedProvider != null)
                                Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showProviderPicker) {
        ModalBottomSheet(onDismissRequest = { showProviderPicker = false }) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "选择模型",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                state.providers.filter { it.enabled }.forEach { provider ->
                    provider.models.forEach { model ->
                        ListItem(
                            headlineContent = { Text(model, fontWeight = FontWeight.Medium) },
                            supportingContent = { Text(provider.name, fontSize = 13.sp) },
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
        topBar = {
            TopAppBar(
                title = { Text("生成中", fontWeight = FontWeight.SemiBold, fontSize = 17.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
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
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
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
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth().clickable { onCancel() }
            ) {
                Text(
                    "取消",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultStep(state: WorkshopUiState, onRetry: () -> Unit, onReset: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("结果", fontWeight = FontWeight.SemiBold, fontSize = 17.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
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
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
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
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().clickable { onRetry() }
            ) {
                Text(
                    "重新生成",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().clickable { onReset() }
            ) {
                Text(
                    "返回开始",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun AppleFormCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
fun AppleFormField(label: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Composable
fun appleTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = Color.Transparent,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent
)
