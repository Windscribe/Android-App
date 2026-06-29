/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.openvpn

import android.content.Intent
import android.net.VpnService
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.VPNState.Status.Connecting
import com.windscribe.vpn.backend.utils.ExcludedIpHolder
import com.windscribe.vpn.backend.utils.WindNotificationBuilder
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.backend.utils.startForegroundImmediately
import com.windscribe.vpn.backend.utils.startForegroundSafely
import com.windscribe.vpn.constants.NotificationConstants
import com.windscribe.vpn.state.ShortcutStateManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import dagger.hilt.android.AndroidEntryPoint
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.VpnStatus
import de.blinkt.openvpn.core.VpnStatus.StateListener
import org.slf4j.LoggerFactory
import javax.inject.Inject

@AndroidEntryPoint
class OpenVPNWrapperService :
    OpenVPNService(),
    StateListener {
    @Inject
    lateinit var windNotificationBuilder: WindNotificationBuilder

    @Inject
    lateinit var preferencesHelper: PreferencesHelper

    @Inject
    lateinit var vpnController: WindVpnController

    @Inject
    lateinit var openVPNBackend: OpenVPNBackend

    @Inject
    lateinit var vpnConnectionStateManager: VPNConnectionStateManager

    @Inject
    lateinit var shortcutStateManager: ShortcutStateManager

    @Inject
    lateinit var excludedIpHolder: ExcludedIpHolder

    private var logger = LoggerFactory.getLogger("vpn")

    override fun onCreate() {
        logger.debug("OpenVPNWrapperService onCreate()")
        startForegroundImmediately(NotificationConstants.SERVICE_NOTIFICATION_ID)
        super.onCreate()
        startForegroundSafely(
            windNotificationBuilder,
            NotificationConstants.SERVICE_NOTIFICATION_ID,
            Connecting,
        )
        openVPNBackend.serviceCreated(this)
        VpnStatus.addStateListener(this)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent == null || intent.action == VpnService.SERVICE_INTERFACE) {
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
        return super.onStartCommand(intent, flags, startId)
    }

    override fun getProfile(): VpnProfile? = Util.getProfile<VpnProfile>()

    override fun onProcessRestore(): Boolean = preferencesHelper.globalUserConnectionPreference

    override fun onDestroy() {
        logger.debug("OpenVPNWrapperService onDestroy()")
        VpnStatus.removeStateListener(this)
        windNotificationBuilder.cancelNotification(NotificationConstants.SERVICE_NOTIFICATION_ID)
        openVPNBackend.serviceDestroyed()
        super.onDestroy()
    }

    override fun protect(socket: Int): Boolean = super.protect(socket)

    override fun updateState(
        state: String?,
        logmessage: String?,
        localizedResId: Int,
        level: ConnectionStatus?,
        intent: Intent?,
    ) {
        level?.let { status ->
            logger.debug("OpenVPN state changed: $status")
            openVPNBackend.getTunnel().onConnectionStatusChange(status)?.let { tunnelState ->
                openVPNBackend.getTunnel().onStateChange(tunnelState)
            }
        }
    }

    override fun setConnectedVPN(uuid: String?) {
    }

    fun stopVPN() {
        logger.debug("stopVPN() called")
        try {
            super.stopVPN(false)
        } catch (e: Exception) {
            logger.error("Error stopping OpenVPN tunnel: ${e.message}")
        }
    }

    fun close() {
        logger.debug("close() called")
        openVPNBackend.getTunnel().onStateChange(OpenVpnTunnel.State.DOWN)
        stopSelf()
    }

    override fun applyExcludedRoutes(builder: Builder) {
        excludedIpHolder.applyExcludedRoutes(builder)
    }
}
