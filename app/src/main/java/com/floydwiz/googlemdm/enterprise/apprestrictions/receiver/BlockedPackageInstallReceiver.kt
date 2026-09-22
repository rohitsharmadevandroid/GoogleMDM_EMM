package com.floydwiz.googlemdm.enterprise.apprestrictions.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.floydwiz.googlemdm.core.logger.Logger
import com.floydwiz.googlemdm.enterprise.admin.manager.DeviceAdminManager
import com.floydwiz.googlemdm.enterprise.apprestrictions.BlockedPackagesStore

/**
 * Fires on any package install on the device, not just this app's own.
 * Deliberately NOT manifest-declared - Android's background-execution
 * limits silently drop a manifest receiver's PACKAGE_ADDED delivery once
 * the app has been backgrounded a while (confirmed via `dumpsys activity
 * broadcasts` showing "Background execution not allowed", identically for
 * Play Store's and GMS's own equivalent receivers - a foreground service
 * alone does NOT exempt a manifest receiver from this). A receiver
 * registered dynamically (Context.registerReceiver, from
 * BlockedPackageEnforcementService while its process is alive) isn't
 * subject to that limit at all, so this class is only ever constructed
 * and registered in code, never referenced from AndroidManifest.xml.
 */
class BlockedPackageInstallReceiver(
    private val blockedPackagesStore: BlockedPackagesStore,
    private val deviceAdminManager: DeviceAdminManager
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_PACKAGE_ADDED) return

        val packageName = intent.data?.schemeSpecificPart ?: return
        if (packageName == context.packageName) return

        if (blockedPackagesStore.isBlocked(packageName)) {
            Logger.w("Blocked package $packageName was installed - uninstalling it")
            deviceAdminManager.silentlyUninstall(packageName)
        }
    }
}
