package com.mckai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mckai.app.ui.components.AppleEasing
import com.mckai.app.ui.navigation.AppNavGraph
import com.mckai.app.ui.navigation.Screen
import com.mckai.app.ui.navigation.bottomTabs
import com.mckai.app.ui.theme.MCKAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MCKAITheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomTabs.map { it.route }
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                AppleBottomBar(
                    tabs = bottomTabs,
                    currentRoute = currentRoute,
                    onTabClick = { tab ->
                        if (currentRoute != tab.route) {
                            navController.navigate(tab.route) {
                                popUpTo(Screen.ChatList.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    isDark = isDark
                )
            }
        }
    ) { padding ->
        AppNavGraph(navController, Modifier.padding(padding))
    }
}

@Composable
fun AppleBottomBar(
    tabs: List<com.mckai.app.ui.navigation.TabItem>,
    currentRoute: String?,
    onTabClick: (com.mckai.app.ui.navigation.TabItem) -> Unit,
    isDark: Boolean
) {
    val bgColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF9F9F9)
    val separatorColor = if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor.copy(alpha = 0.92f))
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Top separator line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(separatorColor)
                .align(Alignment.TopCenter)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                val tintColor = animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.primary
                    else Color(0xFF8E8E93),
                    animationSpec = tween(250, easing = AppleEasing),
                    label = "tabTint"
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onTabClick(tab) }
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label,
                        tint = tintColor.value,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = tab.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = tintColor.value
                    )
                }
            }
        }
    }
}
