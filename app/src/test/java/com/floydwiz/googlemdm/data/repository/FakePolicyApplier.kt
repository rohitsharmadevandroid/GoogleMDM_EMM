package com.floydwiz.googlemdm.data.repository

import com.floydwiz.googlemdm.data.remote.CustomDpcPolicyPayloadDto
import com.floydwiz.googlemdm.enterprise.policy.PolicyApplier

class FakePolicyApplier : PolicyApplier {
    val appliedPayloads = mutableListOf<CustomDpcPolicyPayloadDto>()

    override suspend fun apply(payload: CustomDpcPolicyPayloadDto) {
        appliedPayloads += payload
    }
}
