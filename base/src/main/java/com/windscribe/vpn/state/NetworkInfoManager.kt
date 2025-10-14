/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.state

import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.state.DeviceStateManager.DeviceStateListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Singleton

@Singleton
class NetworkInfoManager(private val preferencesHelper: PreferencesHelper, private val localDbInterface: LocalDbInterface, deviceStateManager: DeviceStateManager) :
        DeviceStateListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    var networkInfo: NetworkInfo? = null
        private set
    private val listeners = ConcurrentLinkedQueue<NetworkInfoListener>()
    fun addNetworkInfoListener(networkInfoListener: NetworkInfoListener) {
        listeners.add(networkInfoListener)
    }

    override fun onNetworkStateChanged() {
        reloadCurrentNetwork(false)
    }

    fun reload(userReload: Boolean = false) {
        reloadCurrentNetwork(userReload)
    }

    fun removeNetworkInfoListener(networkInfoListener: NetworkInfoListener) {
        listeners.remove(networkInfoListener)
    }

    private fun reloadCurrentNetwork(userReload: Boolean) {
        scope.launch {
            try {
                val name = withContext(Dispatchers.IO) {
                    WindUtilities.getNetworkName()
                }

                var network = withContext(Dispatchers.IO) {
                    localDbInterface.getNetwork(name)
                }

                if (network == null) {
                    withContext(Dispatchers.IO) {
                        localDbInterface.addNetwork(preferencesHelper.getDefaultNetworkInfo(name))
                    }
                    network = withContext(Dispatchers.IO) {
                        localDbInterface.getNetwork(name)
                    }
                }

                networkInfo = network
                for (listener in listeners) {
                    listener.onNetworkInfoUpdate(networkInfo, userReload)
                }
            } catch (e: Exception) {
                networkInfo = null
                for (listener in listeners) {
                    listener.onNetworkInfoUpdate(null, userReload)
                }
            }
        }
    }

    init {
        deviceStateManager.addListener(this)
    }
}

interface NetworkInfoListener {

    fun onNetworkInfoUpdate(networkInfo: NetworkInfo?, userReload: Boolean)
}
