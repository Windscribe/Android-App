/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.utils

import android.annotation.TargetApi
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
import com.windscribe.vpn.R.mipmap
import com.windscribe.vpn.R.string
import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.backend.TrafficCounter
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.VPNState.Status
import com.windscribe.vpn.backend.VPNState.Status.*
import com.windscribe.vpn.constants.NotificationConstants
import com.windscribe.vpn.services.DisconnectService
import com.windscribe.vpn.state.VPNConnectionStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
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
        private val interactor: ServiceInteractor
) {

    private var lastUpdateTime = System.currentTimeMillis()
    private var trafficStats: String? = null
    private var logger = LoggerFactory.getLogger("vpn")

    fun buildNotification(status: Status): Notification {
        val location = notificationTitle
        when {
            status === Connected -> {
                updateNotification(
                        mipmap.connected,
                        Windscribe.appContext.getString(string.connected_to, location), trafficStats
                )
            }
            status === Disconnected -> {
                updateNotification(
                        mipmap.disconnected,
                        Windscribe.appContext.getString(string.connected_lower_case), null
                )
            }
            status === Connecting -> {
                updateNotification(
                        mipmap.connecting,
                        Windscribe.appContext.getString(string.connecting_to, location), null
                )
            }
            status == ProtocolSwitch -> {
                updateNotification(mipmap.connection_error, "Waiting for protocol switch", null)
            }
            status == UnsecuredNetwork -> {
                updateNotification(mipmap.connection_error, "Waiting for secured network", null)
            }
        }
        return notificationBuilder.build()
    }

    fun cancelNotification(notificationId: Int) {
        try {
            notificationManager.cancel(notificationId)
            notificationManager.cancelAll()
        } catch (e: Exception) {
            logger.debug(e.toString())
        }
    }

    private fun updateNotification(iconId: Int?, textUpdate: String?, trafficStats: String?) {
        if (trafficStats?.isEmpty() == false) {
            notificationBuilder.setContentText(trafficStats)
        } else {
            notificationBuilder.setContentText(null)
        }
        notificationBuilder.setWhen(lastUpdateTime)
        notificationBuilder.setShowWhen(true)
        notificationBuilder.setUsesChronometer(true)
        iconId?.let { notificationBuilder.setSmallIcon(it) }
        notificationBuilder.setContentTitle(textUpdate)
        notificationBuilder.setOngoing(true)
    }

    private val notificationTitle: String?
        get() {
            return try {
                Util.getLastSelectedLocation(Windscribe.appContext)?.let {
                    String.format("%s - %s", it.nodeName, it.nickName)
                }
            } catch (e: Exception) {
                null
            }
        }

    private fun setContentIntent() {
        // Start disconnect service with pending intent to disconnect.
        val serviceIntent = Intent(Windscribe.appContext, DisconnectService::class.java)
        val disconnectPendingIntent = PendingIntent
                .getService(
                        Windscribe.appContext, 200, serviceIntent,
                        if (VERSION.SDK_INT >= VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
                )
        val action = Action.Builder(
                mipmap.connected, "Disconnect",
                disconnectPendingIntent
        ).build()
        notificationBuilder.addAction(action)
        // Launch App on Notification click.
        val contentIntent = Windscribe.appContext.applicationInterface.splashIntent
        contentIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        val pendingIntent = PendingIntent
                .getActivity(
                        Windscribe.appContext, 0, contentIntent,
                        if (VERSION.SDK_INT >= VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
                )
        notificationBuilder.setContentIntent(pendingIntent)
    }

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private fun setupNotificationLollipop() {
        notificationBuilder.setSmallIcon(mipmap.connecting)
                .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
        setContentIntent()
    }

    @TargetApi(VERSION_CODES.N)
    private fun setupNotificationNougat() {
        notificationBuilder.setSmallIcon(mipmap.connecting)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
        setContentIntent()
    }

    @RequiresApi(api = 26)
    private fun setupNotificationOreo() {
        val channel = NotificationChannel(
                NotificationConstants.NOTIFICATION_CHANNEL_ID,
                "WindScribe", NotificationManager.IMPORTANCE_LOW
        )
        channel.description =
                "Provides information about the VPN connection state and serves as permanent notification to keep the VPN service running in the background."
        channel.enableLights(true)
        notificationManager.createNotificationChannel(channel)

        notificationBuilder.setSmallIcon(mipmap.connecting)
                .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
                .setChannelId(NotificationConstants.NOTIFICATION_CHANNEL_ID)
                .setOnlyAlertOnce(true)
        setContentIntent()
    }

    init {
        createDefaultNotification()
        scope.launch {
            vpnConnectionStateManager.state.collectLatest {
                if (it.status === Disconnected) {
                    trafficStats = null
                    lastUpdateTime = System.currentTimeMillis()
                    notificationManager.cancel(NotificationConstants.SERVICE_NOTIFICATION_ID)
                } else {
                    notificationManager.notify(
                            NotificationConstants.SERVICE_NOTIFICATION_ID,
                            buildNotification(it.status)
                    )
                }
            }
        }
        scope.launch {
            trafficCounter.trafficStats.collectLatest { traffic ->
                trafficStats = traffic.text
                val status = vpnConnectionStateManager.state.value.status
                if (status == Connected && interactor.preferenceHelper.globalUserConnectionPreference) {
                    notificationManager.notify(
                        NotificationConstants.SERVICE_NOTIFICATION_ID,
                        buildNotification(status)
                    )
                }
            }
        }
    }

    private fun createDefaultNotification() {
        when {
            VERSION.SDK_INT >= VERSION_CODES.O -> {
                setupNotificationOreo()
            }
            VERSION.SDK_INT >= VERSION_CODES.N -> {
                setupNotificationNougat()
            }
            else -> {
                setupNotificationLollipop()
            }
        }
    }
}
