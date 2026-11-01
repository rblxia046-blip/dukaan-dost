package com.burhannexus.dukaandost.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.burhannexus.dukaandost.ui.components.AppScaffold
import com.burhannexus.dukaandost.ui.navigation.AppNavHost

@Composable
fun DukaanDostRoot() {
    val navController = rememberNavController()
    AppScaffold(navController = navController) { padding ->
        AppNavHost(navController = navController, contentPadding = padding)
    }
}
