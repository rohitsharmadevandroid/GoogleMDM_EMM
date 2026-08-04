package com.floydwiz.googlemdm.enterprise.policy.model

import com.google.errorprone.annotations.InlineMeValidationDisabled

data class PolicyState (
    val isCameraDisabled: Boolean = false,
    val isScreenCapturedDisabled: Boolean = false,
    val isUsbFileTransferDisabled: Boolean = false
)