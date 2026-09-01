package com.floydwiz.googlemdm.data.repository

import com.floydwiz.googlemdm.data.local.DeviceCredentialsStore

class FakeDeviceCredentialsStore : DeviceCredentialsStore {
    private var deviceId: String? = null
    private var deviceApiKey: String? = null
    private var checkInIntervalSeconds: Long = 60L
    private var lastPolicyVersionApplied: Int? = null
    private var lastSuccessfulCheckInAtMillis: Long? = null

    override fun saveEnrollment(deviceId: String, deviceApiKey: String, checkInIntervalSeconds: Long) {
        this.deviceId = deviceId
        this.deviceApiKey = deviceApiKey
        this.checkInIntervalSeconds = checkInIntervalSeconds
    }

    override fun getDeviceId(): String? = deviceId

    override fun getDeviceApiKey(): String? = deviceApiKey

    override fun isEnrolled(): Boolean = deviceId != null && deviceApiKey != null

    override fun getCheckInIntervalSeconds(): Long = checkInIntervalSeconds

    override fun setCheckInIntervalSeconds(seconds: Long) {
        checkInIntervalSeconds = seconds
    }

    override fun getLastPolicyVersionApplied(): Int? = lastPolicyVersionApplied

    override fun setLastPolicyVersionApplied(version: Int?) {
        lastPolicyVersionApplied = version
    }

    override fun getLastSuccessfulCheckInAtMillis(): Long? = lastSuccessfulCheckInAtMillis

    override fun setLastSuccessfulCheckInAtMillis(timestampMillis: Long) {
        lastSuccessfulCheckInAtMillis = timestampMillis
    }

    override fun clear() {
        deviceId = null
        deviceApiKey = null
        lastPolicyVersionApplied = null
        lastSuccessfulCheckInAtMillis = null
    }
}
