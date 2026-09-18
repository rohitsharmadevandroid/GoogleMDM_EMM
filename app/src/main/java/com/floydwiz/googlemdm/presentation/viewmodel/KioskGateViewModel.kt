package com.floydwiz.googlemdm.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.floydwiz.googlemdm.enterprise.kiosk.manager.KioskManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Lets AppNavigation decide, at the top level, whether to show the normal
 * MDM UI (Enrollment/Dashboard) or the KioskScreen - kiosk mode locks the
 * device into this app, so the MDM management UI itself shouldn't be what's
 * shown while it's active.
 */
@HiltViewModel
class KioskGateViewModel @Inject constructor(
    private val kioskManager: KioskManager
) : ViewModel() {

    val kioskModeState: StateFlow<Boolean> = kioskManager.kioskModeState

    /** Admin escape hatch from the kiosk screen - only clears the DPM allow-list/local flag, KioskController.stopKiosk() is still needed to actually release lock task. */
    fun exitKiosk() {
        kioskManager.disabledKioskMode()
    }
}
