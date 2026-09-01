package com.floydwiz.googlemdm.data.local

/**
 * Abstraction over where device identity/credentials live, so the repository
 * and its tests don't depend on the Keystore-backed implementation directly.
 */
interface DeviceCredentialsStore {

    fun saveEnrollment(deviceId: String, deviceApiKey: String, checkInIntervalSeconds: Long)

    fun getDeviceId(): String?

    fun getDeviceApiKey(): String?

    fun isEnrolled(): Boolean

    fun getCheckInIntervalSeconds(): Long

    fun setCheckInIntervalSeconds(seconds: Long)

    fun getLastPolicyVersionApplied(): Int?

    fun setLastPolicyVersionApplied(version: Int?)

    fun getLastSuccessfulCheckInAtMillis(): Long?

    fun setLastSuccessfulCheckInAtMillis(timestampMillis: Long)

    fun clear()
}
