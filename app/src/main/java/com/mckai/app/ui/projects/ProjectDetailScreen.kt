package com.mckai.app.ui.projects

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mckai.app.domain.workshop.ModEdition
import com.mckai.app.domain.workshop.ModExporter
import com.mckai.app.domain.workshop.ModSpec
import com.mckai.app.domain.workshop.template.ModTemplateType
import com.mckai.app.ui.components.AppleCard
import com.mckai.app.ui.components.AppleNavBar
import com.mckai.app.ui.components.AppleRow
import com.mckai.app.ui.components.AppleSectionHeader
import java.io.File

@Composable
fun ProjectDetailScreen(
    projectId: Long,
    onBack: () -> Unit,
    onOpenFile: (Long) -> Unit,
    viewModel: ProjectDetailViewModel = viewModel()
) {
    LaunchedEffect(projectId) { viewModel.load(projectId) }
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var exportError by remember { mutableStateOf<String?>(null) }
    var showTemplateDialog by remember { mutableStateOf(false) }

    if (showTemplateDialog) {
        TemplateDialog(
            onDismiss = { showTemplateDialog = false },
            onConfirm = { type, className, displayName ->
                showTemplateDialog = false
                viewModel.generateTemplate(type, className, displayName)
            }
        )
    }

    LaunchedEffect(state.message) {
        if (state.message != null) {
            viewModel.clearMessage()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppleNavBar(
            title = state.project?.name ?: "项目",
            onBack = onBack,
            actions = {
                IconButton(
                    onClick = { showTemplateDialog = true },
                    enabled = state.project != null
                ) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        "添加模板",
                        tint = if (state.project != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = {
                        exportError = null
                        val project = state.project ?: return@IconButton
                        if (state.files.isEmpty()) {
                            exportError = "项目没有可导出的文件"
                            return@IconButton
                        }
                        // 生成 ZIP 并走系统分享
                        val edition = runCatching { ModEdition.valueOf(project.edition) }
                            .getOrElse { ModEdition.JAVA_FABRIC }
                        val spec = ModSpec(
                            name = project.name,
                            edition = edition,
                            mcVersion = project.mcVersion,
                            description = project.description ?: "",
                            modId = project.modId ?: ""
                        )
                        val zipBytes = ModExporter.exportZip(spec, state.files.associate { it.filePath to it.content })
                        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
                        val zipFile = File(dir, "${project.name.replace(Regex("[^\\w\\u4e00-\\u9fff-]"), "_")}.zip")
                        zipFile.writeBytes(zipBytes)
                        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "application/zip"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(share, "导出模组项目"))
                    },
                    enabled = state.files.isNotEmpty()
                ) {
                    Icon(
                        Icons.Filled.Share,
                        "导出 ZIP",
                        tint = if (state.files.isNotEmpty()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            exportError?.let { err ->
                item {
                    Text(
                        err,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
            state.message?.let { msg ->
                item {
                    Text(
                        msg,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
            state.project?.let { project ->
                item {
                    AppleCard(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                project.description?.takeIf { it.isNotBlank() } ?: "暂无描述",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${project.edition} | ${project.mcVersion}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            item {
                AppleSectionHeader("文件 (${state.files.size})")
            }
            item {
                if (state.files.isEmpty()) {
                    Box(Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "暂无文件",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    AppleCard(modifier = Modifier.padding(start = 16.dp, end = 16.dp)) {
                        state.files.forEachIndexed { index, file ->
                            AppleRow(
                                title = file.filePath,
                                icon = Icons.Filled.Description,
                                iconTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                showDivider = index < state.files.size - 1,
                                onClick = { onOpenFile(file.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 模板生成对话框（对应 AI 工具 fabric_template_generate 的 UI 版）。 */
@Composable
private fun TemplateDialog(
    onDismiss: () -> Unit,
    onConfirm: (ModTemplateType, String, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(ModTemplateType.ITEM) }
    var className by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var typeMenuOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("生成模组模板", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box {
                    OutlinedButton(
                        onClick = { typeMenuOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedType.label, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                        Icon(Icons.Filled.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = typeMenuOpen, onDismissRequest = { typeMenuOpen = false }) {
                        ModTemplateType.entries.forEach { t ->
                            DropdownMenuItem(
                                text = { Text("${t.label} — ${t.description}") },
                                onClick = { selectedType = t; typeMenuOpen = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it },
                    label = { Text("类名（英文 PascalCase，如 MagicSword）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("显示名（中文，写入 lang 文件）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedType, className.trim(), displayName.trim()) },
                enabled = className.isNotBlank()
            ) { Text("生成") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}