package com.floydwiz.googlemdm.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.floydwiz.googlemdm.enterprise.kiosk.KioskController

@Composable
fun AppNavigation(
    kioskController: KioskController
) {
    val navController = rememberNavController()

    NavGraph(
        navController,
        kioskController = kioskController
    )
}