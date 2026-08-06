package com.floydwiz.googlemdm.enterprise.policy.registry

import com.floydwiz.googlemdm.enterprise.policy.model.EnterprisePolicy
import com.floydwiz.googlemdm.enterprise.policy.model.EnterprisePolicyDefinition
import com.floydwiz.googlemdm.enterprise.policy.model.PolicyType

object PolicyDefinitions {

    fun createPolicy(
        definition: EnterprisePolicyDefinition,
        enabled: Boolean
    ): EnterprisePolicy {
        return EnterprisePolicy(
            type = definition.type,
            title = definition.title,
            description = definition.description,
            enabled = enabled
        )
    }

    val Camera = EnterprisePolicyDefinition(
        type = PolicyType.CAMERA,
        title = "Disabled Camera",
        description = "Prevent users from accessing the device camera"
    )

    val ScreenCapture = EnterprisePolicyDefinition (
        type = PolicyType.SCREEN_CAPTURE,
        title = "Disable Screen Capture",
        description = "Prevent users from capturing the device screen"
    )

    val UsbFileTransfer = EnterprisePolicyDefinition (
        type = PolicyType.USB_FILE_TRANSFER,
        title = "Disable USB File Transfer",
        description = "Prevent users from transferring files over USB"
    )

    val SafeBoot = EnterprisePolicyDefinition (
        type = PolicyType.SAFE_BOOT,
        title = "Disable Safe Boot",
        description = "Prevent users from booting the device into recovery mode"
    )

    val FactoryReset = EnterprisePolicyDefinition (
        type = PolicyType.FACTORY_RESET,
        title = "Disable Factory Reset",
        description = "Prevent users from resetting the device to its factory settings"
    )

    val addUser = EnterprisePolicyDefinition (
        type = PolicyType.ADD_USER,
        title = "Disable Add User",
        description = "Prevent users from adding new users to the device"
    )

    val outgoingCalls = EnterprisePolicyDefinition (
        type = PolicyType.OUTGOING_CALLS,
        title = "Disable Outgoing Calls",
        description = "Prevent users from making outgoing calls"
    )

    val SMS = EnterprisePolicyDefinition (
        type = PolicyType.SMS,
        title = "Disable SMS",
        description = "Prevent users from sending SMS messages"
    )
}