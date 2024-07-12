package com.windscribe.vpn.services

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.windscribe.common.startSafeForeground
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.utils.WindNotificationBuilder
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.constants.NotificationConstants
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.model.User
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.state.NetworkInfoListener
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import javax.inject.Inject

class AutoConnectService : Service(), NetworkInfoListener {

    @Inject
    lateinit var autoConnectionManager: AutoConnectionManager

    @Inject
    lateinit var networkInfoManager: NetworkInfoManager

    @Inject
    lateinit var windNotificationBuilder: WindNotificationBuilder

    @Inject
    lateinit var vpnConnectionStateManager: VPNConnectionStateManager

    @Inject
    lateinit var vpnController: WindVpnController

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var preferencesHelper: PreferencesHelper

    private var serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var logger = LoggerFactory.getLogger("auto_connect_s")

    companion object {
        var isAutoConnectingServiceRunning = false
    }

    override fun onCreate() {
        isAutoConnectingServiceRunning = true
        appContext.serviceComponent.inject(this)
        networkInfoManager.addNetworkInfoListener(this)
        serviceScope.launch {
            vpnConnectionStateManager.state.collectLatest {
                if (it.status == VPNState.Status.Connected || it.status == VPNState.Status.Connecting) {
                    logger.debug("VPN connection is successful. Stopping auto connect service.")
                    stopAutoConnectService()
                }
            }
        }
        super.onCreate()
    }

    override fun onDestroy() {
        logger.debug("Auto connect service on exit.")
        serviceScope.coroutineContext.cancelChildren()
        kotlin.runCatching { networkInfoManager.removeNetworkInfoListener(this) }
        isAutoConnectingServiceRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        return Binder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = windNotificationBuilder.buildNotification(VPNState.Status.UnsecuredNetwork)
        notification.contentIntent = null
        notification.actions = null
        startSafeForeground(NotificationConstants.AUTO_CONNECT_SERVICE_NOTIFICATION_ID, notification)
        return if (canAccessNetworkName()) {
            logger.debug("Auto connect service started and waiting for network changes.")
            START_STICKY
        } else {
            logger.debug("Location permissions are denied, stopping auto connect service.")
            stopAutoConnectService()
            START_NOT_STICKY
        }
    }

    override fun onNetworkInfoUpdate(networkInfo: NetworkInfo?, userReload: Boolean) {
        if (preferencesHelper.autoConnect) {
            if (networkInfo?.isAutoSecureOn == true && vpnConnectionStateManager.state.value.status == VPNState.Status.Disconnected && userRepository.user.value?.accountStatus == User.AccountStatus.Okay) {
                logger.debug("Auto secured turned on for SSID: ${networkInfo.networkName} and connecting to VPN")
                vpnController.connectAsync()
            } else if (networkInfo?.isAutoSecureOn == false && vpnConnectionStateManager.state.value.status == VPNState.Status.Connected) {
                logger.debug("Auto secured turned off for SSID: ${networkInfo.networkName} and disconnecting from VPN.")
                vpnController.disconnectAsync()
            }
        } else {
            stopAutoConnectService()
        }
    }
}

fun Context.startAutoConnectService() {
    if (AutoConnectService.isAutoConnectingServiceRunning.not()) {
        val intent = Intent(this, AutoConnectService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}

fun Context.stopAutoConnectService() {
    val intent = Intent(this, AutoConnectService::class.java)
    stopService(intent)
}

fun Context.canAccessNetworkName(): Boolean {
    val isBackgroundPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    } else {
        return true
    }
    val isForegroundPermissionGranted = checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return isBackgroundPermissionGranted && isForegroundPermissionGranted
}