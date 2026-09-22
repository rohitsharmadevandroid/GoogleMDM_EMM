package com.floydwiz.googlemdm.data.model

sealed class CheckInResult {
    data class Success(
        val checkInIntervalSeconds: Long,
        val hasPolicy: Boolean,
        val policyVersion: Int?,
        val pendingCommandCount: Int
    ) : CheckInResult()

    /** No deviceId/deviceApiKey persisted locally yet - enroll first. */
    data object MissingCredentials : CheckInResult()

    /** Backend rejected the stored deviceApiKey (HTTP 401). */
    data object Unauthorized : CheckInResult()

    data class Error(val message: String) : CheckInResult()
}
