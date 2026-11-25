package com.wordnest.ui

import android.app.Application
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wordnest.di.appContainer
import com.wordnest.ui.decks.DeckDetailScreen
import com.wordnest.ui.decks.DeckDetailViewModel
import com.wordnest.ui.decks.DeckDetailViewModelFactory
import com.wordnest.ui.decks.DeckFormScreen
import com.wordnest.ui.decks.DeckListScreen
import com.wordnest.ui.decks.DeckViewModel
import com.wordnest.ui.decks.DeckViewModelFactory
import com.wordnest.ui.decks.SearchScreen
import com.wordnest.ui.decks.SearchViewModel
import com.wordnest.ui.decks.SearchViewModelFactory
import com.wordnest.ui.decks.WordFormScreen
import com.wordnest.ui.decks.WordFormViewModel
import com.wordnest.ui.decks.WordFormViewModelFactory
import com.wordnest.ui.home.HomeScreen
import com.wordnest.ui.settings.SettingsScreen
import com.wordnest.ui.study.StatisticsScreenContent
import com.wordnest.ui.study.StudyFlowScreen
import com.wordnest.viewmodel.StudyViewModel

private enum class TopLevelRoute(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Trang chủ", Icons.Filled.Home),
    Decks("decks", "Các Tủ", Icons.Filled.Folder),
    Statistics("statistics", "Thống kê", Icons.Filled.BarChart),
    Settings("settings", "Cài đặt", Icons.Filled.Settings),
}

private const val STUDY_ROUTE = "study"
private const val DECK_NEW_ROUTE = "deck/new"
private const val DECK_DETAIL_ROUTE = "deck/{deckId}"
private const val DECK_EDIT_ROUTE = "deck/{deckId}/edit"
private const val WORD_NEW_ROUTE = "deck/{deckId}/word/new"
private const val WORD_EDIT_ROUTE = "deck/{deckId}/word/{wordId}/edit"
private const val SEARCH_ROUTE = "search"

private fun deckDetailRoute(deckId: String): String = "deck/$deckId"
private fun deckEditRoute(deckId: String): String = "deck/$deckId/edit"
private fun wordNewRoute(deckId: String): String = "deck/$deckId/word/new"
private fun wordEditRoute(deckId: String, wordId: String): String = "deck/$deckId/word/$wordId/edit"

@Composable
fun WordNestApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val app = LocalContext.current.applicationContext as Application
    val deckViewModel: DeckViewModel = viewModel(
        factory = remember(app) { DeckViewModelFactory(app.appContainer().deckRepository) },
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                TopLevelRoute.entries.forEach { item ->
                    val isSelected = when (item) {
                        TopLevelRoute.Decks -> currentDestination?.hierarchy?.any {
                            it.route == item.route ||
                                it.route?.startsWith("deck/") == true ||
                                it.route == SEARCH_ROUTE
                        } == true
                        else -> currentDestination?.hierarchy?.any { it.route == item.route } == true
                    }
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            // Lưu ý: KHÔNG dùng restoreState=true — với Navigation hiện tại,
                            // navigate() về destination start (Trang chủ) bị ngắt lặng lẽ khi
                            // đang ở màn học (F17-US: bấm tab không phản ứng).
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    )
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
                    onStudyClick = { navController.navigate(STUDY_ROUTE) },
                    onSeeAllDecks = {
                        navController.navigate(TopLevelRoute.Decks.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onOpenDeck = { deckId -> navController.navigate(deckDetailRoute(deckId)) },
                    onCreateDeck = { navController.navigate(DECK_NEW_ROUTE) },
                )
            }
            composable(TopLevelRoute.Decks.route) {
                DeckListScreen(
                    viewModel = deckViewModel,
                    onCreateDeck = { navController.navigate(DECK_NEW_ROUTE) },
                    onOpenDeck = { deckId -> navController.navigate(deckDetailRoute(deckId)) },
                    onEditDeck = { deckId -> navController.navigate(deckEditRoute(deckId)) },
                    onOpenSearch = { navController.navigate(SEARCH_ROUTE) },
                )
            }
            composable(SEARCH_ROUTE) {
                val searchViewModel: SearchViewModel = viewModel(
                    factory = remember(app) {
                        SearchViewModelFactory(app.appContainer().wordRepository)
                    },
                )
                SearchScreen(
                    viewModel = searchViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenDeck = { deckId -> navController.navigate(deckDetailRoute(deckId)) },
                )
            }
            composable(DECK_NEW_ROUTE) {
                DeckFormScreen(
                    deckId = null,
                    viewModel = deckViewModel,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(
                route = DECK_DETAIL_ROUTE,
                arguments = listOf(navArgument("deckId") { type = NavType.StringType }),
            ) { entry ->
                val deckId = entry.arguments?.getString("deckId") ?: return@composable
                val detailViewModel: DeckDetailViewModel = viewModel(
                    factory = remember(app, deckId) {
                        val container = app.appContainer()
                        DeckDetailViewModelFactory(
                            deckId = deckId,
                            deckRepository = container.deckRepository,
                            wordRepository = container.wordRepository,
                        )
                    },
                )
                DeckDetailScreen(
                    viewModel = detailViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onEditDeck = { navController.navigate(deckEditRoute(deckId)) },
                    onAddWord = { navController.navigate(wordNewRoute(deckId)) },
                    onEditWord = { wordId -> navController.navigate(wordEditRoute(deckId, wordId)) },
                )
            }
            composable(
                route = DECK_EDIT_ROUTE,
                arguments = listOf(navArgument("deckId") { type = NavType.StringType }),
            ) { entry ->
                val deckId = entry.arguments?.getString("deckId")
                DeckFormScreen(
                    deckId = deckId,
                    viewModel = deckViewModel,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(
                route = WORD_NEW_ROUTE,
                arguments = listOf(navArgument("deckId") { type = NavType.StringType }),
            ) { entry ->
                val deckId = entry.arguments?.getString("deckId") ?: return@composable
                val wordFormViewModel: WordFormViewModel = viewModel(
                    factory = remember(app) {
                        val container = app.appContainer()
                        WordFormViewModelFactory(
                            wordRepository = container.wordRepository,
                            dictionaryRepository = container.dictionaryRepository,
                        )
                    },
                )
                WordFormScreen(
                    deckId = deckId,
                    wordId = null,
                    viewModel = wordFormViewModel,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(
                route = WORD_EDIT_ROUTE,
                arguments = listOf(
                    navArgument("deckId") { type = NavType.StringType },
                    navArgument("wordId") { type = NavType.StringType },
                ),
            ) { entry ->
                val deckId = entry.arguments?.getString("deckId") ?: return@composable
                val wordId = entry.arguments?.getString("wordId") ?: return@composable
                val wordFormViewModel: WordFormViewModel = viewModel(
                    factory = remember(app) {
                        val container = app.appContainer()
                        WordFormViewModelFactory(
                            wordRepository = container.wordRepository,
                            dictionaryRepository = container.dictionaryRepository,
                        )
                    },
                )
                WordFormScreen(
                    deckId = deckId,
                    wordId = wordId,
                    viewModel = wordFormViewModel,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
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
        }
    }
}
