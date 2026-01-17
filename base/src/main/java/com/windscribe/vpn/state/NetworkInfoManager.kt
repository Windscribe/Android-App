/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.state

import android.R.attr.name
import android.util.Log
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import javax.inject.Singleton

@Singleton
class NetworkInfoManager(
    private val preferencesHelper: PreferencesHelper,
    private val localDbInterface: LocalDbInterface,
    private val deviceStateManager: DeviceStateManager
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val logger = LoggerFactory.getLogger("network-info-manager")

    private val _networkInfo = MutableStateFlow<NetworkInfo?>(null)
    val networkInfo: StateFlow<NetworkInfo?> = _networkInfo.asStateFlow()

    fun reload() {
        scope.launch {
            val currentDetail = deviceStateManager.networkDetail.value
            if (currentDetail == null) {
                _networkInfo.emit(null)
                return@launch
            }
            try {
                var network = withContext(Dispatchers.IO) {
                    localDbInterface.getNetwork(currentDetail.name)
                }

                if (network == null) {
                    withContext(Dispatchers.IO) {
                        localDbInterface.addNetwork(preferencesHelper.getDefaultNetworkInfo(currentDetail.name))
                    }
                    network = withContext(Dispatchers.IO) {
                        localDbInterface.getNetwork(currentDetail.name)
                    }
                }
                _networkInfo.emit(network)
            } catch (e: Exception) {
                logger.error("Error reloading network: ${e.message}")
                _networkInfo.emit(null)
            }
        }
    }

    init {
        scope.launch {
            deviceStateManager.networkDetail.collect { detail ->
                if (detail != null) {
                    logger.info("Network changed: ${detail.name} (${detail.type})")
                    reload()
                } else {
                    // No network detail available (offline or no network name)
                    logger.info("Network detail unavailable")
                    _networkInfo.emit(null)
                }
            }
        }
    }
}
