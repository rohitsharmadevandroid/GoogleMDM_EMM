package com.floydwiz.googlemdm.enterprise.apprestrictions

import android.content.Context
import com.floydwiz.googlemdm.core.logger.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the current backend BLOCKED package list so BlockedPackageInstallReceiver
 * can react to a sideload attempt immediately, without waiting for the next check-in.
 */
@Singleton
class BlockedPackagesStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("blocked_packages_prefs", Context.MODE_PRIVATE)

    /** Replaces the whole set - a package missing from a later policy is no longer blocked. */
    fun replaceAll(packageNames: Set<String>) {
        Logger.d("Blocked packages set to $packageNames")
        prefs.edit().putStringSet(KEY, packageNames).apply()
    }

    fun isBlocked(packageName: String): Boolean {
        return prefs.getStringSet(KEY, emptySet())?.contains(packageName) ?: false
    }

    companion object {
        private const val KEY = "blocked_package_names"
    }
}
