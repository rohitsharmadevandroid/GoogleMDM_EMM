package com.floydwiz.googlemdm.enterprise.kiosk

import android.content.Context
import com.floydwiz.googlemdm.core.logger.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KioskPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("kiosk_prefs", Context.MODE_PRIVATE)

    fun setKioskEnabled(enabled: Boolean) {
        Logger.d("Kiosk Enabled = $enabled")
        prefs.edit()
            .putBoolean("kiosk_enabled", enabled)
            .apply()
    }

    fun isKioskEnabled(): Boolean {
        val enabled = prefs.getBoolean("kiosk_enabled", false)
        Logger.d("Reading Kiosk state = $enabled")
        return enabled
    }
}