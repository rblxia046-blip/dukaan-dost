package com.burhannexus.dukaandost.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.burhannexus.dukaandost.ui.common.ModuleNotBuiltScreen
import com.burhannexus.dukaandost.ui.dashboard.DashboardScreen
import com.burhannexus.dukaandost.ui.products.ProductDetailScreen
import com.burhannexus.dukaandost.ui.products.ProductEditScreen
import com.burhannexus.dukaandost.ui.products.ProductsScreen
import com.burhannexus.dukaandost.ui.settings.SettingsScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val productIdArg = listOf(navArgument("productId") { type = NavType.LongType })
    val builtDestinations = setOf(
        AppDestination.Dashboard,
        AppDestination.Products,
        AppDestination.Settings
    )

    NavHost(
        navController = navController,
        startDestination = AppDestination.Dashboard.route,
        modifier = modifier
    ) {
        composable(AppDestination.Dashboard.route) { DashboardScreen(contentPadding) }

        composable(AppDestination.Products.route) {
            ProductsScreen(
                contentPadding = contentPadding,
                onAddProduct = { navController.navigate(ProductRoutes.edit()) },
                onOpenProduct = { id -> navController.navigate(ProductRoutes.detail(id)) }
            )
        }
        composable(ProductRoutes.EDIT, arguments = productIdArg) {
            ProductEditScreen(
                contentPadding = contentPadding,
                onClose = { navController.popBackStack() }
            )
        }
        composable(ProductRoutes.DETAIL, arguments = productIdArg) {
            ProductDetailScreen(
                contentPadding = contentPadding,
                onEdit = { id -> navController.navigate(ProductRoutes.edit(id)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(AppDestination.Settings.route) { SettingsScreen(contentPadding) }

        AppDestination.entries
            .filter { it !in builtDestinations }
            .forEach { destination ->
                composable(destination.route) { ModuleNotBuiltScreen(destination, contentPadding) }
            }
    }
}
