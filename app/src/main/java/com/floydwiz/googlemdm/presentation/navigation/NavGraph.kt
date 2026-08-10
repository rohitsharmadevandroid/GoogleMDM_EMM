package com.floydwiz.googlemdm.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.floydwiz.googlemdm.enterprise.kiosk.KioskController
import com.floydwiz.googlemdm.presentation.screens.dashboard.DashboardScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    kioskController: KioskController
) {
   NavHost(
       navController = navController,
       startDestination = Routes.DASHBOARD
   ) {
       composable(Routes.DASHBOARD) {
           DashboardScreen(
               kioskController = kioskController
           )
       }

   }
}