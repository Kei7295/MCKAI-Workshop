package com.mckai.app.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mckai.app.ui.components.MarkdownText
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    convId: Long?,
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var showProviderPicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(convId) { viewModel.loadConversation(convId) }

    LaunchedEffect(state.messages.size, state.streamingText) {
        if (state.messages.isNotEmpty() || state.streamingText.isNotBlank()) {
            listState.animateScrollToItem(state.messages.size + if (state.streamingText.isNotBlank()) 1 else 0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("对话", style = MaterialTheme.typography.titleMedium)
                        if (state.selectedProvider != null) {
                            Text(
                                "${state.selectedProvider!!.name} / ${state.selectedProvider!!.displayModel()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "返回") }
                },
                actions = {
                    IconButton(onClick = { showProviderPicker = true }) {
                        Icon(Icons.Filled.SwapHoriz, "切换模型")
                    }
                    IconButton(onClick = { viewModel.toggleTools() }) {
                        Icon(
                            if (state.toolsEnabled) Icons.Filled.Build else Icons.Outlined.Build,
                            "工具",
                            tint = if (state.toolsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp)
        ) {
            // Messages
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (state.messages.isEmpty() && state.streamingText.isBlank()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.Chat, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                Text("开始对话", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                items(state.messages, key = { it.id }) { msg ->
                    MessageBubble(msg)
                }
                if (state.streamingText.isNotBlank()) {
                    item {
                        StreamingBubble(state.streamingText, state.reasoningText)
                    }
                }
                if (state.error != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(8.dp))
                                Text(state.error!!, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.clearError() }, Modifier.size(24.dp)) {
                                    Icon(Icons.Filled.Close, "关闭", Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Input
            InputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = { viewModel.send(inputText); inputText = "" },
                onStop = { viewModel.stop() },
                isGenerating = state.isGenerating
            )
        }
    }

    if (showProviderPicker) {
        ProviderPickerSheet(
            providers = state.providers,
            selected = state.selectedProvider,
            onSelect = { viewModel.selectProvider(it); showProviderPicker = false },
            onDismiss = { showProviderPicker = false }
        )
    }
}

@Composable
fun MessageBubble(msg: com.mckai.app.data.db.entity.MessageEntity) {
    val isUser = msg.role == "user"
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                MarkdownText(msg.content)
                if (msg.reasoningContent != null) {
                    Spacer(Modifier.height(4.dp))
                    Text("思考过程", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(msg.reasoningContent.take(200), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun StreamingBubble(text: String, reasoning: String) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                MarkdownText(text + "▌")
                if (reasoning.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text("思考中...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(reasoning.take(200), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun InputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    isGenerating: Boolean
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("输入消息...") },
            maxLines = 5,
            enabled = !isGenerating
        )
        Spacer(Modifier.width(8.dp))
        if (isGenerating) {
            FilledIconButton(onClick = onStop) {
                Icon(Icons.Filled.Stop, "停止")
            }
        } else {
            FilledIconButton(onClick = onSend, enabled = text.isNotBlank()) {
                Icon(Icons.Filled.Send, "发送")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderPickerSheet(
    providers: List<com.mckai.app.data.llm.ProviderConfig>,
    selected: com.mckai.app.data.llm.ProviderConfig?,
    onSelect: (com.mckai.app.data.llm.ProviderConfig) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            Text("选择模型", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            providers.filter { it.enabled }.forEach { provider ->
                provider.models.forEach { model ->
                    ListItem(
                        headlineContent = { Text(model) },
                        supportingContent = { Text(provider.name) },
                        leadingContent = {
                            RadioButton(
                                selected = selected?.id == provider.id && selected?.defaultModel == model,
                                onClick = { onSelect(provider.copy(defaultModel = model)) }
                            )
                        },
                        modifier = Modifier.clickable { onSelect(provider.copy(defaultModel = model)) }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
