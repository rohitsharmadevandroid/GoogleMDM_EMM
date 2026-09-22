package com.floydwiz.googlemdm.enterprise.policy

import android.app.admin.DevicePolicyManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PasswordQualityResolverTest {

    @Test
    fun `resolve returns the real DevicePolicyManager constant value for a known name`() {
        val result = PasswordQualityResolver.resolve("PASSWORD_QUALITY_ALPHANUMERIC")

        assertEquals(DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC, result)
    }

    @Test
    fun `resolve returns the unspecified constant value by name`() {
        val result = PasswordQualityResolver.resolve("PASSWORD_QUALITY_UNSPECIFIED")

        assertEquals(DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED, result)
    }

    @Test
    fun `resolve returns null for an unrecognized name without throwing`() {
        val result = PasswordQualityResolver.resolve("NOT_A_REAL_CONSTANT")

        assertNull(result)
    }
}
