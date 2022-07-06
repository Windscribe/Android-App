/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.services

import android.app.IntentService
import android.content.Intent
import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.state.VPNConnectionStateManager
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class DisconnectService : IntentService("DisconnectService") {

    @Inject
    lateinit var controller: WindVpnController

    @Inject
    lateinit var disconnectServiceInteractor: ServiceInteractor

    @Inject
    lateinit var vpnConnectionStateManager: VPNConnectionStateManager

    @Inject
    lateinit var scope: CoroutineScope

    private val logger = LoggerFactory.getLogger("disconnect_service")

    override fun onCreate() {
        super.onCreate()
        Windscribe.appContext.serviceComponent.inject(this)
    }

    override fun onHandleIntent(intent: Intent?) {
        intent?.let {
            scope.launch {
                logger.info("Stopping vpn services from notification.")
                disconnectServiceInteractor.preferenceHelper.globalUserConnectionPreference = false
                controller.disconnect()
            }
        }
    }
}
