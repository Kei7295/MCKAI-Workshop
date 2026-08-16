package com.mckai.app.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.mckai.app.ui.components.AppleAlertDialog
import com.mckai.app.ui.components.AppleEmptyState
import com.mckai.app.ui.components.AppleField
import com.mckai.app.ui.components.AppleLargeTitle
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatListScreen(
    onNewChat: () -> Unit,
    onOpenChat: (Long) -> Unit,
    viewModel: ChatListViewModel = viewModel()
) {
    val conversations by viewModel.conversations.collectAsState()
    val backupMessage by viewModel.backupMessage.collectAsState()
    var showRenameDialog by remember { mutableStateOf<Pair<Long, String>?>(null) }
    val context = LocalContext.current

    // 导入备份：读取 JSON → 恢复为全新会话
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        if (text != null) viewModel.importConversation(text)
    }

    // 导出备份：先取目标 URI，再从库中序列化该会话
    var pendingExport by remember { mutableStateOf<Pair<Long, String>?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val pending = pendingExport ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            viewModel.exportConversation(pending.first) { json ->
                if (json != null) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppleLargeTitle(
            title = "对话",
            subtitle = "${conversations.size} 个会话",
            actions = {
                IconButton(onClick = { importLauncher.launch("*/*") }) {
                    Icon(
                        Icons.Filled.FileUpload,
                        "导入对话备份",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )

        backupMessage?.let { msg ->
            Text(
                msg,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
            )
            LaunchedEffect(msg) { kotlinx.coroutines.delay(3000); viewModel.clearBackupMessage() }
        }
        // New chat action (replaces FAB)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .clickable(onClick = onNewChat)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "新对话",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
            Spacer(Modifier.weight(1f))
        }

        if (conversations.isEmpty()) {
            AppleEmptyState(
                icon = Icons.Filled.Chat,
                title = "暂无对话",
                subtitle = "创建一条新对话，开始与 AI 协作"
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(conversations, key = { it.id }) { conv ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenChat(conv.id) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Chat,
                                null,
                                modifier = Modifier.size(21.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                conv.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                            Text(
                                SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(conv.updatedAt)),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                pendingExport = conv.id to conv.title
                                exportLauncher.launch("对话-${conv.title}.json")
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Filled.FileDownload, "导出备份", modifier = Modifier.size(17.dp))
                        }
                        IconButton(
                            onClick = { showRenameDialog = conv.id to conv.title },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Filled.Edit, "重命名", modifier = Modifier.size(17.dp))
                        }
                        IconButton(
                            onClick = { viewModel.deleteConversation(conv) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                "删除",
                                modifier = Modifier.size(17.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 76.dp, end = 20.dp)
                            .height(0.5.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    )
                }
            }
        }
    }

    showRenameDialog?.let { (id, currentTitle) ->
        var newName by remember { mutableStateOf(currentTitle) }
        AppleAlertDialog(
            title = "重命名对话",
            confirmText = "确定",
            onConfirm = {
                viewModel.renameConversation(id, newName)
                showRenameDialog = null
            },
            onDismiss = { showRenameDialog = null }
        ) {
            Spacer(Modifier.height(12.dp))
            AppleField(
                value = newName,
                onValueChange = { newName = it },
                placeholder = "名称"
            )
        }
    }
}