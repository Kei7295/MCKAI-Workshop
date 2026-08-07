package com.mckai.app.ui.chat

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onNewChat: () -> Unit,
    onOpenChat: (Long) -> Unit,
    viewModel: ChatListViewModel = viewModel()
) {
    val conversations by viewModel.conversations.collectAsState()
    var showRenameDialog by remember { mutableStateOf<Pair<Long, String>?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("MCKAI 对话") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewChat) {
                Icon(Icons.Filled.Add, "新建对话")
            }
        }
    ) { padding ->
        if (conversations.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Chat, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("暂无对话", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("点击右下角按钮开始新对话", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                Modifier.padding(padding),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(conversations, key = { it.id }) { conv ->
                    ListItem(
                        headlineContent = { Text(conv.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = {
                            Text(
                                SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(conv.updatedAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingContent = { Icon(Icons.Filled.Chat, null) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { showRenameDialog = conv.id to conv.title }, Modifier.size(32.dp)) {
                                    Icon(Icons.Filled.Edit, "重命名", Modifier.size(18.dp))
                                }
                                IconButton(onClick = { viewModel.deleteConversation(conv) }, Modifier.size(32.dp)) {
                                    Icon(Icons.Filled.Delete, "删除", Modifier.size(18.dp))
                                }
                            }
                        },
                        modifier = Modifier.clickable { onOpenChat(conv.id) }
                    )
                }
            }
        }
    }

    showRenameDialog?.let { (id, currentTitle) ->
        var newName by remember { mutableStateOf(currentTitle) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("重命名对话") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("名称") }
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.renameConversation(id, newName); showRenameDialog = null }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) { Text("取消") }
            }
        )
    }
}
