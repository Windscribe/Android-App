package com.windscribe.vpn.api

import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.apppreference.PreferencesHelper

class DomainFailOverManager(private val preferencesHelper: PreferencesHelper) {
    private var failedStates = mutableMapOf<String, Boolean>()
    private var wgConnectApiFailOverStates = mutableMapOf<String, Boolean>()

    init {
        wgConnectApiFailOverStates.putAll(preferencesHelper.wgConnectApiFailOverState)
    }

    fun isAccessible(domainType: DomainType, apiCallType: ApiCallType): Boolean {
        if (Windscribe.appContext.vpnConnectionStateManager.isVPNConnected()) {
            return true
        }
        return if (apiCallType == ApiCallType.WgConnect) {
            if (wgConnectApiFailOverStates.isEmpty() && failedStates.isNotEmpty()) {
                wgConnectApiFailOverStates.putAll(failedStates)
            }
            wgConnectApiFailOverStates[domainType.name] ?: true
        } else {
            failedStates[domainType.name] ?: true
        }
    }

    fun reset(apiCallType: ApiCallType) {
        if (apiCallType == ApiCallType.WgConnect) {
            wgConnectApiFailOverStates.clear()
            updateWgConnectFailOverState()
        } else {
            failedStates.clear()
        }
    }

    fun setDomainBlocked(domainType: DomainType, apiCallType: ApiCallType) {
        if (apiCallType == ApiCallType.WgConnect) {
            wgConnectApiFailOverStates[domainType.name] = false
            updateWgConnectFailOverState()
        } else {
            failedStates[domainType.name] = false
        }
    }

    private fun updateWgConnectFailOverState() {
        preferencesHelper.wgConnectApiFailOverState = wgConnectApiFailOverStates
    }
}

enum class DomainType {
    Primary, Secondary, Hashed1, Hashed2, Hashed3, DirectIp1, DirectIp2
}

enum class ApiCallType {
    WgConnect, Other
}