/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Action
import androidx.core.app.NotificationCompat.Builder
import com.windscribe.vpn.R.drawable
import com.windscribe.vpn.R.mipmap
import com.windscribe.vpn.R.string
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.TrafficCounter
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.VPNState.Status
import com.windscribe.vpn.backend.VPNState.Status.Connected
import com.windscribe.vpn.backend.VPNState.Status.Connecting
import com.windscribe.vpn.backend.VPNState.Status.Disconnected
import com.windscribe.vpn.backend.VPNState.Status.ProtocolSwitch
import com.windscribe.vpn.backend.VPNState.Status.UnsecuredNetwork
import com.windscribe.vpn.constants.NotificationConstants
import com.windscribe.vpn.repository.ServerListRepository
import com.windscribe.vpn.services.DisconnectService
import com.windscribe.vpn.state.VPNConnectionStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WindNotificationBuilder @Inject constructor(
    private val notificationManager: NotificationManager,
    private val notificationBuilder: Builder,
    private val vpnConnectionStateManager: VPNConnectionStateManager,
    private val trafficCounter: TrafficCounter,
    val scope: CoroutineScope,
    val serverListRepository: ServerListRepository,
    private val preferencesHelper: PreferencesHelper
) {

    private companion object {
        const val DISCONNECT_ACTION_REQUEST_CODE = 200
        const val CONTENT_INTENT_REQUEST_CODE = 0
        const val CHANNEL_NAME = "WindScribe"
        const val CHANNEL_DESCRIPTION =
            "Provides information about the VPN connection state and serves as permanent notification to keep the VPN service running in the background."
        const val DISCONNECT_ACTION_LABEL = "Disconnect"
        const val PROTOCOL_SWITCH_MESSAGE = "Waiting for protocol switch"
        const val UNSECURED_NETWORK_MESSAGE = "Waiting for secured network"
    }

    private var lastUpdateTime = System.currentTimeMillis()
    private var trafficStats: String? = null
    private val logger = LoggerFactory.getLogger("vpn")

    fun buildNotification(status: Status): Notification {
        val location = notificationTitle
        val icon = getDefaultIcon(status)
        val message = getStatusMessage(status, location)
        val stats = if (status == Connected) trafficStats else null

        updateNotification(icon, message, stats)
        return notificationBuilder.build()
    }

    private fun getStatusMessage(status: Status, location: String?): String {
        return when (status) {
            Connected -> getString(string.connected_to, location)
            Connecting -> getString(string.connecting_to, location)
            ProtocolSwitch -> PROTOCOL_SWITCH_MESSAGE
            UnsecuredNetwork -> UNSECURED_NETWORK_MESSAGE
            else -> getString(string.connected_lower_case)
        }
    }

    private fun getString(resId: Int, vararg formatArgs: Any?): String {
        return Windscribe.appContext.getString(resId, *formatArgs)
    }

    fun cancelNotification(notificationId: Int) {
        try {
            notificationManager.cancel(notificationId)
            notificationManager.cancelAll()
        } catch (e: Exception) {
            logger.debug(e.toString())
        }
    }

    private fun updateNotification(iconId: Int, textUpdate: String?, trafficStats: String?) {
        notificationBuilder.apply {
            setContentText(trafficStats?.takeIf { it.isNotEmpty() })
            setWhen(lastUpdateTime)
            setShowWhen(true)
            setUsesChronometer(true)
            setSmallIcon(iconId)
            setContentTitle(textUpdate)
            setOngoing(true)
        }
    }

    private val notificationTitle: String?
        get() = try {
            Util.getLastSelectedLocation(Windscribe.appContext)?.let { location ->
                val cityName =
                    serverListRepository.getCustomCityName(location.cityId) ?: location.nodeName
                val nickName =
                    serverListRepository.getCustomCityNickName(location.cityId) ?: location.nickName
                "$cityName - $nickName"
            }
        } catch (e: Exception) {
            logger.debug("Error getting notification title", e)
            null
        }

    private fun setContentIntent() {
        val context = Windscribe.appContext

        val disconnectPendingIntent = createDisconnectPendingIntent(context)
        val disconnectAction = Action.Builder(
            mipmap.connected,
            DISCONNECT_ACTION_LABEL,
            disconnectPendingIntent
        ).build()
        notificationBuilder.addAction(disconnectAction)

        val contentPendingIntent = createContentPendingIntent(context)
        notificationBuilder.setContentIntent(contentPendingIntent)
    }

    private fun createDisconnectPendingIntent(context: Windscribe): PendingIntent {
        val serviceIntent = Intent(context, DisconnectService::class.java)
        val flags = if (VERSION.SDK_INT >= VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
        return PendingIntent.getService(
            context,
            DISCONNECT_ACTION_REQUEST_CODE,
            serviceIntent,
            flags
        )
    }

    private fun createContentPendingIntent(context: Windscribe): PendingIntent {
        val contentIntent = context.applicationInterface.splashIntent.apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val flags = if (VERSION.SDK_INT >= VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(
            context,
            CONTENT_INTENT_REQUEST_CODE,
            contentIntent,
            flags
        )
    }

    private fun setupNotificationLegacy() {
        notificationBuilder.apply {
            setSmallIcon(getDefaultIcon(Connecting))
            setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            setOnlyAlertOnce(true)
            setOngoing(true)
        }
        setContentIntent()
    }

    @RequiresApi(VERSION_CODES.N)
    private fun setupNotificationN() {
        notificationBuilder.apply {
            setSmallIcon(getDefaultIcon(Connecting))
            setOnlyAlertOnce(true)
            setOngoing(true)
        }
        setContentIntent()
    }

    @RequiresApi(VERSION_CODES.O)
    private fun setupNotificationO() {
        createNotificationChannel()

        notificationBuilder.apply {
            setSmallIcon(getDefaultIcon(Connecting))
            setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            setChannelId(NotificationConstants.NOTIFICATION_CHANNEL_ID)
            setOnlyAlertOnce(true)
        }
        setContentIntent()
    }

    @RequiresApi(VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NotificationConstants.NOTIFICATION_CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = CHANNEL_DESCRIPTION
            enableLights(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    init {
        createDefaultNotification()
        observeConnectionState()
        observeTrafficStats()
        observeIconChanges()
    }

    private fun observeConnectionState() {
        scope.launch {
            vpnConnectionStateManager.state.collectLatest { state ->
                when (state.status) {
                    Disconnected -> {
                        trafficStats = null
                        lastUpdateTime = System.currentTimeMillis()
                        notificationManager.cancel(NotificationConstants.SERVICE_NOTIFICATION_ID)
                    }

                    else -> {
                        notificationManager.notify(
                            NotificationConstants.SERVICE_NOTIFICATION_ID,
                            buildNotification(state.status)
                        )
                    }
                }
            }
        }
    }

    private fun observeTrafficStats() {
        scope.launch {
            trafficCounter.trafficStats.collectLatest { traffic ->
                trafficStats = traffic.text
                val status = vpnConnectionStateManager.state.value.status
                if (status == Connected && preferencesHelper.globalUserConnectionPreference) {
                    notificationManager.notify(
                        NotificationConstants.SERVICE_NOTIFICATION_ID,
                        buildNotification(status)
                    )
                }
            }
        }
    }

    private fun observeIconChanges() {
        scope.launch {
            preferencesHelper.customIconFlow.distinctUntilChanged().collectLatest {
                // Icon changed, need to update the notification's PendingIntent
                val status = vpnConnectionStateManager.state.value.status
                if (status == Connected && isNotificationActive()) {
                    notificationBuilder.clearActions()
                    setContentIntent()
                    notificationManager.notify(
                        NotificationConstants.SERVICE_NOTIFICATION_ID,
                        buildNotification(status)
                    )
                }
            }
        }
    }

    private fun isNotificationActive(): Boolean {
        return try {
            if (VERSION.SDK_INT >= VERSION_CODES.M) {
                notificationManager.activeNotifications.any {
                    it.id == NotificationConstants.SERVICE_NOTIFICATION_ID
                }
            } else {
                return false
            }
        } catch (e: Exception) {
            logger.debug("Failed to check active notifications", e)
            false
        }
    }

    private fun getDefaultIcon(status: Status): Int = when (status) {
        Connecting -> mipmap.connecting
        Connected -> mipmap.connected
        ProtocolSwitch, UnsecuredNetwork -> mipmap.connection_error
        else -> mipmap.disconnected
    }

    private fun createDefaultNotification() {
        when {
            VERSION.SDK_INT >= VERSION_CODES.O -> setupNotificationO()
            VERSION.SDK_INT >= VERSION_CODES.N -> setupNotificationN()
            else -> setupNotificationLegacy()
        }
    }
}
