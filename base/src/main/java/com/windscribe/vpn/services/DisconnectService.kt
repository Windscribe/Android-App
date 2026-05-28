/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.state.VPNConnectionStateManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import javax.inject.Inject

@AndroidEntryPoint
class DisconnectService : Service() {
    @Inject
    lateinit var controller: WindVpnController

    @Inject
    lateinit var preferencesHelper: PreferencesHelper

    @Inject
    lateinit var vpnConnectionStateManager: VPNConnectionStateManager

    @Inject
    lateinit var scope: CoroutineScope

    private val logger = LoggerFactory.getLogger("vpn")

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        intent?.let {
            scope.launch {
                logger.info("Stopping vpn services from notification.")
                preferencesHelper.globalUserConnectionPreference = false
                controller.disconnectAsync()
            }
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
