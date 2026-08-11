package com.floydwiz.googlemdm.enterprise.provisioning.model

enum class ProvisioningState{
    NOT_PROVISIONED,
    PREPARING,
    READY,
    PROVISIONING,
    COMPLETED,
    FAILED
}