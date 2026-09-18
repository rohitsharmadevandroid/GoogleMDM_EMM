package com.floydwiz.googlemdm.enterprise.apprestrictions.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.Manifest
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.floydwiz.googlemdm.R
import com.floydwiz.googlemdm.core.logger.Logger
import com.floydwiz.googlemdm.enterprise.admin.manager.DeviceAdminManager
import com.floydwiz.googlemdm.enterprise.apprestrictions.BlockedPackagesStore
import com.floydwiz.googlemdm.enterprise.apprestrictions.receiver.BlockedPackageInstallReceiver
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Keeps this app's process continuously alive and dynamically registers
 * BlockedPackageInstallReceiver from here. A manifest-declared receiver for
 * PACKAGE_ADDED gets silently dropped by Android's background-execution
 * limits once the app has sat in the background a while - confirmed via
 * `dumpsys activity broadcasts` showing the identical "Background
 * execution not allowed" skip for Play Store's and GMS's own equivalent
 * receivers, and merely running a foreground service does NOT exempt a
 * manifest receiver from that limit either (verified: the skip persisted
 * with this service already running). A receiver registered dynamically
 * from a live process is not subject to that limit at all, which is why
 * registration happens here instead of in AndroidManifest.xml.
 */
@AndroidEntryPoint
class BlockedPackageEnforcementService : Service() {

    @Inject lateinit var deviceAdminManager: DeviceAdminManager

    @Inject lateinit var blockedPackagesStore: BlockedPackagesStore

    private var packageInstallReceiver: BlockedPackageInstallReceiver? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Device Owner apps can self-grant this without the system prompt,
            // same pattern already used for READ_PHONE_STATE elsewhere.
            deviceAdminManager.grantSelfRuntimePermission(Manifest.permission.POST_NOTIFICATIONS)
        }
        val notification = buildNotification()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
        registerPackageInstallReceiver()
        Logger.i("BlockedPackageEnforcementService started")
    }

    private fun registerPackageInstallReceiver() {
        if (packageInstallReceiver != null) return

        val receiver = BlockedPackageInstallReceiver(blockedPackagesStore, deviceAdminManager)
        val filter = IntentFilter(Intent.ACTION_PACKAGE_ADDED).apply {
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        packageInstallReceiver = receiver
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        packageInstallReceiver?.let { unregisterReceiver(it) }
        packageInstallReceiver = null
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Device management", NotificationManager.IMPORTANCE_MIN)
            )
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Device managed by Google MDM")
            .setContentText("Enforcing device policies")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "device_policy_enforcement"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, BlockedPackageEnforcementService::class.java)
            )
        }
    }
}
