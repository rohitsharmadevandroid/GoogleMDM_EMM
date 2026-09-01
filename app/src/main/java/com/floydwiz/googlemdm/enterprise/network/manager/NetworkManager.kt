package com.floydwiz.googlemdm.enterprise.network.manager

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import com.floydwiz.googlemdm.core.logger.Logger
import com.floydwiz.googlemdm.enterprise.admin.manager.DeviceAdminManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceAdminManager: DeviceAdminManager
) {
    private val wifiManager =
        context.applicationContext.getSystemService(
            WifiManager::class.java
        )

    fun isWifiEnabled(): Boolean {
        return try{
            val enabled = wifiManager.isWifiEnabled
            Logger.d("Wifi Enabled = $enabled")
            enabled
        } catch (e: SecurityException) {
            Logger.d("Failed to read wifi state")
            false
        } catch (e: Exception) {
            Logger.d("Unexpected error occurred while reading wifi state")
            false
        }
    }

    fun setWifiConfigDisabled(disabled: Boolean): Boolean {
        return try{
            val success = deviceAdminManager.setWifiConfigDisabled(disabled)

            Logger.d("Wifi Config Disabled = $disabled")
            success
        } catch (e: Exception) {
            Logger.e("Failed to change Wi-Fi configuration restriction: ${e.message}")
            false
        }
    }

    fun isWifiConfigDisabled(): Boolean {
        return try {
            val disabled = deviceAdminManager.getWifiConfigDisabled()
            Logger.d("Wi-fi Configuration Disabled = $disabled")
            disabled
        } catch (e: Exception) {
            Logger.e("Failed to read Wi-Fi configuration restriction: ${e.message}")
            false
        }
    }

    /**
     * Silently provisions a network as Device Owner - no user interaction,
     * unlike WifiNetworkSuggestion. Only OPEN and *_PSK securityType values
     * are handled; anything else is logged and skipped rather than guessed at.
     */
    @Suppress("DEPRECATION")
    fun connectToWifiNetwork(
        ssid: String,
        securityType: String,
        password: String?,
        hidden: Boolean
    ): Boolean {
        if (!deviceAdminManager.isDeviceOwner()) {
            Logger.e("Cannot Configure Wi-Fi Network. App is not Device Owner")
            return false
        }

        val config = WifiConfiguration().apply {
            SSID = "\"$ssid\""
            hiddenSSID = hidden
        }

        when {
            securityType == "OPEN" -> {
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            }
            securityType.endsWith("PSK") && !password.isNullOrEmpty() -> {
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                config.preSharedKey = "\"$password\""
            }
            else -> {
                Logger.w("Unrecognized or incomplete Wi-Fi securityType: $securityType - skipping")
                return false
            }
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val result = wifiManager.addNetworkPrivileged(config)
                val success = result.statusCode == WifiManager.AddNetworkResult.STATUS_SUCCESS
                Logger.i("Wi-Fi network $ssid added via addNetworkPrivileged, success=$success")
                success
            } else {
                val networkId = wifiManager.addNetwork(config)
                val success = networkId != -1 &&
                    wifiManager.enableNetwork(networkId, false) &&
                    wifiManager.saveConfiguration()
                Logger.i("Wi-Fi network $ssid added via legacy addNetwork, success=$success")
                success
            }
        } catch (e: SecurityException) {
            Logger.e("Failed to configure Wi-Fi network $ssid")
            false
        } catch (e: Exception) {
            Logger.e("Unexpected error occurred while configuring Wi-Fi network $ssid: ${e.message}")
            false
        }
    }
}