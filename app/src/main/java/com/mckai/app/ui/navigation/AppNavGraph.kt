package com.mckai.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mckai.app.ui.chat.ChatListScreen
import com.mckai.app.ui.chat.ChatScreen
import com.mckai.app.ui.workshop.WorkshopScreen
import com.mckai.app.ui.projects.ProjectsScreen
import com.mckai.app.ui.projects.ProjectDetailScreen
import com.mckai.app.ui.projects.FileEditorScreen
import com.mckai.app.ui.settings.SettingsScreen
import com.mckai.app.ui.settings.ProviderEditScreen
import com.mckai.app.ui.settings.AboutScreen

@Composable
fun AppNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = Screen.ChatList.route, modifier = modifier) {
        composable(Screen.ChatList.route) {
            ChatListScreen(
                onNewChat = { navController.navigate(Screen.NewChat.route) },
                onOpenChat = { id -> navController.navigate(Screen.Chat.createRoute(id)) }
            )
        }
        composable(Screen.NewChat.route) {
            ChatScreen(
                convId = null,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.Chat.route,
            arguments = listOf(navArgument("convId") { type = NavType.LongType })
        ) {
            ChatScreen(
                convId = it.arguments?.getLong("convId"),
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Workshop.route) {
            WorkshopScreen()
        }
        composable(Screen.Projects.route) {
            ProjectsScreen(
                onOpenProject = { id -> navController.navigate(Screen.ProjectDetail.createRoute(id)) }
            )
        }
        composable(
            Screen.ProjectDetail.route,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) {
            ProjectDetailScreen(
                projectId = it.arguments?.getLong("projectId") ?: 0,
                onBack = { navController.popBackStack() },
                onOpenFile = { fileId -> navController.navigate(Screen.FileEditor.createRoute(fileId)) }
            )
        }
        composable(
            Screen.FileEditor.route,
            arguments = listOf(navArgument("fileId") { type = NavType.LongType })
        ) {
            FileEditorScreen(
                fileId = it.arguments?.getLong("fileId") ?: 0,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onEditProvider = { id -> navController.navigate(Screen.ProviderEdit.createRoute(id)) },
                onNewProvider = { navController.navigate(Screen.ProviderNew.route) },
                onAbout = { navController.navigate(Screen.About.route) }
            )
        }
        composable(
            Screen.ProviderEdit.route,
            arguments = listOf(navArgument("providerId") { type = NavType.StringType })
        ) {
            ProviderEditScreen(
                providerId = it.arguments?.getString("providerId") ?: "",
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ProviderNew.route) {
            ProviderEditScreen(
                providerId = null,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
