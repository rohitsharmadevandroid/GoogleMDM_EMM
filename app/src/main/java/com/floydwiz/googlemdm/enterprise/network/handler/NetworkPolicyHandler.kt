package com.floydwiz.googlemdm.enterprise.network.handler

import com.floydwiz.googlemdm.enterprise.network.manager.NetworkManager
import com.floydwiz.googlemdm.enterprise.network.model.NetworkPolicyType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkPolicyHandler @Inject constructor(
    private val networkManager: NetworkManager
) {
    fun getNetworkState(): Boolean{
        return networkManager.isWifiEnabled()
    }

    fun setPolicy(
        type: NetworkPolicyType,
        disabled: Boolean
    ): Boolean {
        return when(type) {
            NetworkPolicyType.WIFI -> networkManager.setWifiConfigDisabled(disabled)
            NetworkPolicyType.BLUETOOTH -> false
            NetworkPolicyType.MOBILE_DATA -> false
        }
    }
    fun getPolicy(type: NetworkPolicyType): Boolean {
        return when(type) {
            NetworkPolicyType.WIFI -> networkManager.isWifiConfigDisabled()
            NetworkPolicyType.BLUETOOTH -> false
            NetworkPolicyType.MOBILE_DATA -> false
        }
    }
}