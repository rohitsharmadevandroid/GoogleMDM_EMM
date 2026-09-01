package com.floydwiz.googlemdm.enterprise.appmanagement

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.floydwiz.googlemdm.BuildConfig
import com.floydwiz.googlemdm.core.constants.AppConstants
import com.floydwiz.googlemdm.core.logger.Logger
import com.floydwiz.googlemdm.data.repository.EmmRepository
import com.floydwiz.googlemdm.sync.CheckInScheduler
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class GoogleMdmApplication : Application(), Configuration.Provider {

    @Inject lateinit var hiltWorkerFactory: HiltWorkerFactory

    @Inject lateinit var emmRepository: EmmRepository

    @Inject lateinit var checkInScheduler: CheckInScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(hiltWorkerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Logger.i("${AppConstants.APP_NAME} Application Started")

        // Resume background check-in polling for an already-enrolled device
        // (e.g. after a process restart or device reboot).
        if (emmRepository.isEnrolled) {
            checkInScheduler.ensureScheduled(emmRepository.checkInIntervalSeconds)
        }
    }
}
