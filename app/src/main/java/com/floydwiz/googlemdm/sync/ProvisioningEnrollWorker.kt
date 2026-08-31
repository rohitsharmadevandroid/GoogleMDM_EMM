package com.floydwiz.googlemdm.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.floydwiz.googlemdm.core.logger.Logger
import com.floydwiz.googlemdm.data.model.EnrollmentResult
import com.floydwiz.googlemdm.data.repository.EmmRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Auto-enrolls with the backend right after QR provisioning grants Device
 * Owner - enqueued from MyDeviceAdminReceiver.onProfileProvisioningComplete()
 * with the token pulled from the admin extras bundle. Converges on the same
 * EmmRepository.enroll() call the manual token-entry path (EmmViewModel)
 * already uses, so there's no separate enrollment logic to keep in sync.
 */
@HiltWorker
class ProvisioningEnrollWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val emmRepository: EmmRepository,
    private val checkInScheduler: CheckInScheduler
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val token = inputData.getString(KEY_ENROLLMENT_TOKEN)
        if (token.isNullOrBlank()) {
            Logger.e("QR-provisioning auto-enroll worker started without an enrollmentToken")
            return Result.failure()
        }

        return when (val result = emmRepository.enroll(token)) {
            is EnrollmentResult.Success -> {
                Logger.i("QR-provisioning auto-enroll succeeded, deviceId=${result.deviceId}")
                checkInScheduler.ensureScheduled(result.checkInIntervalSeconds)
                Result.success()
            }

            is EnrollmentResult.Error -> {
                Logger.e("QR-provisioning auto-enroll failed: ${result.message}")
                Result.retry()
            }
        }
    }

    companion object {
        const val KEY_ENROLLMENT_TOKEN = "enrollmentToken"
    }
}
