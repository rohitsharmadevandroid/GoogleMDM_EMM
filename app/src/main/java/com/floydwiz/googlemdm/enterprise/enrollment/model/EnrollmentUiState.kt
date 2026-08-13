package com.floydwiz.googlemdm.enterprise.enrollment.model

data class EnrollmentUiState(
    val isLoading: Boolean = true,
    val isDeviceOwner: Boolean = false,
    val amapiEnvironmentAvailable: Boolean = false,
    val androidDevicePolicyState: String = "UNKNOWN",
    val androidDevicePolicyVersion: String = "UNKNOWN",
    val isPreparingEnvironment: Boolean = false,
    val isEnvironmentPrepared: Boolean = false,
    val enrollmentStatus: String = "NOT_ENROLLED",
    val error: String? = null,
    val initialCheckComplete: Boolean = false
)