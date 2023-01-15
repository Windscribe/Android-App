/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.ikev2

import android.app.Notification
import android.content.Intent
import android.net.VpnService
import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.VPNState.Status.Connecting
import com.windscribe.vpn.backend.utils.WindNotificationBuilder
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.constants.NotificationConstants
import com.windscribe.vpn.state.ShortcutStateManager
import kotlinx.coroutines.CoroutineScope
import org.strongswan.android.data.VpnProfile
import org.strongswan.android.logic.CharonVpnService
import javax.inject.Inject

class CharonVpnServiceWrapper : CharonVpnService() {

    @Inject
    lateinit var windNotificationBuilder: WindNotificationBuilder

    @Inject
    lateinit var serviceInteractor: ServiceInteractor

    @Inject
    lateinit var vpnController: WindVpnController

    @Inject
    lateinit var iKev2VpnBackend: IKev2VpnBackend

    @Inject
    lateinit var shortcutStateManager: ShortcutStateManager

    @Inject
    lateinit var scope: CoroutineScope

    override fun onCreate() {
        appContext.serviceComponent.inject(this)
        super.onCreate()
    }

    override fun getMainActivityClass(): Class<*> {
        return appContext.applicationInterface.homeIntent.component!!.javaClass
    }

    override fun buildNotification(publicVersion: Boolean): Notification {
        return windNotificationBuilder.buildNotification(Connecting)
    }

    override fun onDestroy() {
        windNotificationBuilder.cancelNotification(NotificationConstants.SERVICE_NOTIFICATION_ID)
        super.onDestroy()
    }

    override fun getNotificationID(): Int {
        return NotificationConstants.SERVICE_NOTIFICATION_ID
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            VpnService.SERVICE_INTERFACE -> {
                shortcutStateManager.connect()
                START_NOT_STICKY
            }
            DISCONNECT_ACTION -> {
                startForeground(NotificationConstants.SERVICE_NOTIFICATION_ID, windNotificationBuilder.buildNotification(Connecting))
                stopForeground(false)
                setNextProfile(null)
                START_NOT_STICKY
            }
            else -> {
                startForeground(NotificationConstants.SERVICE_NOTIFICATION_ID, windNotificationBuilder.buildNotification(Connecting))
                Util.getProfile<VpnProfile>()?.let {
                    setNextProfile(it)
                    START_STICKY
                } ?: kotlin.run {
                    START_NOT_STICKY
                }
            }
        }
    }
}
