package com.floydwiz.googlemdm.enterprise.admin.receiver

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.floydwiz.googlemdm.core.logger.Logger
import com.floydwiz.googlemdm.sync.ProvisioningEnrollWorker
import java.util.concurrent.TimeUnit

class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Logger.i("Device Admin Enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Logger.i("Device Admin Disabled")
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        super.onPasswordSucceeded(context, intent)
        Logger.d("Password Authentication Succeeded")
    }

    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        Logger.w("Password Authentication Failed")
    }

    /**
     * Fired by Setup Wizard once QR/NFC/zero-touch provisioning has granted
     * this app Device Owner - never fired by the `adb shell dpm
     * set-device-owner` path used for manual testing. The admin extras
     * bundle carries the enrollment token under ADMIN_EXTRAS_ENROLLMENT_TOKEN_KEY,
     * a contract confirmed with the backend's QR-generation side.
     */
    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        Logger.i("Device Owner provisioning complete")

        val extras = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(
                DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE,
                PersistableBundle::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE)
        }
        val enrollmentToken = extras?.getString(ADMIN_EXTRAS_ENROLLMENT_TOKEN_KEY)

        if (enrollmentToken.isNullOrBlank()) {
            Logger.w(
                "Provisioning complete but no enrollmentToken in admin extras - device owner " +
                    "granted, backend enrollment must be done manually from EnrollmentScreen"
            )
            return
        }

        val workRequest = OneTimeWorkRequestBuilder<ProvisioningEnrollWorker>()
            .setInputData(workDataOf(ProvisioningEnrollWorker.KEY_ENROLLMENT_TOKEN to enrollmentToken))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
        Logger.i("Enqueued auto-enroll work request from QR provisioning")
    }

    companion object {
        const val ADMIN_EXTRAS_ENROLLMENT_TOKEN_KEY = "enrollmentToken"
    }
}
