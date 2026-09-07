package com.floydwiz.googlemdm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.floydwiz.googlemdm.enterprise.kiosk.KioskController
import com.floydwiz.googlemdm.enterprise.kiosk.manager.KioskManager
import com.floydwiz.googlemdm.presentation.navigation.AppNavigation
import com.floydwiz.googlemdm.presentation.theme.Google_MdmTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
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

        // A backend-driven kioskMode policy is applied from a background
        // WorkManager coroutine (DevicePolicyApplier), which can't call
        // startLockTask() itself - it only sets the OS-level allow-list.
        // If this Activity is already in the foreground when that happens
        // (e.g. a manual "Check In Now" tap), onResume() below won't fire
        // again on its own, so react to the change live instead.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                kioskManager.kioskModeChanges.collect { enabled ->
                    if (enabled) startKiosk() else stopKiosk()
                }
            }
        }
    }

    /**
     * Fallback for when the policy was applied while this Activity wasn't
     * running at all (backgrounded/killed process, device reboot) - the
     * live collector above only catches changes that arrive while already
     * STARTED, so this re-checks persisted state on every resume too.
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
