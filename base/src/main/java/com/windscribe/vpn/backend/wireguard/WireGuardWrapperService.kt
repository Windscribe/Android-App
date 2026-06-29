/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.wireguard

import android.content.Intent
import android.net.VpnService
import com.windscribe.common.DNSDetails
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.ProxyDNSManager
import com.windscribe.vpn.backend.VPNState.Status.Connecting
import com.windscribe.vpn.backend.utils.ExcludedIpHolder
import com.windscribe.vpn.backend.utils.WindNotificationBuilder
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.backend.utils.startForegroundImmediately
import com.windscribe.vpn.backend.utils.startForegroundSafely
import com.windscribe.vpn.constants.NotificationConstants
import com.windscribe.vpn.state.ShortcutStateManager
import com.wireguard.android.backend.GoBackend
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import javax.inject.Inject

@AndroidEntryPoint
class WireGuardWrapperService : GoBackend.VpnService() {
    @Inject
    lateinit var windNotificationBuilder: WindNotificationBuilder

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

    @Inject
    lateinit var excludedIpHolder: ExcludedIpHolder

    private var logger = LoggerFactory.getLogger("vpn")

    override fun onCreate() {
        // Promote to foreground IMMEDIATELY before DI to prevent
        // ForegroundServiceDidNotStartInTimeException on slow devices.
        startForegroundImmediately(NotificationConstants.SERVICE_NOTIFICATION_ID)
        super.onCreate()
        // Replace placeholder with full notification now that DI is complete.
        startForegroundSafely(
            windNotificationBuilder,
            NotificationConstants.SERVICE_NOTIFICATION_ID,
            Connecting,
        )
        wireguardBackend.serviceCreated(this)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent == null || intent.action == SERVICE_INTERFACE) {
            logger.debug("System relaunched service, starting shortcut state manager")
            startForegroundSafely(
                windNotificationBuilder,
                NotificationConstants.SERVICE_NOTIFICATION_ID,
                Connecting,
            )
            shortcutStateManager.connect()
            stopSelf()
            return START_NOT_STICKY
        }
        startForegroundSafely(
            windNotificationBuilder,
            NotificationConstants.SERVICE_NOTIFICATION_ID,
            Connecting,
        )
        return if (preferencesHelper.globalUserConnectionPreference) {
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
        // Don't call stopForeground() here - service must stay foreground until onDestroy()
        // to avoid ForegroundServiceDidNotStartInTimeException
        // onDestroy() will handle notification cleanup
        stopSelf()
    }

    override fun onRevoke() {
        wireguardBackend.scope.launch { vpnController.disconnectAsync() }
    }

    override fun getDnsDetails(): DNSDetails? = proxyDNSManager.dnsDetails

    override fun getControlDPort(): Int = proxyDNSManager.getListenPort()

    override fun applyExcludedRoutes(builder: Builder) {
        excludedIpHolder.applyExcludedRoutes(builder)
    }
}
