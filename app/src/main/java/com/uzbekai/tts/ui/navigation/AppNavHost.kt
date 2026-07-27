package com.uzbekai.tts.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.uzbekai.tts.ui.MainViewModel
import com.uzbekai.tts.ui.screens.HistoryScreen
import com.uzbekai.tts.ui.screens.HomeScreen
import com.uzbekai.tts.ui.screens.SettingsScreen

private sealed class Destination(val route: String, val label: String) {
    data object Home : Destination("home", "Yaratish")
    data object History : Destination("history", "Tarix")
    data object Settings : Destination("settings", "Sozlamalar")
}

private val bottomDestinations = listOf(Destination.Home, Destination.History, Destination.Settings)

@Composable
fun AppNavHost(viewModel: MainViewModel) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomDestinations.forEach { dest ->
                    val icon = when (dest) {
                        Destination.Home -> Icons.Filled.Mic
                        Destination.History -> Icons.Filled.History
                        Destination.Settings -> Icons.Filled.Settings
                    }
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destination.Home.route) {
                val synthesisState by viewModel.synthesisState.collectAsState()
                HomeScreen(
                    synthesisState = synthesisState,
                    onSynthesize = { text -> viewModel.synthesize(text) }
                )
            }
            composable(Destination.History.route) {
                val history by viewModel.history.collectAsState()
                HistoryScreen(
                    items = history,
                    onDelete = { viewModel.deleteHistoryItem(it) },
                    onClearAll = { viewModel.clearHistory() }
                )
            }
            composable(Destination.Settings.route) {
                val settings by viewModel.settings.collectAsState()
                SettingsScreen(
                    settings = settings,
                    onSpeechRateChange = { viewModel.updateSpeechRate(it) },
                    onVoiceVariationChange = { viewModel.updateVoiceVariation(it) },
                    onThemeModeChange = { viewModel.updateThemeMode(it) }
                )
            }
        }
    }
}
