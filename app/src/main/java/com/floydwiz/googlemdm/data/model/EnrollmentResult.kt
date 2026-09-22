package com.floydwiz.googlemdm.data.model

sealed class EnrollmentResult {
    data class Success(
        val deviceId: String,
        val checkInIntervalSeconds: Long
    ) : EnrollmentResult()

    data class Error(val message: String) : EnrollmentResult()
}
