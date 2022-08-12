/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.state

import android.os.Build
import android.os.Build.VERSION
import androidx.lifecycle.Lifecycle.Event.*
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.windscribe.vpn.R.string
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.Windscribe.Companion.applicationScope
import com.windscribe.vpn.api.ApiCallType
import com.windscribe.vpn.api.DomainFailOverManager
import com.windscribe.vpn.api.response.PushNotificationAction
import com.windscribe.vpn.commonutils.WindUtilities
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
    private val domainFailOverManager: DomainFailOverManager
) :
    LifecycleObserver {

    private val logger = LoggerFactory.getLogger("vpn_")
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
        val logFile = appContext.resources.getString(
                string.log_file_header,
                VERSION.SDK_INT, Build.BRAND, Build.DEVICE, Build.MODEL, Build.MANUFACTURER,
                VERSION.RELEASE, WindUtilities.getVersionCode()
        )
        logger.info(logFile)
    }

    @OnLifecycleEvent(ON_PAUSE)
    fun pausingApp() {
        isInForeground = false
        logger.debug("*****App going to background.*****")
        workManager.onAppMovedToBackground()
    }

    @OnLifecycleEvent(ON_RESUME)
    fun resumingApp() {
        if(appContext.vpnConnectionStateManager.isVPNConnected().not()){
            logger.debug("Resetting server list country code.")
            overriddenCountryCode = null
        }
        domainFailOverManager.reset(ApiCallType.Other)
        networkInfoManager.reload()
        if (startingFresh.getAndSet(false)) {
            isInForeground = false
            logger.debug("App on Start")
            workManager.onAppStart()
        } else {
            isInForeground = true
            logger.debug("App moved to Foreground.")
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
