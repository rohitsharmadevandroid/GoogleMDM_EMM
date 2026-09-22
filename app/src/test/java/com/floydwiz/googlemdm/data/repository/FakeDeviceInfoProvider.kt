package com.floydwiz.googlemdm.data.repository

import com.floydwiz.googlemdm.data.local.DeviceInfoProvider

class FakeDeviceInfoProvider(
    private val osVersion: String = "14",
    private val model: String = "Pixel 7",
    private val manufacturer: String = "Google"
) : DeviceInfoProvider {
    override fun osVersion(): String = osVersion
    override fun model(): String = model
    override fun manufacturer(): String = manufacturer
}
