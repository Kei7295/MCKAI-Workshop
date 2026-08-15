package com.mckai.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mckai.app.ui.components.AppleActionSheet
import com.mckai.app.ui.components.AppleAvatar
import com.mckai.app.ui.components.AppleCard
import com.mckai.app.ui.components.AppleEmptyState
import com.mckai.app.ui.components.AppleNavBar
import com.mckai.app.ui.components.AppleSheetOption

/** 助手列表：RikkaHub Assistant 概念——角色卡片管理页 */
@Composable
fun AssistantListScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: AssistantViewModel = viewModel()
) {
    val assistants by viewModel.assistants.collectAsState()
    val activeId by viewModel.activeId.collectAsState()
    var showActionsFor by remember { mutableStateOf<Long?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppleNavBar(
            title = "助手",
            subtitle = "${assistants.size} 个角色",
            onBack = onBack,
            actions = {
                IconButton(onClick = { onEdit(0L) }) {
                    Icon(Icons.Filled.Add, "新建助手", tint = MaterialTheme.colorScheme.primary)
                }
            }
        )

        if (assistants.isEmpty()) {
            AppleEmptyState(
                icon = Icons.Filled.SmartToy,
                title = "还没有助手",
                subtitle = "点击右上角 + 创建你的第一个角色"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    Text(
                        "内置助手",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                items(assistants.filter { it.isBuiltIn }, key = { it.id }) { assistant ->
                    AssistantRow(
                        assistant = assistant,
                        isActive = activeId == assistant.id,
                        onClick = { viewModel.setActive(assistant.id) },
                        onMore = { showActionsFor = assistant.id }
                    )
                }
                item {
                    Text(
                        "自定义助手",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                items(assistants.filter { !it.isBuiltIn }, key = { it.id }) { assistant ->
                    AssistantRow(
                        assistant = assistant,
                        isActive = activeId == assistant.id,
                        onClick = { viewModel.setActive(assistant.id) },
                        onMore = { showActionsFor = assistant.id }
                    )
                }
            }
        }
    }

    showActionsFor?.let { id ->
        val target = assistants.firstOrNull { it.id == id } ?: return@let
        AppleActionSheet(
            title = target.name,
            options = listOf(
                AppleSheetOption(
                    label = if (activeId == id) "✓ 正在使用" else "设为当前助手",
                    bold = activeId == id,
                    onClick = { viewModel.setActive(id) }
                ),
                AppleSheetOption(
                    label = "编辑",
                    onClick = { onEdit(id) }
                ),
                AppleSheetOption(
                    label = if (target.isBuiltIn) "内置助手不可删除" else "删除",
                    destructive = !target.isBuiltIn,
                    onClick = { if (!target.isBuiltIn) viewModel.delete(id, target.isBuiltIn) }
                )
            ),
            onDismiss = { showActionsFor = null }
        )
    }
}

@Composable
private fun AssistantRow(
    assistant: com.mckai.app.data.db.entity.AssistantEntity,
    isActive: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit
) {
    AppleCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppleAvatar(
                name = assistant.name,
                avatar = assistant.avatar,
                size = 42.dp,
                fontSize = 17.sp
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(assistant.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    if (isActive) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("使用中", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                assistant.description?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            IconButton(onClick = onMore) {
                Icon(
                    Icons.Filled.MoreHoriz,
                    "更多",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}