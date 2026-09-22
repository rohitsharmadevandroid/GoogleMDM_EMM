package com.floydwiz.googlemdm.data.local

import android.os.Build
import javax.inject.Inject

/**
 * Isolates android.os.Build access behind an interface so the repository's
 * check-in request construction can be unit tested on the plain JVM.
 */
interface DeviceInfoProvider {
    fun osVersion(): String
    fun model(): String
    fun manufacturer(): String
}

class AndroidDeviceInfoProvider @Inject constructor() : DeviceInfoProvider {
    override fun osVersion(): String = Build.VERSION.RELEASE ?: "unknown"
    override fun model(): String = Build.MODEL ?: "unknown"
    override fun manufacturer(): String = Build.MANUFACTURER ?: "unknown"
}
