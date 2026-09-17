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
import com.windscribe.common.startSafeForeground
import com.windscribe.vpn.R.mipmap
import com.windscribe.vpn.R.string
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.TrafficCounter
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.VPNState.Status
import com.windscribe.vpn.backend.VPNState.Status.Connected
import com.windscribe.vpn.backend.VPNState.Status.Connecting
import com.windscribe.vpn.backend.VPNState.Status.Disconnected
import com.windscribe.vpn.backend.VPNState.Status.ProtocolSwitch
import com.windscribe.vpn.backend.VPNState.Status.UnsecuredNetwork
import com.windscribe.vpn.backend.tunnel.VpnServiceGate
import com.windscribe.vpn.backend.tunnel.WindscribeVpnService
import com.windscribe.vpn.constants.NotificationConstants
import com.windscribe.vpn.model.User
import com.windscribe.vpn.repository.ServerListRepository
import com.windscribe.vpn.state.ConnectionPolicy.ServiceMode
import com.windscribe.vpn.state.DeviceStateManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.state.WatchReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WindNotificationBuilder
    @Inject
    constructor(
        private val notificationManager: NotificationManager,
        private val notificationBuilder: Builder,
        private val vpnConnectionStateManager: VPNConnectionStateManager,
        private val trafficCounter: TrafficCounter,
        val scope: CoroutineScope,
        val serverListRepository: ServerListRepository,
        private val preferencesHelper: PreferencesHelper,
        private val deviceStateManager: DeviceStateManager,
        private val serviceGate: VpnServiceGate,
    ) {
        private companion object {
            const val CONTENT_INTENT_REQUEST_CODE = 0

            // One request code per action, or PendingIntents differing only by action collide.
            const val DISCONNECT_ACTION_REQUEST_CODE = 200
            const val PAUSE_ACTION_REQUEST_CODE = 201
            const val RESUME_ACTION_REQUEST_CODE = 202
            const val CONNECT_ACTION_REQUEST_CODE = 203
            const val TURN_OFF_ACTION_REQUEST_CODE = 204
            const val DISMISS_REQUEST_CODE = 205

            const val CHANNEL_NAME = "WindScribe"
            const val CHANNEL_DESCRIPTION =
                "Provides information about the VPN connection state and serves as permanent notification to keep the VPN service running in the background."
        }

        private var lastUpdateTime = System.currentTimeMillis()
        private var trafficStats: String? = null
        private val logger = LoggerFactory.getLogger("vpn")

        /**
         * Builds the notification for [status], with the actions that state allows.
         *
         * Synchronized because [notificationBuilder] is shared and rebuilding actions is
         * `clearActions()` then `addAction()`. Contended: publishing an idle mode calls this directly
         * and also wakes the connection-state collector, which calls it from another coroutine.
         */
        @Synchronized
        fun buildNotification(status: Status): Notification {
            val mode = vpnConnectionStateManager.serviceMode.value
            val location = notificationTitle
            val icon = getDefaultIcon(status)
            val message = getStatusMessage(status, mode, location)
            val stats = if (status == Connected) trafficStats else null

            updateNotification(icon, message, stats)
            setActionsFor(status, mode)
            return notificationBuilder.build()
        }

        /**
         * True while failover is driving: a connect is still coming and the interface is held.
         * Read through `appContext` to stay out of the injection cycle.
         */
        private fun isHandover(): Boolean = runCatching { appContext.autoConnectionManager.isFailoverActive }.getOrDefault(false)

        /** The idle mode wins when there is one: paused and watching both report [Disconnected]. */
        private fun getStatusMessage(
            status: Status,
            mode: ServiceMode?,
            location: String?,
        ): String =
            when (mode) {
                is ServiceMode.Paused -> getString(string.notification_paused)

                is ServiceMode.NoNetwork -> getString(string.notification_no_network)

                // Names no culprit, because none is known yet.
                is ServiceMode.AwaitingSession -> getString(string.notification_awaiting_session)

                is ServiceMode.AuthFailed -> getString(string.notification_config_auth_failed)

                // The block covers the Play Store too, so upgrading has to follow turning off.
                is ServiceMode.AccountBlocked ->
                    if (mode.status == User.AccountStatus.Banned) {
                        getString(string.notification_account_banned)
                    } else {
                        getString(string.notification_account_expired)
                    }

                // Named like the connected copy: which location failed is what the user wants.
                is ServiceMode.Failed ->
                    if (mode.blocking) {
                        // "Couldn't connect" describes an attempt; this also blocks every app, and
                        // the user has to know that is why nothing works.
                        getString(string.notification_blocked)
                    } else {
                        location?.let { getString(string.notification_failed_to, it) }
                            ?: getString(string.notification_failed)
                    }

                is ServiceMode.Watching ->
                    when (mode.reason) {
                        WatchReason.StoodDownByUser ->
                            deviceStateManager.getCurrentNetworkName()?.let {
                                getString(string.notification_stood_down_on, it)
                            } ?: getString(string.notification_stood_down)

                        // The next network needing protection gets connected, so say that rather
                        // than implying we are waiting on the user.
                        WatchReason.NetworkOptedOut ->
                            deviceStateManager.getCurrentNetworkName()?.let {
                                getString(string.notification_opted_out_on, it)
                            } ?: getString(string.notification_opted_out)

                        WatchReason.NetworkNotSecured -> getString(string.notification_waiting_for_secured)
                    }

                else ->
                    when (status) {
                        Connected -> getString(string.connected_to, location)
                        Connecting -> getString(string.connecting_to, location)
                        ProtocolSwitch -> getString(string.notification_protocol_switch)
                        UnsecuredNetwork -> getString(string.notification_waiting_for_secured)
                        // Mid-handover the interface is still held, so "DISCONNECTED" would be the
                        // opposite of true. Only a genuine stop says that.
                        Disconnected ->
                            if (isHandover()) {
                                location?.let { getString(string.notification_reconnecting_to, it) }
                                    ?: getString(string.notification_reconnecting)
                            } else {
                                getString(string.disconnected)
                            }
                        else -> getString(string.connected_lower_case)
                    }
            }

        /**
         * Pause is absent under the system killswitch, where dropping the interface blackholes the
         * device rather than letting traffic through.
         */
        private fun setActionsFor(
            status: Status,
            mode: ServiceMode?,
        ) {
            notificationBuilder.clearActions()
            val turnOff = action(string.notification_action_turn_off, WindscribeVpnService.ACTION_TURN_OFF, TURN_OFF_ACTION_REQUEST_CODE)
            val disconnect = action(string.state_disconnected, WindscribeVpnService.ACTION_DISCONNECT, DISCONNECT_ACTION_REQUEST_CODE)
            val actions =
                when {
                    // Resume and Connect are the same act with different labels.
                    mode is ServiceMode.Paused ->
                        listOf(
                            action(string.notification_action_resume, WindscribeVpnService.ACTION_RESUME, RESUME_ACTION_REQUEST_CODE),
                            turnOff,
                        )

                    // Same offer as Watching: nothing failed here that a retry would fix.
                    mode is ServiceMode.NoNetwork ->
                        listOf(
                            action(string.notification_action_connect, WindscribeVpnService.ACTION_RESUME, CONNECT_ACTION_REQUEST_CODE),
                            turnOff,
                        )

                    mode is ServiceMode.Watching ->
                        listOf(
                            action(string.notification_action_connect, WindscribeVpnService.ACTION_RESUME, CONNECT_ACTION_REQUEST_CODE),
                            turnOff,
                        )

                    // "Retry" rather than "Connect": the difference matters when the last attempt
                    // failed. Note this is a single attempt, not another failover walk — that needs
                    // a foreground Activity. See docs/architecture/VPN_BACKEND.md §7.
                    mode is ServiceMode.Failed || mode is ServiceMode.AuthFailed ->
                        listOf(
                            action(string.notification_action_retry, WindscribeVpnService.ACTION_RESUME, CONNECT_ACTION_REQUEST_CODE),
                            turnOff,
                        )

                    // Neither can be connected out of: the verdict is pending, or the account is
                    // not Okay and `ShortcutStateManager` refuses to load its session.
                    mode is ServiceMode.AwaitingSession || mode is ServiceMode.AccountBlocked -> listOf(turnOff)

                    status == Connected ->
                        buildList {
                            if (!isSystemKillswitchOn()) {
                                add(action(string.notification_action_pause, WindscribeVpnService.ACTION_PAUSE, PAUSE_ACTION_REQUEST_CODE))
                            }
                            add(disconnect)
                        }

                    status == Connecting ||
                        status == ProtocolSwitch ||
                        status == Status.Disconnecting -> listOf(disconnect)

                    // Idle with no mode: "Disconnect" would be meaningless.
                    else ->
                        listOf(
                            action(string.notification_action_connect, WindscribeVpnService.ACTION_RESUME, CONNECT_ACTION_REQUEST_CODE),
                            turnOff,
                        )
                }
            actions.forEach { notificationBuilder.addAction(it) }
        }

        private fun action(
            labelRes: Int,
            action: String,
            requestCode: Int,
        ): Action =
            Action
                .Builder(mipmap.connected, getString(labelRes), servicePendingIntent(action, requestCode))
                .build()

        /** `VpnService.isLockdownEnabled()` can only be read from the service, so go through it. */
        private fun isSystemKillswitchOn(): Boolean = serviceGate.current?.isSystemLockdownEnabled() == true

        private fun getString(
            resId: Int,
            vararg formatArgs: Any?,
        ): String = Windscribe.appContext.localized.getString(resId, *formatArgs)

        /** Cancels one notification. Not `cancelAll()`, which would take every other one with it. */
        fun cancelNotification(notificationId: Int) {
            try {
                notificationManager.cancel(notificationId)
            } catch (e: Exception) {
                logger.debug(e.toString())
            }
        }

        private fun updateNotification(
            iconId: Int,
            textUpdate: String?,
            trafficStats: String?,
        ) {
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
            get() =
                try {
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

        /** The icon changed, so the content intent points at a stale activity. */
        @Synchronized
        private fun refreshContentIntent() {
            setContentIntent()
        }

        private fun setContentIntent() {
            notificationBuilder.setContentIntent(createContentPendingIntent(Windscribe.appContext))
        }

        /** Targets the single VPN service directly; it handles each of these actions itself. */
        private fun servicePendingIntent(
            action: String,
            requestCode: Int,
        ): PendingIntent {
            val serviceIntent =
                Intent(Windscribe.appContext, WindscribeVpnService::class.java)
                    .setAction(action)
            return PendingIntent.getService(
                Windscribe.appContext,
                requestCode,
                serviceIntent,
                PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun createContentPendingIntent(context: Windscribe): PendingIntent {
            val contentIntent =
                context.applicationInterface.splashIntent.apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            return PendingIntent.getActivity(
                context,
                CONTENT_INTENT_REQUEST_CODE,
                contentIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        private fun setupNotificationPreO() {
            notificationBuilder.apply {
                setSmallIcon(getDefaultIcon(Connecting))
                setOnlyAlertOnce(true)
                setOngoing(true)
                setDeleteIntent(dismissPendingIntent())
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
                setDeleteIntent(dismissPendingIntent())
            }
            setContentIntent()
        }

        /** Lets the service put the notification back while traffic is blocked. */
        private fun dismissPendingIntent(): PendingIntent =
            servicePendingIntent(WindscribeVpnService.ACTION_NOTIFICATION_DISMISSED, DISMISS_REQUEST_CODE)

        @RequiresApi(VERSION_CODES.O)
        private fun createNotificationChannel() {
            val channel =
                NotificationChannel(
                    NotificationConstants.NOTIFICATION_CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW,
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
                    if (state.status == Disconnected) {
                        trafficStats = null
                        lastUpdateTime = System.currentTimeMillis()
                    }
                    // The notification's lifetime follows the service, not the connection: a
                    // disconnect updates it to idle and leaves it posted, and the service cancels it
                    // in onDestroy. Cancelling on Disconnected instead would strip the foreground
                    // notification from a service that outlives the connection.
                    //
                    // Which means nothing may be posted once the service is going. A session ending
                    // in ServiceMode.Off calls stopSelf() and then publishes its terminal
                    // Disconnected, so this wakes with the service already leaving; posting then
                    // races onDestroy's cancel and leaves a notification nothing will remove.
                    // `isStopping` is set before that state is published, so it cannot be missed.
                    if (serviceGate.current == null || serviceGate.isStopping) {
                        return@collectLatest
                    }
                    notificationManager.notify(
                        NotificationConstants.SERVICE_NOTIFICATION_ID,
                        buildNotification(state.status),
                    )
                }
            }
        }

        private fun observeTrafficStats() {
            scope.launch {
                trafficCounter.trafficStats.collectLatest { traffic ->
                    trafficStats = traffic.text
                    val status = vpnConnectionStateManager.state.value.status
                    // Connected is the whole condition: an auto-connected session sets no user
                    // preference, and gating on one left it with a notification that never ticked.
                    if (status == Connected) {
                        notificationManager.notify(
                            NotificationConstants.SERVICE_NOTIFICATION_ID,
                            buildNotification(status),
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
                        // Same lock as buildNotification: this mutates the shared builder too.
                        refreshContentIntent()
                        notificationManager.notify(
                            NotificationConstants.SERVICE_NOTIFICATION_ID,
                            buildNotification(status),
                        )
                    }
                }
            }
        }

        private fun isNotificationActive(): Boolean =
            try {
                notificationManager.activeNotifications.any {
                    it.id == NotificationConstants.SERVICE_NOTIFICATION_ID
                }
            } catch (e: Exception) {
                logger.debug("Failed to check active notifications", e)
                false
            }

        private fun getDefaultIcon(status: Status): Int =
            when (status) {
                Connecting -> mipmap.connecting
                Connected -> mipmap.connected
                ProtocolSwitch, UnsecuredNetwork -> mipmap.connection_error
                else -> mipmap.disconnected
            }

        private fun createDefaultNotification() {
            if (VERSION.SDK_INT >= VERSION_CODES.O) {
                setupNotificationO()
            } else {
                setupNotificationPreO()
            }
        }
    }

/**
 * Calls `startForeground` right after injection, before Android's ~5s timeout.
 *
 * Call from `Service.onCreate()` immediately after `super.onCreate()`, where Hilt injects.
 *
 * @param notificationBuilder must already be injected.
 * @param clearActions clears the content intent and actions from the notification.
 * @param serviceType defaults to `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`.
 */
fun android.app.Service.startForegroundSafely(
    notificationBuilder: WindNotificationBuilder,
    notificationId: Int,
    status: Status,
    clearActions: Boolean = false,
    serviceType: Int = android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
) {
    val notification = notificationBuilder.buildNotification(status)
    if (clearActions) {
        notification.contentIntent = null
        notification.actions = null
    }
    startSafeForeground(notificationId, notification, serviceType)
}
