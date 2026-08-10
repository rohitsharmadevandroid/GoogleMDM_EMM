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

    val camera = EnterprisePolicyDefinition(
        type = PolicyType.CAMERA,
        title = "Disabled Camera",
        description = "Prevent users from accessing the device camera"
    )

    val screenCapture = EnterprisePolicyDefinition (
        type = PolicyType.SCREEN_CAPTURE,
        title = "Disable Screen Capture",
        description = "Prevent users from capturing the device screen"
    )

    val usbFileTransfer = EnterprisePolicyDefinition (
        type = PolicyType.USB_FILE_TRANSFER,
        title = "Disable USB File Transfer",
        description = "Prevent users from transferring files over USB"
    )

    val safeBoot = EnterprisePolicyDefinition (
        type = PolicyType.SAFE_BOOT,
        title = "Disable Safe Boot",
        description = "Prevent users from booting the device into recovery mode"
    )

    val factoryReset = EnterprisePolicyDefinition (
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

    val KIOSK = EnterprisePolicyDefinition (
        type = PolicyType.KIOSK,
        title = "Kiosk Mode",
        description = "Lock the device into a controlled kiosk environment"
    )

    val all = listOf(
        camera,
        screenCapture,
        usbFileTransfer,
        safeBoot,
        factoryReset,
        addUser,
        outgoingCalls,
        SMS
    )
}