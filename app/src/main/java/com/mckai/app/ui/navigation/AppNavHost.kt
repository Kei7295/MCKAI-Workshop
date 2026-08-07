package com.mckai.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object ChatList : Screen("chatlist")
    object Chat : Screen("chat/{convId}") {
        fun createRoute(convId: Long) = "chat/$convId"
    }
    object NewChat : Screen("chat/new")
    object Workshop : Screen("workshop")
    object Projects : Screen("projects")
    object ProjectDetail : Screen("project/{projectId}") {
        fun createRoute(projectId: Long) = "project/$projectId"
    }
    object FileEditor : Screen("editor/{fileId}") {
        fun createRoute(fileId: Long) = "editor/$fileId"
    }
    object Settings : Screen("settings")
    object ProviderEdit : Screen("provider/{providerId}") {
        fun createRoute(providerId: String) = "provider/$providerId"
    }
    object ProviderNew : Screen("provider/new")
    object Workflow : Screen("workflows")
    object Assistants : Screen("assistants")
    object Memory : Screen("memory")
}

data class TabItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomTabs = listOf(
    TabItem("对话", Screen.ChatList.route, Icons.Filled.Chat, Icons.Outlined.Chat),
    TabItem("工坊", Screen.Workshop.route, Icons.Filled.Build, Icons.Outlined.Build),
    TabItem("项目", Screen.Projects.route, Icons.Filled.Folder, Icons.Outlined.Folder),
    TabItem("设置", Screen.Settings.route, Icons.Filled.Settings, Icons.Outlined.Settings)
)
