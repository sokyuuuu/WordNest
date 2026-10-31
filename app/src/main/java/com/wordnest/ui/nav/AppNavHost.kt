package com.wordnest.ui.nav

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wordnest.di.AppContainer
import com.wordnest.ui.screens.AddWordScreen
import com.wordnest.ui.screens.DeckDetailScreen
import com.wordnest.ui.screens.DeckEditScreen
import com.wordnest.ui.screens.DeckListScreen
import com.wordnest.ui.screens.DetailStreakScreen
import com.wordnest.ui.screens.HomeScreen
import com.wordnest.ui.screens.SearchScreen
import com.wordnest.ui.screens.SettingsScreen
import com.wordnest.ui.screens.StatsPlaceholderScreen
import com.wordnest.ui.screens.StudyComingSoonScreen
import com.wordnest.ui.screens.WordEditScreen
import com.wordnest.ui.theme.CardWhite
import com.wordnest.ui.theme.CreamBackground
import com.wordnest.ui.theme.ForestGreen
import com.wordnest.ui.theme.ForestGreenLight
import com.wordnest.ui.theme.TextSecondary
import com.wordnest.ui.viewmodel.AddWordViewModel
import com.wordnest.ui.viewmodel.DeckViewModel
import com.wordnest.ui.viewmodel.HomeViewModel
import com.wordnest.ui.viewmodel.SearchViewModel
import com.wordnest.ui.viewmodel.SettingsViewModel
import com.wordnest.ui.viewmodel.ViewModelFactories
import com.wordnest.ui.viewmodel.WordViewModel

object Routes {
    const val HOME = "home"
    const val DECKS = "decks"
    const val STATS = "stats"
    const val SETTINGS = "settings"

    const val SEARCH = "search"
    const val DECK_CREATE = "deck/create"
    const val DECK_EDIT = "deck/edit/{deckId}"
    const val DECK_DETAIL = "deck/detail/{deckId}"
    const val WORD_ADD = "deck/{deckId}/word/add"
    const val WORD_EDIT = "deck/{deckId}/word/edit/{wordId}"
    const val STUDY_COMING_SOON = "study/coming-soon?mode={mode}"
    const val DETAIL_STREAK = "streak"

    fun deckEdit(deckId: String) = "deck/edit/$deckId"
    fun deckDetail(deckId: String) = "deck/detail/$deckId"
    fun wordAdd(deckId: String) = "deck/$deckId/word/add"
    fun wordEdit(deckId: String, wordId: String) = "deck/$deckId/word/edit/$wordId"
    fun studyComingSoon(mode: String = "Ôn tập") =
        "study/coming-soon?mode=${Uri.encode(mode)}"
}

private data class BottomNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val BOTTOM_TABS = listOf(
    BottomNavTab(Routes.HOME, "Trang chủ", Icons.Default.Home),
    BottomNavTab(Routes.DECKS, "Tổ từ", Icons.Default.Folder),
    BottomNavTab(Routes.STATS, "Thống kê", Icons.Default.BarChart),
    BottomNavTab(Routes.SETTINGS, "Cài đặt", Icons.Default.Settings)
)

