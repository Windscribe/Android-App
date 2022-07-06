/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.openvpn

import android.content.Intent
import android.net.VpnService
import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.VPNState.Status.Connecting
import com.windscribe.vpn.backend.utils.WindNotificationBuilder
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.constants.NotificationConstants
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.VpnStatus.StateListener
import javax.inject.Inject
import org.slf4j.LoggerFactory

class OpenVPNWrapperService : OpenVPNService(), StateListener {

    @Inject
    lateinit var windNotificationBuilder: WindNotificationBuilder

    @Inject
    lateinit var serviceInteractor: ServiceInteractor

    @Inject
    lateinit var vpnController: WindVpnController

    private var logger = LoggerFactory.getLogger("open_vpn_wrapper")

    override fun onCreate() {
        Windscribe.appContext.serviceComponent.inject(this)
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.debug("Launching open VPN Service")
        if (intent != null && intent.action == VpnService.SERVICE_INTERFACE) {
            vpnController.connect(alwaysOnVPN = true)
            START_NOT_STICKY
        }
        startForeground(NotificationConstants.SERVICE_NOTIFICATION_ID, windNotificationBuilder.buildNotification(Connecting))
        return super.onStartCommand(intent, flags, startId)
    }

    override fun getProfile(): VpnProfile? {
        return Util.getProfile<VpnProfile>()
    }

    override fun onProcessRestore(): Boolean {
        return serviceInteractor.preferenceHelper.globalUserConnectionPreference
    }

    override fun onDestroy() {
        windNotificationBuilder.cancelNotification(NotificationConstants.SERVICE_NOTIFICATION_ID)
        super.onDestroy()
    }
}
