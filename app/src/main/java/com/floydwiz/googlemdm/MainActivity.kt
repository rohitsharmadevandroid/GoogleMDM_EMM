package com.floydwiz.googlemdm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.floydwiz.googlemdm.core.logger.Logger
import com.floydwiz.googlemdm.enterprise.kiosk.KioskController
import com.floydwiz.googlemdm.enterprise.kiosk.manager.KioskManager
import com.floydwiz.googlemdm.presentation.navigation.AppNavigation
import com.floydwiz.googlemdm.presentation.theme.Google_MdmTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity(), KioskController {

    @Inject lateinit var kioskManager: KioskManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Google_MdmTheme {
                AppNavigation(
                    kioskController = this
                )
            }
        }
    }

    /**
     * A backend-driven kioskMode policy can only set the OS-level lock task
     * allow-list from the background (DevicePolicyApplier); startLockTask()
     * itself requires a foregrounded Activity, so it's engaged here on
     * resume instead - it takes effect the next time this Activity is
     * opened, not the instant the policy is delivered.
     */
    override fun onResume() {
        super.onResume()
        if (kioskManager.isKioskModeEnabled()) {
            startKiosk()
        }
    }

    override fun startKiosk() {
        startLockTask()
    }
    override fun stopKiosk() {
        stopLockTask()
    }
}

