/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.state

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.Windscribe.Companion.applicationScope
import com.windscribe.vpn.api.response.PushNotificationAction
import com.windscribe.vpn.backend.ProxyDNSManager
import com.windscribe.vpn.workers.WindScribeWorkManager
import com.windscribe.vpn.wsnet.WSNetWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
Tracks App life cycle
 * */
@Singleton
class AppLifeCycleObserver
    @Inject
    constructor(
        private val workManager: WindScribeWorkManager,
        private val networkInfoManager: NetworkInfoManager,
        private val vpnConnectionStateManager: VPNConnectionStateManager,
        private val proxyDNSManager: ProxyDNSManager,
        private val wsNetWrapper: WSNetWrapper,
        private val deviceStateManager: DeviceStateManager,
    ) : DefaultLifecycleObserver {
        private val logger = LoggerFactory.getLogger("app")
        private val startingFresh = AtomicBoolean(false)
        var overriddenCountryCode: String? = null
        private var _appActivationState = MutableStateFlow(false)
        val appActivationState: StateFlow<Boolean> = _appActivationState
        private var pushNotification: PushNotificationAction? = null
        var pushNotificationAction: PushNotificationAction?
            get() = pushNotification
            set(value) {
                pushNotification = value
                appContext.workManager.updateNotifications()
            }

        override fun onCreate(owner: LifecycleOwner) {
            startingFresh.set(true)
        }

        override fun onPause(owner: LifecycleOwner) {
            isInForeground = false
            workManager.onAppMovedToBackground()
            if (!vpnConnectionStateManager.isVPNActive()) {
                applicationScope.launch {
                    proxyDNSManager.stopControlD()
                }
            }
            // Only save WSNet settings if it's already initialized to avoid forcing initialization
            wsNetWrapper.withWSNet { wsNet ->
                appContext.preference.wsNetSettings = wsNet.currentPersistentSettings()
            }
            // Clear whitelist when app goes to background
            deviceStateManager.setWhitelistedNetwork(null)
            logger.info("----------App going to background.--------\n")
        }

        override fun onResume(owner: LifecycleOwner) {
            if (startingFresh.get().not()) {
                logger.info("----------------App moved to Foreground.------------\n")
            }
            if (appContext.vpnConnectionStateManager.isVPNConnected().not()) {
                overriddenCountryCode = null
            }
            networkInfoManager.reload()
            if (startingFresh.getAndSet(false)) {
                isInForeground = false
                workManager.onAppStart()
            } else {
                isInForeground = true
                workManager.onAppMovedToForeground()
            }
            applicationScope.launch {
                _appActivationState.emit(_appActivationState.value.not())
            }
        }

        companion object {
            var isInForeground = false
        }
    }
