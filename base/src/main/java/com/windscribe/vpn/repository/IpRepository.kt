package com.windscribe.vpn.repository

import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.GetMyIpResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.state.VPNConnectionStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class IpRepository(
    private val scope: CoroutineScope,
    private val preferenceHelper: PreferencesHelper,
    private val apiCallManagerV2: IApiCallManager,
    private val vpnConnectionStateManager: VPNConnectionStateManager,
    private val isOnline: StateFlow<Boolean>,
) {
    private val logger = LoggerFactory.getLogger("data")
    private val events = MutableSharedFlow<RepositoryEvent>()

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<RepositoryState<String>> =
        merge(
            // manual refresh
            events,
            // refresh on vpn state changes
            vpnConnectionStateManager.state.map {
                RepositoryEvent.Refresh
            },
        ).onStart {
            emit(RepositoryEvent.Refresh)
        }.mapLatest { event ->
            if (event is RepositoryEvent.ForceRefresh) {
                return@mapLatest refreshIp()
            }
            when (vpnConnectionStateManager.state.value.status) {
                VPNState.Status.Connected -> {
                    loadIpFromStorage()
                }

                VPNState.Status.Disconnected -> {
                    delay(500)
                    refreshIp()
                }

                else -> {
                    refreshIp()
                }
            }
        }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = RepositoryState.Loading(),
        )

    fun update() {
        scope.launch {
            events.emit(RepositoryEvent.ForceRefresh)
        }
    }

    private suspend fun refreshIp(): RepositoryState<String> {
        if (!isOnline.value) {
            return loadIpFromStorage()
        }
        return when (val result = result<GetMyIpResponse> { apiCallManagerV2.getApiIp() }) {
            is CallResult.Error -> {
                loadIpFromStorage()
            }

            is CallResult.Success -> {
                val ipAddress = getModifiedIpAddress(result.data.userIp?.trim() ?: "")
                preferenceHelper.userIP = ipAddress
                RepositoryState.Success(ipAddress)
            }
        }
    }

    private fun loadIpFromStorage(): RepositoryState<String> {
        val ip = preferenceHelper.userIP
        return if (ip != null) {
            RepositoryState.Success(ip)
        } else {
            RepositoryState.Error("No saved ip found.")
        }
    }

    private fun getModifiedIpAddress(ipResponse: String): String =
        if (ipResponse.length >= 32) {
            logger.info("Ipv6 address. Truncating and saving ip data...")
            ipResponse
                .replace("0000".toRegex(), "0")
                .replace("000".toRegex(), "")
                .replace("00".toRegex(), "")
        } else {
            ipResponse
        }
}
