package com.floydwiz.googlemdm.enterprise.policy

import android.app.admin.DevicePolicyManager
import com.floydwiz.googlemdm.core.logger.Logger

/**
 * The backend names password.quality values after DevicePolicyManager's own
 * PASSWORD_QUALITY_* constants on purpose, so resolving by reflection here is
 * the intended mechanism - not a hardcoded map that can drift out of sync
 * with whatever constant names the backend emits.
 */
object PasswordQualityResolver {
    fun resolve(qualityName: String): Int? {
        return try {
            DevicePolicyManager::class.java.getField(qualityName).getInt(null)
        } catch (e: NoSuchFieldException) {
            Logger.w("Unrecognized password quality constant: $qualityName")
            null
        } catch (e: Exception) {
            Logger.w("Failed to resolve password quality constant: $qualityName")
            null
        }
    }
}
