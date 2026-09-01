package com.floydwiz.googlemdm.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.floydwiz.googlemdm.core.logger.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules the periodic background check-in. The server tells us how often
 * to check in via checkInIntervalSeconds, but WorkManager enforces its own
 * minimum periodic interval (15 minutes) regardless of what the server asks
 * for - we clamp to that floor and log when we can't honor the exact value.
 */
@Singleton
class CheckInScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    private var lastScheduledIntervalSeconds: Long? = null

    /** Called right after enrollment, or on app start to resume polling for an already-enrolled device. */
    fun ensureScheduled(requestedIntervalSeconds: Long) {
        enqueue(requestedIntervalSeconds, ExistingPeriodicWorkPolicy.KEEP)
    }

    /** Called after a check-in reports a different server interval than we're currently polling at. */
    fun rescheduleIfIntervalChanged(newIntervalSeconds: Long) {
        if (lastScheduledIntervalSeconds == clampToWorkManagerMinimum(newIntervalSeconds)) return
        enqueue(newIntervalSeconds, ExistingPeriodicWorkPolicy.UPDATE)
    }

    private fun enqueue(requestedIntervalSeconds: Long, policy: ExistingPeriodicWorkPolicy) {
        val clampedSeconds = clampToWorkManagerMinimum(requestedIntervalSeconds)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequest.Builder(
            CheckInWorker::class.java,
            clampedSeconds,
            TimeUnit.SECONDS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, policy, request)
        lastScheduledIntervalSeconds = clampedSeconds

        Logger.i("Check-in polling scheduled every ${clampedSeconds}s (server requested ${requestedIntervalSeconds}s)")
    }

    fun stop() {
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
        Logger.i("Check-in polling cancelled")
    }

    private fun clampToWorkManagerMinimum(requestedSeconds: Long): Long {
        val minSeconds = TimeUnit.MILLISECONDS.toSeconds(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS)
        if (requestedSeconds < minSeconds) {
            Logger.w(
                "Server requested ${requestedSeconds}s check-in interval, but WorkManager's " +
                    "minimum periodic interval is ${minSeconds}s - clamping to the floor."
            )
            return minSeconds
        }
        return requestedSeconds
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "emm_check_in_work"
    }
}
