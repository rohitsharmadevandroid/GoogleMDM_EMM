package com.floydwiz.googlemdm.enterprise.apprestrictions.manager

import com.floydwiz.googlemdm.core.logger.Logger
import com.floydwiz.googlemdm.data.remote.AppRestrictionPayloadDto
import com.floydwiz.googlemdm.enterprise.admin.manager.DeviceAdminManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies one backend appRestrictions[] entry. A non-GMS device has no Play
 * EMM integration, so REQUIRED never comes with an APK source - this DPC
 * cannot fetch/install a package on its own, only track/enforce what's
 * already there.
 */
@Singleton
class AppRestrictionsManager @Inject constructor(
    private val deviceAdminManager: DeviceAdminManager
) {
    fun apply(restriction: AppRestrictionPayloadDto): Boolean {
        return when (restriction.installType) {
            "BLOCKED" -> deviceAdminManager.setApplicationHidden(restriction.packageName, hidden = true)

            "REQUIRED" -> {
                if (!deviceAdminManager.isPackageInstalled(restriction.packageName)) {
                    Logger.w(
                        "appRestrictions requires ${restriction.packageName} but it is not " +
                            "installed - this app has no APK source to install it from " +
                            "(no Play EMM integration on a non-GMS device)"
                    )
                    false
                } else {
                    deviceAdminManager.setApplicationHidden(restriction.packageName, hidden = false)
                }
            }

            "AVAILABLE" -> {
                Logger.d(
                    "appRestrictions AVAILABLE for ${restriction.packageName} - no device-side " +
                        "restriction to apply (no app-catalog integration on a non-GMS device)"
                )
                true
            }

            else -> {
                Logger.w("Unrecognized appRestrictions installType: ${restriction.installType}")
                false
            }
        }
    }
}
