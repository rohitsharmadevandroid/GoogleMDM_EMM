package com.floydwiz.googlemdm.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.floydwiz.googlemdm.enterprise.kiosk.KioskController
import com.floydwiz.googlemdm.presentation.screens.kiosk.KioskScreen
import com.floydwiz.googlemdm.presentation.viewmodel.KioskGateViewModel

@Composable
fun AppNavigation(
    kioskController: KioskController
) {
    val navController = rememberNavController()
    val kioskGateViewModel: KioskGateViewModel = hiltViewModel()
    val isKioskModeEnabled by kioskGateViewModel.kioskModeState.collectAsState()

    if (isKioskModeEnabled) {
        // Kiosk mode locks the device into this app - show the kiosk screen
        // instead of the MDM management UI while it's active.
        KioskScreen(
            onExitKiosk = {
                kioskGateViewModel.exitKiosk()
                kioskController.stopKiosk()
            }
        )
    } else {
        NavGraph(
            navController,
            kioskController = kioskController
        )
    }
}