@Composable
fun AppNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val deckViewModel: DeckViewModel = viewModel(factory = ViewModelFactories.deck(container))
    val homeViewModel: HomeViewModel = viewModel(factory = ViewModelFactories.home(container))
    val settingsViewModel: SettingsViewModel = viewModel(factory = ViewModelFactories.settings(container))

    val decks by deckViewModel.decks.collectAsStateWithLifecycle()

    val showBottomBar = currentRoute in BOTTOM_TABS.map { it.route }

    Scaffold(
        containerColor = CreamBackground,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = CardWhite) {
                    BOTTOM_TABS.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ForestGreen,
                                selectedTextColor = ForestGreen,
                                indicatorColor = ForestGreenLight,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    viewModel = homeViewModel,
                    decks = decks,
                    onOpenDecksTab = {
                        navController.navigate(Routes.DECKS) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenStreak = {
                        navController.navigate(Routes.DETAIL_STREAK)
                    }
                )
            }

            composable(Routes.DETAIL_STREAK) {
                DetailStreakScreen(
                    viewModel = homeViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.DECKS) {
                DeckListScreen(
                    viewModel = deckViewModel,
                    onOpenDeck = { navController.navigate(Routes.deckDetail(it)) },
                    onCreateDeck = { navController.navigate(Routes.DECK_CREATE) },
                    onEditDeck = { navController.navigate(Routes.deckEdit(it)) },
                    onOpenSearch = { navController.navigate(Routes.SEARCH) }
                )
            }

            composable(Routes.STATS) {
                StatsPlaceholderScreen(decks = decks)
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(viewModel = settingsViewModel)
            }

            composable(Routes.SEARCH) {
                val searchVm: SearchViewModel = viewModel(
                    key = "search_vm",
                    factory = ViewModelFactories.search(container)
                )
                SearchScreen(
                    searchViewModel = searchVm,
                    deckViewModel = deckViewModel,
                    wordRepository = container.wordRepository,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenDeck = { navController.navigate(Routes.deckDetail(it)) },
                    onEditDeck = { navController.navigate(Routes.deckEdit(it)) },
                    onEditWord = { deckId, wordId ->
                        navController.navigate(Routes.wordEdit(deckId, wordId))
                    }
                )
            }

            composable(Routes.DECK_CREATE) {
                DeckEditScreen(
                    deckId = null,
                    viewModel = deckViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onDeckDeleted = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.DECK_EDIT,
                arguments = listOf(navArgument("deckId") { type = NavType.StringType })
            ) { entry ->
                val deckId = entry.arguments?.getString("deckId")
                DeckEditScreen(
                    deckId = deckId,
                    viewModel = deckViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onDeckDeleted = {
                        navController.popBackStack(Routes.DECKS, inclusive = false)
                    }
                )
            }

            composable(
                route = Routes.DECK_DETAIL,
                arguments = listOf(navArgument("deckId") { type = NavType.StringType })
            ) { entry ->
                val deckId = entry.arguments?.getString("deckId") ?: return@composable
                val wordVm: WordViewModel = viewModel(
                    key = "word_vm_$deckId",
                    factory = ViewModelFactories.word(container, deckId)
                )
                DeckDetailScreen(
                    deckId = deckId,
                    deckViewModel = deckViewModel,
                    wordViewModel = wordVm,
                    onNavigateBack = { navController.popBackStack() },
                    onEditDeck = { navController.navigate(Routes.deckEdit(it)) },
                    onAddWord = { navController.navigate(Routes.wordAdd(it)) },
                    onEditWord = { dId, wId -> navController.navigate(Routes.wordEdit(dId, wId)) },
                    onOpenStudyMode = { mode ->
                        navController.navigate(Routes.studyComingSoon(mode))
                    }
                )
            }

            composable(
                route = Routes.WORD_ADD,
                arguments = listOf(navArgument("deckId") { type = NavType.StringType })
            ) { entry ->
                val deckId = entry.arguments?.getString("deckId") ?: return@composable
                val addWordVm: AddWordViewModel = viewModel(
                    key = "add_word_vm_$deckId",
                    factory = ViewModelFactories.addWord(container, deckId)
                )
                AddWordScreen(
                    viewModel = addWordVm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.WORD_EDIT,
                arguments = listOf(
                    navArgument("deckId") { type = NavType.StringType },
                    navArgument("wordId") { type = NavType.StringType }
                )
            ) { entry ->
                val deckId = entry.arguments?.getString("deckId") ?: return@composable
                val wordId = entry.arguments?.getString("wordId") ?: return@composable
                val wordVm: WordViewModel = viewModel(
                    key = "word_vm_$deckId",
                    factory = ViewModelFactories.word(container, deckId)
                )
                WordEditScreen(
                    wordId = wordId,
                    wordViewModel = wordVm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.STUDY_COMING_SOON,
                arguments = listOf(
                    navArgument("mode") {
                        type = NavType.StringType
                        defaultValue = "Ôn tập"
                    }
                )
            ) { entry ->
                val mode = entry.arguments?.getString("mode") ?: "Ôn tập"
                StudyComingSoonScreen(
                    title = mode,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
