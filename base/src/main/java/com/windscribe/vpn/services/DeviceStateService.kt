/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.services

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentWorkAroundService
import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.exceptions.WindScribeException
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.state.VPNConnectionStateManager
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class DeviceStateService : JobIntentWorkAroundService() {

    @Inject
    lateinit var interactor: ServiceInteractor

    @Inject
    lateinit var vpnController: WindVpnController

    @Inject
    lateinit var scope: CoroutineScope

    @Inject
    lateinit var preferencesHelper: PreferencesHelper

    @Inject
    lateinit var vpnConnectionStateManager: VPNConnectionStateManager

    private val logger = LoggerFactory.getLogger("network_status")
    private val stateBoolean = AtomicBoolean()
    private val compositeDisposable = CompositeDisposable()
    override fun onCreate() {
        super.onCreate()
        stateBoolean.set(true)
        appContext.serviceComponent.inject(this)
    }

    override fun onHandleWork(intent: Intent) {
        if (stateBoolean.getAndSet(false)) {
            compositeDisposable.clear()
            val networkInfo = WindUtilities.getUnderLayNetworkInfo()
            if (networkInfo != null) {
                logger.debug("Network: ${networkInfo.detailedState} | VPN: ${vpnConnectionStateManager.state.value.status.name}")
            }
            if (networkInfo != null && networkInfo.isConnected && vpnConnectionStateManager.isVPNActive()) {
                logger.debug("New network detected. VPN is connected. Checking for SSID.")
                getNetworkName()
            }
        }
    }

    private fun getNetworkName() {
        try {
            val networkName = WindUtilities.getNetworkName()
            addToKnownNetworks(networkName)
        } catch (e: WindScribeException) {
            logger.debug(e.message)
            compositeDisposable.clear()
        }
    }

    private fun addToKnownNetworks(networkName: String) {
        compositeDisposable.add(
                interactor.getNetwork(networkName)
                        .onErrorResumeNext {
                            logger.debug("Saving $networkName(SSID) to database.")
                            interactor.addNetworkToKnown(networkName).flatMap { interactor.getNetwork(networkName) }
                        }.subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe({
                            resetConnectState(it)
                        }, {
                            logger.debug("Ignore: no network information for network name: $networkName")
                            interactor.compositeDisposable.dispose()
                        }))
    }

    private fun resetConnectState(networkInfo: NetworkInfo) {
        logger.debug("SSID: ${networkInfo.networkName} AutoSecure: ${networkInfo.isAutoSecureOn} Preferred Protocols: ${networkInfo.isPreferredOn} ${networkInfo.protocol} ${networkInfo.port} | Whitelist override: ${preferencesHelper.whitelistOverride} | Connect Intent: ${preferencesHelper.globalUserConnectionPreference}")
        if (networkInfo.isAutoSecureOn.not() && preferencesHelper.whitelistOverride.not() && vpnConnectionStateManager.state.value.status == VPNState.Status.Connected) {
            logger.debug("${networkInfo.networkName} is unsecured. Starting network whitelist service.")
            vpnController.disconnectAsync(true)
        }
        if(vpnConnectionStateManager.state.value.status == VPNState.Status.Connected){
            preferencesHelper.whitelistOverride = false
        }
        compositeDisposable.clear()
    }

    companion object {

        private const val JOB_ID = 7877

        @JvmStatic
        fun enqueueWork(context: Context) {
            enqueueWork(
                    context, DeviceStateService::class.java, JOB_ID,
                    Intent(context, DeviceStateService::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
