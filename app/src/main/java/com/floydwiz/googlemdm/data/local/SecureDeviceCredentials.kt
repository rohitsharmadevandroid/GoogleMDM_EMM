package com.floydwiz.googlemdm.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.floydwiz.googlemdm.core.logger.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android Keystore-backed storage for the long-lived DPC device identity and
 * bearer credential. The deviceApiKey is never written to plain SharedPreferences,
 * BuildConfig, source, or logs.
 */
@Singleton
class SecureDeviceCredentials @Inject constructor(
    @ApplicationContext private val context: Context
) : DeviceCredentialsStore {

    private val prefs: SharedPreferences by lazy { createEncryptedPrefs() }

    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREF_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun saveEnrollment(deviceId: String, deviceApiKey: String, checkInIntervalSeconds: Long) {
        prefs.edit()
            .putString(KEY_DEVICE_ID, deviceId)
            .putString(KEY_DEVICE_API_KEY, deviceApiKey)
            .putLong(KEY_CHECK_IN_INTERVAL_SECONDS, checkInIntervalSeconds)
            .apply()
        Logger.i("Device enrollment credentials saved for deviceId=$deviceId")
    }

    override fun getDeviceId(): String? = prefs.getString(KEY_DEVICE_ID, null)

    override fun getDeviceApiKey(): String? = prefs.getString(KEY_DEVICE_API_KEY, null)

    override fun isEnrolled(): Boolean = getDeviceId() != null && getDeviceApiKey() != null

    override fun getCheckInIntervalSeconds(): Long =
        prefs.getLong(KEY_CHECK_IN_INTERVAL_SECONDS, DEFAULT_CHECK_IN_INTERVAL_SECONDS)

    override fun setCheckInIntervalSeconds(seconds: Long) {
        prefs.edit().putLong(KEY_CHECK_IN_INTERVAL_SECONDS, seconds).apply()
    }

    override fun getLastPolicyVersionApplied(): Int? =
        if (prefs.contains(KEY_LAST_POLICY_VERSION)) prefs.getInt(KEY_LAST_POLICY_VERSION, 0) else null

    override fun setLastPolicyVersionApplied(version: Int?) {
        val editor = prefs.edit()
        if (version == null) {
            editor.remove(KEY_LAST_POLICY_VERSION)
        } else {
            editor.putInt(KEY_LAST_POLICY_VERSION, version)
        }
        editor.apply()
    }

    override fun getLastSuccessfulCheckInAtMillis(): Long? =
        if (prefs.contains(KEY_LAST_CHECK_IN_AT)) prefs.getLong(KEY_LAST_CHECK_IN_AT, 0L) else null

    override fun setLastSuccessfulCheckInAtMillis(timestampMillis: Long) {
        prefs.edit().putLong(KEY_LAST_CHECK_IN_AT, timestampMillis).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
        Logger.i("Device enrollment credentials cleared")
    }

    companion object {
        private const val PREF_FILE_NAME = "emm_secure_device_prefs"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DEVICE_API_KEY = "device_api_key"
        private const val KEY_CHECK_IN_INTERVAL_SECONDS = "check_in_interval_seconds"
        private const val KEY_LAST_POLICY_VERSION = "last_policy_version_applied"
        private const val KEY_LAST_CHECK_IN_AT = "last_check_in_at_millis"
        const val DEFAULT_CHECK_IN_INTERVAL_SECONDS = 60L
    }
}
