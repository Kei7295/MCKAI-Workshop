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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mckai.app.data.db.entity.MessageEntity
import com.mckai.app.data.llm.ToolCallSpec
import com.mckai.app.ui.components.AppleActionSheet
import com.mckai.app.ui.components.AppleAlertDialog
import com.mckai.app.ui.components.AppleNavBar
import com.mckai.app.ui.components.AppleSheetOption
import com.mckai.app.ui.components.MarkdownText
import kotlinx.serialization.json.Json
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
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var showProviderPicker by remember { mutableStateOf(false) }
    var actionMsg by remember { mutableStateOf<MessageEntity?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(convId) { viewModel.loadConversation(convId) }

    // 离开页面时停止朗读
    DisposableEffect(Unit) {
        onDispose { TtsManager.stop() }
    }

    // 仅当底部条数变化或生成开始/结束时滚动（流式增量不打断用户浏览）
    val visibleCount = remember(state.messages) { state.messages.count { !it.isHidden } }
    LaunchedEffect(visibleCount, state.isGenerating) {
        val streamingActive = state.streamingText.isNotBlank()
        val target = visibleCount + if (streamingActive) 1 else 0
        if (target > 0) listState.animateScrollToItem(target)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppleNavBar(
            title = "对话",
            subtitle = state.selectedProvider?.let { "${it.name} / ${it.displayModel()}" } ?: "",
            onBack = onBack,
            actions = {
                IconButton(onClick = { showProviderPicker = true }) {
                    Icon(Icons.Filled.SwapHoriz, "切换模型")
                }
                IconButton(onClick = { viewModel.toggleTools() }) {
                    Icon(
                        if (state.toolsEnabled) Icons.Filled.Build else Icons.Outlined.Build,
                        "工具",
                        tint = if (state.toolsEnabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        if (state.sessionTokens > 0) {
            Text(
                "本会话已使用 ${state.sessionTokens.toLong()} tokens",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 4.dp)
            )
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val visibleMessages = state.messages.filter { !it.isHidden }
            if (visibleMessages.isEmpty() && state.streamingText.isBlank()) {
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
            items(visibleMessages, key = { it.id }) { msg ->
                AppleMessageBubble(
                    msg = msg,
                    onMore = { actionMsg = msg }
                )
            }
            if (state.streamingText.isNotBlank()) {
                item {
                    AppleStreamingBubble(state.streamingText, state.reasoningText, state.activeToolCalls)
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
            onSend = { if (viewModel.send(inputText)) inputText = "" },
            onStop = { viewModel.stop() },
            isGenerating = state.isGenerating
        )
    }

    if (showProviderPicker) {
        AppleActionSheet(
            title = "选择模型",
            options = buildList {
                state.providers.filter { it.enabled }.forEach { provider ->
                    provider.models.forEach { model ->
                        val isSelected = state.selectedProvider?.id == provider.id &&
                            state.selectedProvider?.defaultModel == model
                        add(
                            AppleSheetOption(
                                label = "$model  ·  ${provider.name}",
                                bold = isSelected,
                                onClick = { viewModel.selectProvider(provider.copy(defaultModel = model)) }
                            )
                        )
                    }
                }
            },
            onDismiss = { showProviderPicker = false }
        )
    }

    // 消息操作菜单：重新生成 / 朗读 / 删除
    actionMsg?.let { msg ->
        val branchCandidates = state.messages.filter { it.branchGroupId != null && it.branchGroupId == msg.branchGroupId }
        AppleActionSheet(
            title = "消息操作",
            options = buildList {
                add(
                    AppleSheetOption(
                        label = "重新生成回复",
                        bold = true,
                        onClick = { viewModel.regenerate(msg.id) }
                    )
                )
                if (branchCandidates.size > 1) {
                    add(
                        AppleSheetOption(
                            label = "切换到此版本 (${branchCandidates.size} 个候选)",
                            onClick = { viewModel.switchBranch(msg.id) }
                        )
                    )
                }
                add(
                    AppleSheetOption(
                        label = "朗读回复",
                        onClick = {
                            TtsManager.speak(context, msg.content)
                        }
                    )
                )
                add(
                    AppleSheetOption(
                        label = "删除消息",
                        destructive = true,
                        onClick = { viewModel.deleteMessage(msg.id) }
                    )
                )
            },
            onDismiss = { actionMsg = null }
        )
    }

    // 敏感工具确认弹窗
    state.pendingToolApproval?.let { approval ->
        AppleAlertDialog(
            title = "允许工具调用?",
            message = "工具「${approval.toolName}」请求执行\n参数：${approval.argsSummary.ifBlank { "(无)" }}",
            confirmText = "允许",
            dismissText = "拒绝",
            onConfirm = { viewModel.approveTool() },
            onDismiss = { viewModel.rejectTool() }
        )
    }
}

@Composable
fun AppleMessageBubble(msg: MessageEntity, onMore: () -> Unit) {
    val isUser = msg.role == "user"
    val isHidden = msg.isHidden
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isUser) Color.White
    else MaterialTheme.colorScheme.onSurface

    // 隐藏消息折叠为灰条
    if (isHidden) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("此回复已重新生成", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("↻", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

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
            modifier = Modifier.widthIn(max = 300.dp)
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
                msg.toolCallsJson?.let { raw ->
                    val calls = runCatching {
                        Json { ignoreUnknownKeys = true }.decodeFromString<List<ToolCallSpec>>(raw)
                    }.getOrNull().orEmpty()
                    if (calls.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        calls.forEach { tc ->
                            ToolCallCard(tc.name, tc.args)
                        }
                    }
                }
                // token 角标 + 操作菜单（assistant 气泡）
                if (!isUser) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            timeText(msg.createdAt),
                            fontSize = 10.sp,
                            color = textColor.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.weight(1f))
                        val tokens = (msg.promptTokens ?: 0) + (msg.completionTokens ?: 0)
                        if (tokens > 0) {
                            Text(
                                "$tokens tok",
                                fontSize = 10.sp,
                                color = textColor.copy(alpha = 0.4f),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        Icon(
                            Icons.Filled.MoreHoriz,
                            contentDescription = "更多",
                            tint = textColor.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .clickable { onMore() }
                        )
                    }
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
private fun ToolCallCard(name: String, args: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Extension,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(6.dp))
            Column {
                Text(name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                if (args.isNotBlank() && args != "{}" && args.length <= 80) {
                    Text(args, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private fun timeText(ts: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))

@Composable
fun AppleStreamingBubble(text: String, reasoning: String, toolCalls: List<String>) {
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
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                MarkdownText(text + "▌")
                if (toolCalls.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    toolCalls.distinct().forEach { name ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "调用工具: $name",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
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