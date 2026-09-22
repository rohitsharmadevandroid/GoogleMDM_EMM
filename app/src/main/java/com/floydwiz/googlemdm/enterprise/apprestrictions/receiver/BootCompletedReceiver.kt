package com.floydwiz.googlemdm.enterprise.apprestrictions.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.floydwiz.googlemdm.enterprise.apprestrictions.service.BlockedPackageEnforcementService

/** Restarts BlockedPackageEnforcementService after a reboot - a foreground service does not survive one on its own. */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        BlockedPackageEnforcementService.start(context)
    }
}
