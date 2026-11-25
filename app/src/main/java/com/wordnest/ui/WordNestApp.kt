package com.wordnest.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wordnest.ui.decks.ComingSoonScreen
import com.wordnest.ui.home.HomeScreen
import com.wordnest.ui.settings.SettingsScreen
import com.wordnest.ui.study.StatisticsScreenContent
import com.wordnest.ui.study.StudyFlowScreen
import com.wordnest.viewmodel.StudyViewModel
import com.wordnest.ui.ai.GeminiChatScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material.icons.filled.ChatBubble
private enum class TopLevelRoute(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Trang chủ", Icons.Filled.Home),
    Decks("decks", "Các Tủ", Icons.Filled.Folder),
    Statistics("statistics", "Thống kê", Icons.Filled.BarChart),
    Settings("settings", "Cài đặt", Icons.Filled.Settings),
}

private const val STUDY_ROUTE = "study"
private const val AI_ROUTE = "ai_chat"
@Composable
fun WordNestApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,

        floatingActionButton = {
            if (currentDestination?.route == TopLevelRoute.Home.route) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate(AI_ROUTE) {
                            launchSingleTop = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChatBubble,
                        contentDescription = "Mở WordNest AI"
                    )
                }
            }
        },


        bottomBar = {
            if (currentDestination?.route == TopLevelRoute.Home.route) { //vừa sửa
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    TopLevelRoute.entries.forEach { item ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any {
                                it.route == item.route
                            } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                }
                            },
                            icon = {
                                Icon(item.icon, contentDescription = item.label)
                            },
                            label = {
                                Text(item.label)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelRoute.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TopLevelRoute.Home.route) {
                HomeScreen(
                    onStudyClick = {
                        navController.navigate(STUDY_ROUTE)
                    },
                    onAiClick = {
                        navController.navigate(AI_ROUTE)
                    }
                )
            }
            composable(TopLevelRoute.Decks.route) { ComingSoonScreen() }
            composable(TopLevelRoute.Statistics.route) {
                // Thống kê dùng dữ liệu đàn từ hiện tại (F16); tạo ViewModel riêng
                // cho tab để không đụng phiên học đang dở của route "study".
                val statsViewModel: StudyViewModel = viewModel()
                val words by statsViewModel.words.collectAsStateWithLifecycle()
                StatisticsScreenContent(words = words, onBack = null)
            }
            composable(TopLevelRoute.Settings.route) { SettingsScreen() }
            composable(STUDY_ROUTE) {
                val studyViewModel: StudyViewModel = viewModel()
                StudyFlowScreen(
                    viewModel = studyViewModel,
                    onExit = { navController.popBackStack() },
                )
            }
            composable(AI_ROUTE) {
                GeminiChatScreen(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
