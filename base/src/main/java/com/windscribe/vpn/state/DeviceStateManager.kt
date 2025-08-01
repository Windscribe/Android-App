/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.state

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SCREEN_OFF
import android.content.Intent.ACTION_SCREEN_ON
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import com.windscribe.vpn.services.DeviceStateService.Companion.enqueueWork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceStateManager @Inject constructor(val scope: CoroutineScope) : BroadcastReceiver() {

    private val listeners = ConcurrentLinkedQueue<DeviceStateListener>()
    private var _deviceInteractiveEvents = MutableStateFlow(false)
    val isDeviceInteractive: StateFlow<Boolean> = _deviceInteractiveEvents
    private val logger = LoggerFactory.getLogger("state")

    fun addListener(deviceStateListener: DeviceStateListener) {
        listeners.add(deviceStateListener)
    }

    fun init(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.registerReceiver(this, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION), Context.RECEIVER_NOT_EXPORTED)
            context.registerReceiver(this, IntentFilter(ACTION_SCREEN_OFF), Context.RECEIVER_NOT_EXPORTED)
            context.registerReceiver(this, IntentFilter(ACTION_SCREEN_ON), Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(this, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
            context.registerReceiver(this, IntentFilter(ACTION_SCREEN_OFF))
            context.registerReceiver(this, IntentFilter(ACTION_SCREEN_ON))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (!isInitialStickyBroadcast) {
            when (intent.action) {
                ConnectivityManager.CONNECTIVITY_ACTION -> {
                    enqueueWork(context)
                    for (deviceStateListener in listeners) {
                        deviceStateListener.onNetworkStateChanged()
                    }
                }
                ACTION_SCREEN_OFF -> {
                    logger.debug("Device going to Idle state.")
                    scope.launch { _deviceInteractiveEvents.emit(false) }
                }
                ACTION_SCREEN_ON -> {
                    logger.debug("Device coming out of Idle state.")
                    scope.launch { _deviceInteractiveEvents.emit(true) }
                }
            }
        }
    }

    fun removeListener(deviceStateListener: DeviceStateListener) {
        listeners.remove(deviceStateListener)
    }

    interface DeviceStateListener {
        fun onNetworkStateChanged() {}
    }
}
