package com.mckai.app.ui.projects

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mckai.app.ui.components.AppleCard
import com.mckai.app.ui.components.AppleEmptyState
import com.mckai.app.ui.components.AppleLargeTitle
import com.mckai.app.ui.components.AppleRow

@Composable
fun ProjectsScreen(
    onOpenProject: (Long) -> Unit,
    viewModel: ProjectsViewModel = viewModel()
) {
    val projects by viewModel.projects.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppleLargeTitle(
            title = "项目",
            subtitle = "${projects.size} 个模组项目",
            actions = {
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(
                        Icons.Filled.AddCircle,
                        "从模板创建项目",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )

        if (projects.isEmpty()) {
            AppleEmptyState(
                icon = Icons.Filled.Folder,
                title = "暂无项目",
                subtitle = "去工坊生成你的第一个模组项目"
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(projects, key = { it.id }) { project ->
                    AppleCard {
                        AppleRow(
                            title = project.name,
                            subtitle = "${project.edition} | ${project.mcVersion}",
                            icon = Icons.Filled.Folder,
                            iconTint = Color(0xFFFF9500),
                            trailing = {
                                IconButton(
                                    onClick = { viewModel.deleteProject(project) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        "删除",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            onClick = { onOpenProject(project.id) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateProjectDialog(
            templates = viewModel.templates,
            onDismiss = { showCreateDialog = false },
            onConfirm = { templateId, name ->
                showCreateDialog = false
                viewModel.createFromTemplate(templateId, name, onCreated = onOpenProject)
            }
        )
    }
}

/** 从模板创建项目对话框（Operit 9 种项目模板）。 */
@Composable
private fun CreateProjectDialog(
    templates: List<com.mckai.app.data.templates.ProjectTemplateRepository.TemplateMeta>,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf(templates.firstOrNull()?.id ?: "") }
    var menuOpen by remember { mutableStateOf(false) }
    val selectedLabel = templates.firstOrNull { it.id == selectedTemplate }?.label ?: "请选择"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("从模板创建项目", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("项目名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Box {
                    OutlinedButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedLabel, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                        Icon(Icons.Filled.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        templates.forEach { t ->
                            DropdownMenuItem(
                                text = { Text("${t.label}（${t.fileCount} 文件）") },
                                onClick = { selectedTemplate = t.id; menuOpen = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedTemplate, name.trim()) },
                enabled = name.isNotBlank() && selectedTemplate.isNotBlank()
            ) { Text("创建") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}