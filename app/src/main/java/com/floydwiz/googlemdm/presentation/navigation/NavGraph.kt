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
   NavHost(
       navController = navController,
       startDestination = Routes.ENROLLMENT
   ) {
       composable(Routes.ENROLLMENT) {
           EnrollmentScreen(
               navController = navController,
               enrollmentViewModel = enrollmentViewModel
           )
       }

       composable(Routes.DASHBOARD) {
           DashboardScreen(
               kioskController = kioskController,
               onEnrollmentClick = {
                   navController.navigate(Routes.ENROLLMENT)
               }
           )
       }
   }
}