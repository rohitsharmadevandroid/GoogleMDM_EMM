package com.floydwiz.googlemdm.enterprise.policy

import com.floydwiz.googlemdm.data.remote.CustomDpcPolicyPayloadDto

/**
 * Applies a backend-assigned policy payload to the device. Kept as an
 * interface so EmmRepository's tests can fake it instead of exercising the
 * real DevicePolicyManager chain.
 */
interface PolicyApplier {
    fun apply(payload: CustomDpcPolicyPayloadDto)
}
