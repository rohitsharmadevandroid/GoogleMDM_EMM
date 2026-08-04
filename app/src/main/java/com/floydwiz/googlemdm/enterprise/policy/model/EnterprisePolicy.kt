package com.floydwiz.googlemdm.enterprise.policy.model

data class EnterprisePolicy (
    val type: PolicyType,
    val title: String,
    val description: String,
    val enabled: Boolean,
)