/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.wireguard

import android.content.Intent
import android.net.VpnService
import com.windscribe.common.DNSDetails
import com.windscribe.common.startSafeForeground
import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.ProxyDNSManager
import com.windscribe.vpn.backend.VPNState.Status.Connecting
import com.windscribe.vpn.backend.utils.WindNotificationBuilder
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.constants.NotificationConstants
import com.windscribe.vpn.state.ShortcutStateManager
import com.wireguard.android.backend.GoBackend
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import javax.inject.Inject

class WireGuardWrapperService : GoBackend.VpnService() {

    @Inject
    lateinit var windNotificationBuilder: WindNotificationBuilder

    @Inject
    lateinit var serviceInteractor: ServiceInteractor

    @Inject
    lateinit var wireguardBackend: WireguardBackend

    @Inject
    lateinit var vpnController: WindVpnController

    @Inject
    lateinit var shortcutStateManager: ShortcutStateManager

    @Inject
    lateinit var proxyDNSManager: ProxyDNSManager

    @Inject
    lateinit var preferencesHelper: PreferencesHelper

    private var logger = LoggerFactory.getLogger("vpn")

    override fun onCreate() {
        Windscribe.appContext.serviceComponent.inject(this)
        super.onCreate()
        wireguardBackend.serviceCreated(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.action == VpnService.SERVICE_INTERFACE) {
            logger.debug("System relaunched service, starting shortcut state manager")
            shortcutStateManager.connect()
            stopSelf()
            return START_NOT_STICKY
        }
        startSafeForeground(
            NotificationConstants.SERVICE_NOTIFICATION_ID,
            windNotificationBuilder.buildNotification(Connecting)
        )
        return if (serviceInteractor.preferenceHelper.globalUserConnectionPreference) {
            START_STICKY
        } else {
            START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        windNotificationBuilder.cancelNotification(NotificationConstants.SERVICE_NOTIFICATION_ID)
        wireguardBackend.serviceDestroyed()
        super.onDestroy()
    }

    fun close() {
        stopForeground(false)
        stopSelf()
    }

    override fun onRevoke() {
        wireguardBackend.scope.launch { vpnController.disconnectAsync() }
    }

    override fun getDnsDetails(): DNSDetails? {
        return proxyDNSManager.dnsDetails
    }
}
