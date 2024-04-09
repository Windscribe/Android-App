/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.state

import androidx.lifecycle.Lifecycle.Event.*
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.Windscribe.Companion.applicationScope
import com.windscribe.vpn.api.ApiCallType
import com.windscribe.vpn.api.DomainFailOverManager
import com.windscribe.vpn.api.response.PushNotificationAction
import com.windscribe.vpn.backend.ProxyDNSManager
import com.windscribe.vpn.workers.WindScribeWorkManager
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
class AppLifeCycleObserver @Inject constructor(
    private val workManager: WindScribeWorkManager,
    private val networkInfoManager: NetworkInfoManager,
    private val domainFailOverManager: DomainFailOverManager,
    private val vpnConnectionStateManager: VPNConnectionStateManager,
    private val proxyDNSManager: ProxyDNSManager
) :
    LifecycleObserver {

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

    @OnLifecycleEvent(ON_CREATE)
    fun createApp() {
        startingFresh.set(true)
    }

    @OnLifecycleEvent(ON_PAUSE)
    fun pausingApp() {
        isInForeground = false
        workManager.onAppMovedToBackground()
        if (!vpnConnectionStateManager.isVPNActive()) {
            applicationScope.launch {
               proxyDNSManager.stopControlD()
            }
        }
        logger.debug("----------App going to background.--------\n")
    }

    @OnLifecycleEvent(ON_RESUME)
    fun resumingApp() {
        if (startingFresh.get().not()) {
            logger.debug("----------------App moved to Foreground.------------\n")
        }
        if (appContext.vpnConnectionStateManager.isVPNConnected().not()) {
            logger.debug("Resetting server list country code.")
            overriddenCountryCode = null
        }
        domainFailOverManager.reset(ApiCallType.Other)
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
