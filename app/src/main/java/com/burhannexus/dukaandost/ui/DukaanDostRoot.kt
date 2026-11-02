package com.burhannexus.dukaandost.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.burhannexus.dukaandost.ui.business.BusinessProfileScreen
import com.burhannexus.dukaandost.ui.components.AppScaffold
import com.burhannexus.dukaandost.ui.navigation.AppNavHost

@Composable
fun DukaanDostRoot(viewModel: RootViewModel = hiltViewModel()) {
    val setup by viewModel.setup.collectAsStateWithLifecycle()

    when (val state = setup) {
        SetupState.Loading -> Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }

        // First run: ask for the shop details before anything else. After saving, the state
        // becomes Done by itself and the normal app appears.
        SetupState.Needed -> Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            BusinessProfileScreen(
                contentPadding = PaddingValues(0.dp),
                firstRun = true,
                onSaved = {}
            )
        }

        is SetupState.Done -> {
            val navController = rememberNavController()
            AppScaffold(navController = navController, businessName = state.profile.shopName) { padding ->
                AppNavHost(navController = navController, contentPadding = padding)
            }
        }
    }
}
