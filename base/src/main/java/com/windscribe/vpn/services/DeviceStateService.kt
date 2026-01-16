/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.services

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentWorkAroundService
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.state.DeviceStateManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class DeviceStateService : JobIntentWorkAroundService() {

    @Inject
    lateinit var vpnController: WindVpnController

    @Inject
    lateinit var scope: CoroutineScope

    @Inject
    lateinit var preferencesHelper: PreferencesHelper

    @Inject
    lateinit var vpnConnectionStateManager: VPNConnectionStateManager

    @Inject
    lateinit var localDbInterface: LocalDbInterface

    @Inject
    lateinit var deviceStateManager: DeviceStateManager

    private val logger = LoggerFactory.getLogger("device-state-service")
    private val stateBoolean = AtomicBoolean()
    override fun onCreate() {
        super.onCreate()
        stateBoolean.set(true)
        appContext.serviceComponent.inject(this)
    }

    override fun onHandleWork(intent: Intent) {
        if (stateBoolean.getAndSet(false)) {
            val networkInfo = WindUtilities.getUnderLayNetworkInfo()
            if (networkInfo != null && networkInfo.isConnected && vpnConnectionStateManager.isVPNActive()) {
                logger.info("New network detected. VPN is connected. Checking for SSID.")
                val networkName = deviceStateManager.networkDetail.value?.name
                if (networkName != null) {
                    addToKnownNetworks(networkName)
                }
            }
        }
    }

    private fun addToKnownNetworks(networkName: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val network = localDbInterface.getNetwork(networkName)
                if (network == null) {
                    logger.info("Saving $networkName(SSID) to database.")
                    localDbInterface.addNetwork(preferencesHelper.getDefaultNetworkInfo(networkName))
                    val newNetwork = localDbInterface.getNetwork(networkName)
                    if (newNetwork != null) {
                        resetConnectState(newNetwork)
                    }
                } else {
                    resetConnectState(network)
                }
            } catch (e: Exception) {
                logger.info("Ignore: no network information for network name: $networkName")
            }
        }
    }

    private fun resetConnectState(networkInfo: NetworkInfo) {
        logger.info("SSID: ${networkInfo.networkName} AutoSecure: ${networkInfo.isAutoSecureOn} Preferred Protocols: ${networkInfo.isPreferredOn} ${networkInfo.protocol} ${networkInfo.port} | Whitelisted network: ${preferencesHelper.whiteListedNetwork} | Connect Intent: ${preferencesHelper.globalUserConnectionPreference}")
        if (networkInfo.isAutoSecureOn.not() && preferencesHelper.whiteListedNetwork != networkInfo.networkName && vpnConnectionStateManager.state.value.status == VPNState.Status.Connected) {
            logger.debug("${networkInfo.networkName} is unsecured. Starting network whitelist service.")
            vpnController.disconnectAsync(true)
        }
        if (vpnConnectionStateManager.state.value.status == VPNState.Status.Connected && preferencesHelper.whiteListedNetwork != networkInfo.networkName) {
            preferencesHelper.whiteListedNetwork = null
        }
    }

    companion object {

        private const val JOB_ID = 7877

        @JvmStatic
        fun enqueueWork(context: Context) {
            enqueueWork(
                context, DeviceStateService::class.java, JOB_ID,
                Intent(
                    context,
                    DeviceStateService::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
