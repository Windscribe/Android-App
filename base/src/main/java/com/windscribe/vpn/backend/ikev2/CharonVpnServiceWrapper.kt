/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.ikev2

import android.app.Notification
import android.content.Intent
import android.util.Log
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.VPNState.Status.Connecting
import com.windscribe.vpn.backend.utils.WindNotificationBuilder
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.backend.utils.startForegroundImmediately
import com.windscribe.vpn.backend.utils.startForegroundSafely
import com.windscribe.vpn.constants.NotificationConstants
import com.windscribe.vpn.state.ShortcutStateManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.strongswan.android.data.VpnProfile
import org.strongswan.android.logic.CharonVpnService
import javax.inject.Inject

@AndroidEntryPoint
class CharonVpnServiceWrapper : CharonVpnService() {
    companion object {
        private const val STATE_CHILD_SA_UP = 1
        private const val STATE_CHILD_SA_DOWN = 2
        private const val STATE_AUTH_ERROR = 3
        private const val STATE_PEER_AUTH_ERROR = 4
        private const val STATE_LOOKUP_ERROR = 5
        private const val STATE_UNREACHABLE_ERROR = 6
        private const val STATE_CERTIFICATE_UNAVAILABLE = 7
        private const val STATE_GENERIC_ERROR = 8
    }

    @Inject
    lateinit var windNotificationBuilder: WindNotificationBuilder

    @Inject
    lateinit var vpnController: WindVpnController

    @Inject
    lateinit var iKev2VpnBackend: IKev2VpnBackend

    @Inject
    lateinit var shortcutStateManager: ShortcutStateManager

    @Inject
    lateinit var scope: CoroutineScope

    private var logger = LoggerFactory.getLogger("vpn")

    override fun onCreate() {
        logger.debug("CharonVpnServiceWrapper onCreate()")
        startForegroundImmediately(NotificationConstants.SERVICE_NOTIFICATION_ID)
        super.onCreate()
        startForegroundSafely(
            windNotificationBuilder,
            NotificationConstants.SERVICE_NOTIFICATION_ID,
            Connecting,
        )
        Log.i("GoLog", "Setting service")
        iKev2VpnBackend.serviceCreated(this)
    }

    override fun getMainActivityClass(): Class<*> =
        appContext.applicationInterface.homeIntent.component!!
            .javaClass

    override fun buildNotification(publicVersion: Boolean): Notification = windNotificationBuilder.buildNotification(Connecting)

    override fun onDestroy() {
        logger.debug("CharonVpnServiceWrapper onDestroy()")
        windNotificationBuilder.cancelNotification(NotificationConstants.SERVICE_NOTIFICATION_ID)
        iKev2VpnBackend.serviceDestroyed()
        super.onDestroy()
    }

    override fun getNotificationID(): Int = NotificationConstants.SERVICE_NOTIFICATION_ID

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
        return when (intent.action) {
            DISCONNECT_ACTION -> {
                logger.debug("Disconnect action received, setting next profile to null")
                setNextProfile(null)
                START_NOT_STICKY
            }

            else -> {
                startForegroundSafely(
                    windNotificationBuilder,
                    NotificationConstants.SERVICE_NOTIFICATION_ID,
                    Connecting,
                )
                Util.getProfile<VpnProfile>()?.let {
                    setNextProfile(it)
                    START_NOT_STICKY
                } ?: kotlin.run {
                    START_NOT_STICKY
                }
            }
        }
    }

    fun connect() {
        logger.debug("CharonVpnServiceWrapper connect() called")
        Util.getProfile<VpnProfile>()?.let { profile ->
            logger.info("Setting next profile: ${profile.name}")
            setNextProfile(profile)
        } ?: run {
            logger.error("Failed to get VPN profile for connection")
            iKev2VpnBackend.getTunnel().onStateChange(IKev2Tunnel.State.DOWN)
        }
    }

    override fun stateChanged() {
        logger.debug("stateChanged() callback from CharonVpnService")
    }

    override fun updateStatus(status: Int) {
        logger.debug("updateStatus($status) called")
        super.updateStatus(status)

        when (status) {
            STATE_CHILD_SA_DOWN -> {
                logger.info("IKEv2 tunnel: CHILD_SA_DOWN -> CONNECTING")
                iKev2VpnBackend.getTunnel().onStateChange(IKev2Tunnel.State.CONNECTING)
            }

            STATE_CHILD_SA_UP -> {
                logger.info("IKEv2 tunnel: CHILD_SA_UP -> CONNECTED")
                iKev2VpnBackend.getTunnel().onStateChange(IKev2Tunnel.State.CONNECTED)
            }

            STATE_AUTH_ERROR -> {
                logger.error("IKEv2 authentication failed")
                scope.launch {
                    iKev2VpnBackend.disconnect(
                        VPNState.Error(
                            error = VPNState.ErrorType.AuthenticationError,
                            message = "Authentication failed.",
                        ),
                    )
                }
            }

            STATE_PEER_AUTH_ERROR -> {
                logger.error("IKEv2 peer authentication failed")
                scope.launch {
                    iKev2VpnBackend.disconnect(
                        VPNState.Error(
                            error = VPNState.ErrorType.AuthenticationError,
                            message = "Peer authentication failed.",
                        ),
                    )
                }
            }

            STATE_LOOKUP_ERROR -> {
                logger.error("IKEv2 DNS lookup failed")
                scope.launch {
                    iKev2VpnBackend.disconnect(
                        VPNState.Error(
                            error = VPNState.ErrorType.GenericError,
                            message = "Failed to resolve server hostname.",
                        ),
                    )
                }
            }

            STATE_UNREACHABLE_ERROR -> {
                logger.error("IKEv2 server unreachable")
                scope.launch {
                    iKev2VpnBackend.disconnect(
                        VPNState.Error(
                            error = VPNState.ErrorType.TimeoutError,
                            message = "Server unreachable.",
                        ),
                    )
                }
            }

            STATE_CERTIFICATE_UNAVAILABLE -> {
                logger.error("IKEv2 certificate unavailable")
                scope.launch {
                    iKev2VpnBackend.disconnect(
                        VPNState.Error(
                            error = VPNState.ErrorType.AuthenticationError,
                            message = "Certificate unavailable.",
                        ),
                    )
                }
            }

            STATE_GENERIC_ERROR -> {
                logger.error("IKEv2 generic error")
                scope.launch {
                    iKev2VpnBackend.disconnect(
                        VPNState.Error(
                            error = VPNState.ErrorType.GenericError,
                            message = "Connection error.",
                        ),
                    )
                }
            }

            else -> {
                logger.warn("Unknown IKEv2 status: $status")
            }
        }
    }

    fun close() {
        logger.debug("CharonVpnServiceWrapper close() called")
        iKev2VpnBackend.getTunnel().onStateChange(IKev2Tunnel.State.DOWN)
        stopSelf()
    }
}
