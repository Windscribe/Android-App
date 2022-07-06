/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.services

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentWorkAroundService
import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.state.VPNConnectionStateManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class DeviceStateService : JobIntentWorkAroundService() {

    @Inject
    lateinit var interactor: ServiceInteractor

    @Inject
    lateinit var vpnController: WindVpnController

    @Inject
    lateinit var scope: CoroutineScope

    @Inject
    lateinit var vpnConnectionStateManager: VPNConnectionStateManager

    private val logger = LoggerFactory.getLogger("network_state_service")
    private val stateBoolean = AtomicBoolean()
    override fun onCreate() {
        super.onCreate()
        stateBoolean.set(true)
        appContext.serviceComponent.inject(this)
    }

    override fun onDestroy() {
        interactor.compositeDisposable.dispose()
        super.onDestroy()
    }

    override fun onHandleWork(intent: Intent) {
        if (stateBoolean.getAndSet(false)) {
            val networkInfo = WindUtilities.getUnderLayNetworkInfo()
            if (networkInfo != null && networkInfo.isConnected && vpnConnectionStateManager.isVPNActive()) {
                addToKnownNetworks()
            }
        }
    }

    private fun addToKnownNetworks() {
        try {
            val networkName = WindUtilities.getNetworkName()
            interactor.compositeDisposable.add(
                    interactor.getNetwork(networkName)
                            .onErrorResumeNext {
                                interactor
                                        .addNetworkToKnown(networkName).flatMap {
                                            interactor
                                                    .getNetwork(networkName)
                                        }
                            }.subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeWith(
                                    object : DisposableSingleObserver<NetworkInfo?>() {
                                        override fun onError(ignored: Throwable) {
                                            logger.debug("Ignore: no network information for network name: $networkName")
                                            interactor.compositeDisposable.dispose()
                                        }

                                        override fun onSuccess(
                                                networkInfo: NetworkInfo
                                        ) {
                                            if (!networkInfo.isAutoSecureOn && !interactor
                                                            .preferenceHelper.whitelistOverride
                                            ) {
                                                logger.debug(
                                                        "Underlying network to current vpn connection is unsecured. Starting standby service."
                                                )
                                                scope.launch {
                                                    vpnController.disconnect(true)
                                                }
                                            }
                                            interactor.compositeDisposable.dispose()
                                        }
                                    })
            )
        } catch (e: Exception) {
            logger.debug("unable to get network name.")
        }
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
