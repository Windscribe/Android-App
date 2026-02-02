package com.windscribe.vpn.repository

import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.GetMyIpResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import com.windscribe.vpn.state.VPNConnectionStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class IpRepository(
    private val scope: CoroutineScope,
    private val preferenceHelper: PreferencesHelper,
    private val apiCallManagerV2: IApiCallManager,
    private val vpnConnectionStateManager: VPNConnectionStateManager
) {
    private val logger = LoggerFactory.getLogger("data")
    private val _state = MutableSharedFlow<RepositoryState<String>>()
    val state = _state

    init {
        update()
        scope.launch {
            vpnConnectionStateManager.state.collectLatest {
                if (it.status == VPNState.Status.Connected) {
                    loadIpFromStorage()
                }
                if (it.status == VPNState.Status.Disconnected) {
                    delay(500)
                    update()
                }
            }
        }
    }

    fun update() {
        scope.launch {
            _state.emit(RepositoryState.Loading())
            if (WindUtilities.isOnline()) {
                val result = result<GetMyIpResponse> {
                    apiCallManagerV2.getApiIp()
                }
                when (result) {
                    is CallResult.Error -> loadIpFromStorage()
                    is CallResult.Success -> {
                        val ipAddress = getModifiedIpAddress(result.data.userIp.trim())
                        preferenceHelper.userIP = ipAddress
                        _state.emit(RepositoryState.Success(ipAddress))
                    }
                }
            } else {
                loadIpFromStorage()
            }
        }
    }

    private suspend fun loadIpFromStorage() {
        preferenceHelper.userIP?.let {
            _state.emit(RepositoryState.Success(it))
        } ?: kotlin.run {
            _state.emit(RepositoryState.Error("No saved ip found."))
        }
    }

    private fun getModifiedIpAddress(ipResponse: String): String {
        var ipAddress: String?
        if (ipResponse.length >= 32) {
            logger.info("Ipv6 address. Truncating and saving ip data...")
            ipAddress = ipResponse.replace("0000".toRegex(), "0")
            ipAddress = ipAddress.replace("000".toRegex(), "")
            ipAddress = ipAddress.replace("00".toRegex(), "")
        } else {
            ipAddress = ipResponse
        }
        return ipAddress
    }
}