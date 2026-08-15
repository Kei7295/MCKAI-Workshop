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

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppleLargeTitle(
            title = "项目",
            subtitle = "${projects.size} 个模组项目"
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
}