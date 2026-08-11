package com.floydwiz.googlemdm.enterprise.enrollment.manager

import com.floydwiz.googlemdm.enterprise.admin.manager.DeviceAdminManager
import com.floydwiz.googlemdm.enterprise.enrollment.model.EnrollmentState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnrollmentManager @Inject constructor(
    private val deviceAdminManager: DeviceAdminManager
) {
    fun getEnrollmentState(): EnrollmentState {
        return when {
            deviceAdminManager.isDeviceOwner() -> {
                EnrollmentState.DEVICE_OWNER
            }

            deviceAdminManager.isAdminActive() -> {
                EnrollmentState.DEVICE_ADMIN
            }

            else -> {
                EnrollmentState.NOT_ENROLLED
            }
        }
    }
}