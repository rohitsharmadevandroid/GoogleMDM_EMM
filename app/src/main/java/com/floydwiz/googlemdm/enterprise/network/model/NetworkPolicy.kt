package com.floydwiz.googlemdm.enterprise.network.model

import com.google.errorprone.annotations.InlineMeValidationDisabled

data class NetworkPolicy(
    val type: NetworkPolicyType,
    val title: String,
    val description: String,
    val disabled: Boolean
)
