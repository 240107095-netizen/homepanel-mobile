package com.example.homepanel.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.homepanel.ui.downloads.DownloadsScreen
import com.example.homepanel.ui.library.LibraryScreen
import com.example.homepanel.ui.search.SearchScreen
import com.example.homepanel.ui.season.SeasonScreen
import com.example.homepanel.ui.server.ServerScreen
import com.example.homepanel.ui.sources.SourcesScreen
import com.example.homepanel.ui.theme.tui
import com.example.homepanel.ui.title.TitleScreen

// Корень интерфейса: Scaffold с нижней панелью и NavHost со всеми экранами
@Composable
fun HomepanelApp(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    // Нижняя панель видна только на экранах-вкладках, на вложенных — стрелка назад
    val showBottomBar = TopTab.entries.any { it.route == destination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomBar(
                    isSelected = { tab -> destination?.hierarchy?.any { it.route == tab.route } == true },
                    onSelect = { tab -> navController.navigateToTab(tab) },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LIBRARY,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.LIBRARY) {
                LibraryScreen(onTitleClick = { id -> navController.navigate(Routes.title(id)) })
            }
            composable(
                route = Routes.TITLE,
                arguments = listOf(navArgument(Routes.ARG_ITEM_ID) { type = NavType.StringType }),
            ) {
                // itemId сам попадёт во ViewModel через SavedStateHandle
                TitleScreen(
                    onBack = { navController.navigateUp() },
                    onSeasonClick = { season -> navController.navigate(Routes.season(season.seriesId, season.id)) },
                )
            }
            composable(
                route = Routes.SEASON,
                arguments = listOf(
                    navArgument(Routes.ARG_SERIES_ID) { type = NavType.StringType },
                    navArgument(Routes.ARG_SEASON_ID) { type = NavType.StringType },
                ),
            ) {
                SeasonScreen(onBack = { navController.navigateUp() })
            }
            composable(Routes.DOWNLOADS) { DownloadsScreen() }
            composable(Routes.SEARCH) {
                SearchScreen(onOpenSources = { item ->
                    navController.navigate(Routes.sources(item.type.name, item.tmdbId, item.name))
                })
            }
            composable(
                route = Routes.SOURCES,
                arguments = listOf(
                    navArgument(Routes.ARG_TYPE) { type = NavType.StringType },
                    navArgument(Routes.ARG_TMDB_ID) { type = NavType.IntType },
                    navArgument(Routes.ARG_TITLE) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) {
                SourcesScreen(onBack = { navController.navigateUp() })
            }
            composable(Routes.SERVER) { ServerScreen() }
        }
    }
}

// Переход на вкладку: не копим одинаковые экраны в стеке и помним состояние вкладок
private fun NavHostController.navigateToTab(tab: TopTab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomBar(isSelected: (TopTab) -> Boolean, onSelect: (TopTab) -> Unit) {
    Column {
        HorizontalDivider(color = MaterialTheme.tui.line)
        NavigationBar(containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
            TopTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = isSelected(tab),
                    onClick = { onSelect(tab) },
                    icon = { Text(tab.glyph, style = MaterialTheme.typography.titleMedium) },
                    label = { Text(stringResource(tab.label), style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.tui.boost,
                        unselectedIconColor = MaterialTheme.tui.muted,
                        unselectedTextColor = MaterialTheme.tui.muted,
                    ),
                )
            }
        }
    }
}

