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
import com.mckai.app.ui.components.AppleCard
import com.mckai.app.ui.components.AppleLargeTitle
import com.mckai.app.ui.components.AppleRow
import com.mckai.app.ui.components.AppleSectionHeader
import com.mckai.app.ui.components.AppleSegmented

@Composable
fun SettingsScreen(
    onEditProvider: (String) -> Unit,
    onNewProvider: () -> Unit,
    onAbout: () -> Unit = {},
    onAssistants: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    val providers by viewModel.providers.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val memoryEnabled by viewModel.memoryEnabled.collectAsState()
    val autoApprove by viewModel.autoApproveSensitive.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppleLargeTitle(
            title = "设置",
            subtitle = "McKAI 模组工坊"
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Appearance Section
            item {
                AppleSectionHeader("外观")
                AppleCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        "主题",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 14.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    AppleSegmented(
                        options = listOf(
                            "system" to "自动",
                            "light" to "浅色",
                            "dark" to "深色"
                        ),
                        selected = themeMode,
                        onSelect = { viewModel.setTheme(it) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                }
            }

            // AI Models Section
            item {
                AppleSectionHeader("AI 模型")
                AppleCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    providers.forEachIndexed { index, provider ->
                        AppleRow(
                            title = provider.name,
                            subtitle = "${provider.type.label} / ${provider.displayModel()}",
                            icon = Icons.Filled.SmartToy,
                            iconTint = MaterialTheme.colorScheme.primary,
                            showDivider = index < providers.size - 1,
                            onClick = { onEditProvider(provider.id) }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNewProvider() }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "添加模型",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // Features Section
            item {
                AppleSectionHeader("功能")
                AppleCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    AppleRow(
                        title = "助手",
                        subtitle = "角色管理与系统提示词",
                        icon = Icons.Filled.SmartToy,
                        iconTint = MaterialTheme.colorScheme.primary,
                        showDivider = true,
                        onClick = onAssistants
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF007AFF).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp),
                                tint = Color(0xFF007AFF)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("敏感工具自动执行", modifier = Modifier.weight(1f), fontSize = 16.sp)
                        Switch(
                            checked = autoApprove,
                            onCheckedChange = { viewModel.setAutoApproveSensitive(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF34C759)
                            )
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF34C759).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Memory,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp),
                                tint = Color(0xFF34C759)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("记忆功能", modifier = Modifier.weight(1f), fontSize = 16.sp)
                        Switch(
                            checked = memoryEnabled,
                            onCheckedChange = { viewModel.setMemoryEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF34C759)
                            )
                        )
                    }
                }
            }

            // About Section
            item {
                AppleSectionHeader("关于")
                AppleCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    AppleRow(
                        title = "MCKAI 模组工坊",
                        subtitle = "v1.0.0 · 开源免费",
                        icon = Icons.Filled.Info,
                        iconTint = Color(0xFF007AFF),
                        onClick = onAbout
                    )
                }
            }
        }
    }
}