package com.floydwiz.googlemdm.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.floydwiz.googlemdm.core.logger.Logger
import com.floydwiz.googlemdm.data.model.CheckInResult
import com.floydwiz.googlemdm.data.repository.EmmRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class CheckInWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val emmRepository: EmmRepository,
    private val checkInScheduler: CheckInScheduler
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return when (val result = emmRepository.checkIn()) {
            is CheckInResult.Success -> {
                Logger.i("Background check-in succeeded, next interval=${result.checkInIntervalSeconds}s")
                checkInScheduler.rescheduleIfIntervalChanged(result.checkInIntervalSeconds)
                Result.success()
            }

            CheckInResult.MissingCredentials -> {
                Logger.w("Background check-in skipped: device not enrolled")
                Result.failure()
            }

            CheckInResult.Unauthorized -> {
                Logger.w("Background check-in unauthorized: credential rejected by backend, local enrollment cleared")
                checkInScheduler.stop()
                Result.failure()
            }

            is CheckInResult.Error -> {
                Logger.e("Background check-in failed: ${result.message}")
                Result.retry()
            }
        }
    }
}
