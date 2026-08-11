package com.floydwiz.googlemdm.enterprise.enrollment.model

sealed class EnrollmentResult {
    data object Success: EnrollmentResult()

    data object NotDeviceOwner: EnrollmentResult()

    data object NotDeviceAdmin: EnrollmentResult()

    data class Error(
        val message: String
    ): EnrollmentResult()
}
