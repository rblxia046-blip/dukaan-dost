package com.burhannexus.dukaandost.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.burhannexus.dukaandost.ui.common.ModuleNotBuiltScreen
import com.burhannexus.dukaandost.ui.dashboard.DashboardScreen
import com.burhannexus.dukaandost.ui.settings.SettingsScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Dashboard.route,
        modifier = modifier
    ) {
        composable(AppDestination.Dashboard.route) { DashboardScreen(contentPadding) }
        composable(AppDestination.Settings.route) { SettingsScreen(contentPadding) }
        AppDestination.entries
            .filter { it != AppDestination.Dashboard && it != AppDestination.Settings }
            .forEach { destination ->
                composable(destination.route) { ModuleNotBuiltScreen(destination, contentPadding) }
            }
    }
}
