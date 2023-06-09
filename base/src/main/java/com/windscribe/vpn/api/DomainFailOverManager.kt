package com.windscribe.vpn.api

import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.apppreference.PreferencesHelper
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DomainFailOverManager(private val preferencesHelper: PreferencesHelper) {
    private var failedStates = mutableMapOf<String, Boolean>()
    private val lock = Mutex(false)
    private var wgConnectApiFailOverStates = mutableMapOf<String, Boolean>()

    init {
        wgConnectApiFailOverStates.putAll(preferencesHelper.wgConnectApiFailOverState)
    }

    fun isAccessible(domainType: DomainType, apiCallType: ApiCallType): Boolean {
        if (Windscribe.appContext.vpnConnectionStateManager.isVPNConnected()) {
            return true
        }
        return runBlocking {
            lock.withLock {
                if (apiCallType == ApiCallType.WgConnect) {
                    if (wgConnectApiFailOverStates.isEmpty() && failedStates.isNotEmpty()) {
                        wgConnectApiFailOverStates.putAll(failedStates)
                    }
                    return@runBlocking wgConnectApiFailOverStates[domainType.name] ?: true
                } else {
                    return@runBlocking failedStates[domainType.name] ?: true
                }
            }
        }
    }

    fun reset(apiCallType: ApiCallType) {
        runBlocking {
            lock.withLock {
                if (apiCallType == ApiCallType.WgConnect) {
                    wgConnectApiFailOverStates.clear()
                    updateWgConnectFailOverState()
                } else {
                    failedStates.clear()
                }
            }
        }
    }

    fun setDomainBlocked(domainType: DomainType, apiCallType: ApiCallType) {
        runBlocking {
            lock.withLock {
                if (apiCallType == ApiCallType.WgConnect) {
                    wgConnectApiFailOverStates[domainType.name] = false
                    updateWgConnectFailOverState()
                } else {
                    failedStates[domainType.name] = false
                }
            }
        }
    }

    private fun updateWgConnectFailOverState() {
        preferencesHelper.wgConnectApiFailOverState = wgConnectApiFailOverStates
    }
}

enum class DomainType {
    Primary, Secondary, DYNAMIC_DOH, Hashed, Ech, DirectIp1, DirectIp2
}

enum class ApiCallType {
    WgConnect, Other
}