package com.floydwiz.googlemdm.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.floydwiz.googlemdm.enterprise.kiosk.KioskController
import com.floydwiz.googlemdm.presentation.components.icons.IconNetwork
import com.floydwiz.googlemdm.presentation.components.icons.IconOverview
import com.floydwiz.googlemdm.presentation.components.icons.IconPolicy
import com.floydwiz.googlemdm.presentation.screens.emmbackend.EmmBackendScreen
import com.floydwiz.googlemdm.presentation.screens.network.NetworkScreen
import com.floydwiz.googlemdm.presentation.screens.overview.OverviewScreen
import com.floydwiz.googlemdm.presentation.screens.policies.PoliciesScreen
import com.floydwiz.googlemdm.presentation.screens.splash.SplashScreen
import com.floydwiz.googlemdm.presentation.theme.AccentBlue
import com.floydwiz.googlemdm.presentation.viewmodel.DashboardViewModel

private data class BottomNavDestination(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit
)

private val bottomNavDestinations = listOf(
    BottomNavDestination(Routes.OVERVIEW, "Overview") { IconOverview() },
    BottomNavDestination(Routes.NETWORK, "Network") { IconNetwork() },
    BottomNavDestination(Routes.POLICIES, "Policies") { IconPolicy() },
)

private val consoleRoutes = bottomNavDestinations.map { it.route }.toSet()

@Composable
fun NavGraph(
    navController: NavHostController,
    kioskController: KioskController
) {
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val uiState by dashboardViewModel.uiState.collectAsState()
    val policiesEnabledCount = uiState.policies.count { it.enabled }

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            val showBottomBar = currentDestination?.hierarchy?.any { it.route in consoleRoutes } == true

            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    bottomNavDestinations.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                        val badgeCount = if (destination.route == Routes.POLICIES) policiesEnabledCount else 0
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(Routes.OVERVIEW) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                if (badgeCount > 0) {
                                    BadgedBox(badge = { Badge { Text("$badgeCount") } }) {
                                        destination.icon()
                                    }
                                } else {
                                    destination.icon()
                                }
                            },
                            label = { Text(destination.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AccentBlue,
                                selectedTextColor = AccentBlue,
                                indicatorColor = AccentBlue.copy(alpha = 0.16f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    onSelectConsole = { navController.navigate(Routes.OVERVIEW) },
                    onSelectEmmBackend = { navController.navigate(Routes.EMM_BACKEND) }
                )
            }
            composable(Routes.OVERVIEW) {
                OverviewScreen(viewModel = dashboardViewModel)
            }
            composable(Routes.NETWORK) {
                NetworkScreen(viewModel = dashboardViewModel)
            }
            composable(Routes.POLICIES) {
                PoliciesScreen(kioskController = kioskController, viewModel = dashboardViewModel)
            }
            composable(Routes.EMM_BACKEND) {
                EmmBackendScreen()
            }
        }
    }
}
