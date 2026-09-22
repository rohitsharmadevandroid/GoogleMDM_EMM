package com.floydwiz.googlemdm.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the real Keystore-backed EncryptedSharedPreferences round-trip.
 * Requires a device/emulator (Android Keystore isn't available on the plain JVM).
 */
@RunWith(AndroidJUnit4::class)
class SecureDeviceCredentialsTest {

    private lateinit var credentials: SecureDeviceCredentials

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        credentials = SecureDeviceCredentials(context)
        credentials.clear()
    }

    @After
    fun tearDown() {
        credentials.clear()
    }

    @Test
    fun freshInstall_isNotEnrolled() {
        assertFalse(credentials.isEnrolled())
        assertNull(credentials.getDeviceId())
        assertNull(credentials.getDeviceApiKey())
    }

    @Test
    fun saveEnrollment_persistsDeviceIdAndApiKeyAndInterval() {
        credentials.saveEnrollment("device-123", "super-secret-api-key", 90L)

        assertTrue(credentials.isEnrolled())
        assertEquals("device-123", credentials.getDeviceId())
        assertEquals("super-secret-api-key", credentials.getDeviceApiKey())
        assertEquals(90L, credentials.getCheckInIntervalSeconds())
    }

    @Test
    fun setCheckInIntervalSeconds_updatesStoredValueIndependently() {
        credentials.saveEnrollment("device-123", "api-key", 60L)

        credentials.setCheckInIntervalSeconds(300L)

        assertEquals(300L, credentials.getCheckInIntervalSeconds())
        assertEquals("device-123", credentials.getDeviceId())
    }

    @Test
    fun lastPolicyVersionApplied_defaultsToNullAndCanBeCleared() {
        assertNull(credentials.getLastPolicyVersionApplied())

        credentials.setLastPolicyVersionApplied(5)
        assertEquals(5, credentials.getLastPolicyVersionApplied())

        credentials.setLastPolicyVersionApplied(null)
        assertNull(credentials.getLastPolicyVersionApplied())
    }

    @Test
    fun clear_removesAllPersistedCredentials() {
        credentials.saveEnrollment("device-123", "api-key", 60L)

        credentials.clear()

        assertFalse(credentials.isEnrolled())
        assertNull(credentials.getDeviceId())
        assertNull(credentials.getDeviceApiKey())
    }
}
