package com.floydwiz.googlemdm.enterprise.appmanagement

import android.app.Application
import com.floydwiz.googlemdm.BuildConfig
import com.floydwiz.googlemdm.core.constants.AppConstants
import com.floydwiz.googlemdm.core.logger.Logger
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class GoogleMdmApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Logger.i("${AppConstants.APP_NAME} Application Started")
    }
}