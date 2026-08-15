package com.mckai.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mckai.app.ui.components.AppleCard
import com.mckai.app.ui.components.AppleNavBar
import com.mckai.app.ui.components.AppleRow

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppleNavBar(title = "关于", onBack = onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(12.dp))

            Text("MCKAI 模组工坊", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "v1.0.0",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            AppleCard {
                Column(Modifier.padding(16.dp)) {
                    Text("项目简介", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "MCKAI 是一个融合了 RikkaHub、Operit、ModCrafting 三个应用功能的 AI 模组开发助手。" +
                        "支持多种大语言模型提供商，提供 AI Agent 工具调用、Minecraft 模组工坊、项目管理等功能。",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            AppleCard {
                Column {
                    Text(
                        "功能特性",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(16.dp)
                    )
                    val features = listOf(
                        "多提供商 LLM 客户端",
                        "AI Agent 工具调用 · 25+ 内置工具",
                        "Minecraft 模组工坊",
                        "项目管理与文件编辑",
                        "流式响应与 Markdown 渲染"
                    )
                    features.forEachIndexed { index, feature ->
                        Text(
                            "·  $feature",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        if (index < features.size - 1) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, top = 10.dp)
                                    .height(0.5.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            AppleCard {
                AppleRow(
                    title = "技术栈",
                    subtitle = "Kotlin · Compose · Room · OkHttp",
                    icon = Icons.Filled.Code,
                    iconTint = MaterialTheme.colorScheme.primary
                ) {
                    // 无跳转，展示用
                }
            }

            Spacer(Modifier.height(12.dp))

            AppleCard {
                Column(Modifier.padding(16.dp)) {
                    Text("开源协议", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "本项目采用 MIT License 开源协议。",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "github.com/Kei7295/MCKAI-Workshop",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}