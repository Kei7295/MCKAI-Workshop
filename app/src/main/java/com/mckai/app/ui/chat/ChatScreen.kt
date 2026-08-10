package com.mckai.app.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            AppleChatTopBar(
                providerName = state.selectedProvider?.name ?: "",
                modelName = state.selectedProvider?.displayModel() ?: "",
                onBack = onBack,
                onSwitchModel = { showProviderPicker = true },
                onToggleTools = { viewModel.toggleTools() },
                toolsEnabled = state.toolsEnabled
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (state.messages.isEmpty() && state.streamingText.isBlank()) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(vertical = 80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Chat,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "开始对话",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "选择一个模型，开始你的创作",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                items(state.messages, key = { it.id }) { msg ->
                    AppleMessageBubble(msg)
                }
                if (state.streamingText.isNotBlank()) {
                    item {
                        AppleStreamingBubble(state.streamingText, state.reasoningText)
                    }
                }
                if (state.error != null) {
                    item {
                        AppleErrorCard(state.error!!) { viewModel.clearError() }
                    }
                }
            }

            // Input Bar
            AppleInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = { viewModel.send(inputText); inputText = "" },
                onStop = { viewModel.stop() },
                isGenerating = state.isGenerating
            )
        }
    }

    if (showProviderPicker) {
        AppleProviderPickerSheet(
            providers = state.providers,
            selected = state.selectedProvider,
            onSelect = { viewModel.selectProvider(it); showProviderPicker = false },
            onDismiss = { showProviderPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleChatTopBar(
    providerName: String,
    modelName: String,
    onBack: () -> Unit,
    onSwitchModel: () -> Unit,
    onToggleTools: () -> Unit,
    toolsEnabled: Boolean
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    "对话",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp
                )
                if (providerName.isNotBlank()) {
                    Text(
                        "$providerName / $modelName",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ChevronLeft, "返回", modifier = Modifier.size(28.dp))
            }
        },
        actions = {
            IconButton(onClick = onSwitchModel) {
                Icon(Icons.Filled.SwapHoriz, "切换模型")
            }
            IconButton(onClick = onToggleTools) {
                Icon(
                    if (toolsEnabled) Icons.Filled.Build else Icons.Outlined.Build,
                    "工具",
                    tint = if (toolsEnabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
fun AppleMessageBubble(msg: com.mckai.app.data.db.entity.MessageEntity) {
    val isUser = msg.role == "user"
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isUser) Color.White
    else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.SmartToy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = if (isUser) 18.dp else 4.dp,
                topEnd = if (isUser) 4.dp else 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 18.dp
            ),
            color = bubbleColor,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                MarkdownText(msg.content)
                if (msg.reasoningContent != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "思考过程",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor.copy(alpha = 0.6f)
                    )
                    Text(
                        msg.reasoningContent.take(200),
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.5f)
                    )
                }
            }
        }

        if (isUser) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun AppleStreamingBubble(text: String, reasoning: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.SmartToy,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                MarkdownText(text + "▌")
                if (reasoning.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "思考中...",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun AppleErrorCard(error: String, onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                error,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Close, "关闭", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun AppleInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    isGenerating: Boolean
) {
    val bgColor = MaterialTheme.colorScheme.surfaceVariant

    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .windowInsetsPadding(WindowInsets.ime),
            verticalAlignment = Alignment.Bottom
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = bgColor,
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("输入消息...", fontSize = 15.sp)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    maxLines = 5,
                    enabled = !isGenerating,
                    textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = if (isGenerating) MaterialTheme.colorScheme.error
                else if (text.isNotBlank()) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(40.dp)
            ) {
                IconButton(
                    onClick = { if (isGenerating) onStop() else onSend() },
                    enabled = text.isNotBlank() || isGenerating
                ) {
                    Icon(
                        if (isGenerating) Icons.Filled.Stop else Icons.Filled.Send,
                        contentDescription = if (isGenerating) "停止" else "发送",
                        tint = if (isGenerating || text.isNotBlank()) Color.White
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleProviderPickerSheet(
    providers: List<com.mckai.app.data.llm.ProviderConfig>,
    selected: com.mckai.app.data.llm.ProviderConfig?,
    onSelect: (com.mckai.app.data.llm.ProviderConfig) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "选择模型",
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            providers.filter { it.enabled }.forEach { provider ->
                provider.models.forEach { model ->
                    val isSelected = selected?.id == provider.id && selected?.defaultModel == model
                    ListItem(
                        headlineContent = {
                            Text(model, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                        },
                        supportingContent = { Text(provider.name, fontSize = 13.sp) },
                        leadingContent = {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelect(provider.copy(defaultModel = model)) }
                            )
                        },
                        modifier = Modifier.clickable { onSelect(provider.copy(defaultModel = model)) },
                        colors = ListItemDefaults.colors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            else Color.Transparent
                        )
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
