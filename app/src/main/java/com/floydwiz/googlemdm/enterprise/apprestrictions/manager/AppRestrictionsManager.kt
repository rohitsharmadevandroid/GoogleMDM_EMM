package com.floydwiz.googlemdm.enterprise.apprestrictions.manager

import com.floydwiz.googlemdm.core.logger.Logger
import com.floydwiz.googlemdm.data.remote.AppRestrictionPayloadDto
import com.floydwiz.googlemdm.enterprise.admin.manager.DeviceAdminManager
import com.floydwiz.googlemdm.enterprise.apprestrictions.BlockedPackagesStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies one backend appRestrictions[] entry. A non-GMS device has no Play
 * EMM integration - REQUIRED only becomes installable when the backend
 * supplies an apkUrl/apkSha256 (an admin-hosted source), otherwise this DPC
 * can only track/enforce what's already there.
 */
@Singleton
class AppRestrictionsManager @Inject constructor(
    private val deviceAdminManager: DeviceAdminManager,
    private val apkInstaller: ApkInstaller,
    private val blockedPackagesStore: BlockedPackagesStore
) {
    /**
     * Keeps BlockedPackageInstallReceiver's package list current - called
     * once per check-in with the full list, not per-item, since a package
     * dropped from BLOCKED here must stop being enforced too.
     */
    fun syncBlockedPackages(appRestrictions: List<AppRestrictionPayloadDto>) {
        val blocked = appRestrictions
            .filter { it.installType == "BLOCKED" }
            .map { it.packageName }
            .toSet()
        blockedPackagesStore.replaceAll(blocked)
    }

    suspend fun apply(restriction: AppRestrictionPayloadDto): Boolean {
        return when (restriction.installType) {
            "BLOCKED" -> deviceAdminManager.setApplicationHidden(restriction.packageName, hidden = true)

            "REQUIRED" -> {
                if (deviceAdminManager.isPackageInstalled(restriction.packageName)) {
                    deviceAdminManager.setApplicationHidden(restriction.packageName, hidden = false)
                } else if (!restriction.apkUrl.isNullOrEmpty() && !restriction.apkSha256.isNullOrEmpty()) {
                    val installed = apkInstaller.downloadAndInstall(
                        restriction.packageName,
                        restriction.apkUrl,
                        restriction.apkSha256
                    )
                    Logger.i("Silent install of ${restriction.packageName} from backend apkUrl, success=$installed")
                    installed && deviceAdminManager.setApplicationHidden(restriction.packageName, hidden = false)
                } else {
                    Logger.w(
                        "appRestrictions requires ${restriction.packageName} but it is not " +
                            "installed and the backend supplied no apkUrl/apkSha256 for it"
                    )
                    false
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
