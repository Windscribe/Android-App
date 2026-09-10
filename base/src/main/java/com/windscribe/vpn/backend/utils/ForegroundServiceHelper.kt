/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import com.windscribe.common.startSafeForeground
import com.windscribe.vpn.R
import com.windscribe.vpn.constants.NotificationConstants

/**
 * Promotes a service to foreground before Dagger injection.
 *
 * Android allows ~5s between `startForegroundService()` and `startForeground()`, and injection can
 * exceed that on a slow device, throwing `ForegroundServiceDidNotStartInTimeException`.
 *
 * Call [Service.startForegroundImmediately] as the first line of `onCreate()`, then
 * [startForegroundSafely] once injection is done.
 */
object ForegroundServiceHelper {
    /**
     * Ensures the notification channel exists. Safe to call multiple times.
     * Must be called from Application.onCreate() so the channel is ready before
     * any service tries to post a foreground notification.
     */
    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(NotificationConstants.NOTIFICATION_CHANNEL_ID) == null) {
                val channel =
                    NotificationChannel(
                        NotificationConstants.NOTIFICATION_CHANNEL_ID,
                        "WindScribe",
                        NotificationManager.IMPORTANCE_LOW,
                    ).apply {
                        description = "VPN connection state and background service notification."
                        enableLights(true)
                    }
                manager.createNotificationChannel(channel)
            }
        }
    }

    /**
     * Builds a minimal notification that requires no DI-injected dependencies.
     * Uses only the application context and static resources.
     */
    fun buildMinimalNotification(context: Context): Notification =
        NotificationCompat
            .Builder(context, NotificationConstants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.connecting)
            .setContentTitle(context.getString(R.string.app_name))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
}

/**
 * Promotes this service to foreground with a placeholder notification. Call as the first line of
 * `onCreate()`, then [startForegroundSafely] once injection is done.
 *
 * @param notificationId must match the one passed to [startForegroundSafely].
 * @param serviceType defaults to `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`.
 */
fun Service.startForegroundImmediately(
    notificationId: Int,
    serviceType: Int = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
) {
    ForegroundServiceHelper.ensureNotificationChannel(applicationContext)
    val notification = ForegroundServiceHelper.buildMinimalNotification(applicationContext)
    startSafeForeground(notificationId, notification, serviceType)
}
