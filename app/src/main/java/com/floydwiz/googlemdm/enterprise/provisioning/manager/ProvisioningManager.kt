package com.floydwiz.googlemdm.enterprise.provisioning.manager

import com.floydwiz.googlemdm.enterprise.provisioning.model.ProvisioningState
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class ProvisioningManager @Inject constructor() {

    fun getProvisioningManager(): ProvisioningState
    {
        return ProvisioningState.NOT_PROVISIONED
    }
}