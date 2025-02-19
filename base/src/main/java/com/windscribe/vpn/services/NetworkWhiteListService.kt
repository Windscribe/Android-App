/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.IBinder
import com.windscribe.common.startSafeForeground
import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.backend.VPNState.Status.UnsecuredNetwork
import com.windscribe.vpn.backend.utils.WindNotificationBuilder
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.constants.NotificationConstants
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.state.NetworkInfoListener
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import javax.inject.Inject

class NetworkWhiteListService : Service(), NetworkInfoListener {

    @Inject
    lateinit var scope: CoroutineScope

    @Inject
    lateinit var interactor: ServiceInteractor

    @Inject
    lateinit var notificationBuilder: WindNotificationBuilder

    @Inject
    lateinit var windVpnController: WindVpnController

    @Inject
    lateinit var networkInfoManager: NetworkInfoManager

    @Inject
    lateinit var vpnConnectionStateManager: VPNConnectionStateManager

    private val binder = Binder()
    private val logger = LoggerFactory.getLogger(TAG)
    override fun onCreate() {
        super.onCreate()
        appContext.serviceComponent.inject(this)
        logger.info("Check network service started")
    }

    override fun onDestroy() {
        networkInfoManager.removeNetworkInfoListener(this)
        logger.debug("Service on destroy.")
        if (!interactor.compositeDisposable.isDisposed) {
            interactor.compositeDisposable.dispose()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onNetworkInfoUpdate(networkInfo: NetworkInfo?, userReload: Boolean) {
        if (!interactor.preferenceHelper.globalUserConnectionPreference) {
            logger.debug("New network available but user connection intent is false. now disconnecting")
            scope.launch {
                windVpnController.disconnectAsync()
            }
            return
        }
        networkInfo?.let {
            logger.debug("Network white list service > SSID: ${networkInfo.networkName} AutoSecure: ${networkInfo.isAutoSecureOn} Preferred Protocols: ${networkInfo.isPreferredOn} ${networkInfo.protocol} ${networkInfo.port} | Whitelisted network: ${interactor.preferenceHelper.whiteListedNetwork}")
            if (!it.isAutoSecureOn) {
                onTrustedNetworkFound()
            } else {
                onUntrustedNetworkFound()
            }
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        startSafeForeground(NotificationConstants.SERVICE_NOTIFICATION_ID, notificationBuilder.buildNotification(UnsecuredNetwork))
        if (intent.action != null && intent.action == DISCONNECT_ACTION) {
            stopService()
            return START_NOT_STICKY
        }
        if (intent.action != null && intent.action == UNSECURED_NETWORK_ACTION) {
            startSafeForeground(NotificationConstants.SERVICE_NOTIFICATION_ID, notificationBuilder.buildNotification(UnsecuredNetwork))
            networkInfoManager.addNetworkInfoListener(this)
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    private fun onTrustedNetworkFound() {
        scope.launch {
            windVpnController.disconnectAsync(waitForNextProtocol = true)
        }
    }

    private fun onUntrustedNetworkFound() {
        networkInfoManager.removeNetworkInfoListener(this)
        interactor.preferenceHelper.globalUserConnectionPreference = true
        windVpnController.connectAsync()
    }

    private fun stopService() {
        try {
            stopForeground(false)
            notificationBuilder.cancelNotification(NotificationConstants.SERVICE_NOTIFICATION_ID)
            stopSelf()
        } catch (e: Exception) {
            logger.debug("Failed to stop check network service:$e")
        }
    }

    companion object {

        const val UNSECURED_NETWORK_ACTION = "unsecured_network_action"
        const val DISCONNECT_ACTION = "disconnect_action"
        private const val TAG = "vpn"
        fun startService(context: Context) {
            val startIntent = Intent(context, NetworkWhiteListService::class.java)
            startIntent.action = UNSECURED_NETWORK_ACTION
            if (VERSION.SDK_INT >= VERSION_CODES.O) {
                context.startForegroundService(startIntent)
            } else {
                context.startService(startIntent)
            }
        }

        fun stopService(context: Context) {
            val startIntent = Intent(context, NetworkWhiteListService::class.java)
            startIntent.action = DISCONNECT_ACTION
            if (VERSION.SDK_INT >= VERSION_CODES.O) {
                context.startForegroundService(startIntent)
            } else {
                context.startService(startIntent)
            }
        }
    }
}
