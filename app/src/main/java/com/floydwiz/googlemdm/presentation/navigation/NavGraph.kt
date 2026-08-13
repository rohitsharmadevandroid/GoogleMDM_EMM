package com.floydwiz.googlemdm.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.floydwiz.googlemdm.enterprise.kiosk.KioskController
import com.floydwiz.googlemdm.presentation.screens.dashboard.DashboardScreen
import com.floydwiz.googlemdm.presentation.screens.enrollment.EnrollmentScreen
import com.floydwiz.googlemdm.presentation.viewmodel.EnrollmentViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    kioskController: KioskController,
    enrollmentViewModel: EnrollmentViewModel = hiltViewModel()
) {
    val uiState = enrollmentViewModel.uiState.collectAsState().value

    if (!uiState.initialCheckComplete) {
        return
    }

    val startDestination = if(
        uiState.isDeviceOwner &&
        uiState.isEnvironmentPrepared
    ) {
        Routes.DASHBOARD
    } else {
        Routes.ENROLLMENT
    }
   NavHost(
       navController = navController,
       startDestination = startDestination
   ) {
       composable(Routes.DASHBOARD) {
           DashboardScreen(
               kioskController = kioskController,
               onEnrollmentClick = {
                   navController.navigate(Routes.ENROLLMENT)
               }
           )
       }
       composable(Routes.ENROLLMENT) {
           EnrollmentScreen(
               onEnrollmentClick = {
                   enrollmentViewModel.loadEnrollmentState()
               }
           )
       }
   }
}