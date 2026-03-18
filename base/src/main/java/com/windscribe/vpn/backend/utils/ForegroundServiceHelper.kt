/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.windscribe.common.startSafeForeground
import com.windscribe.vpn.R
import com.windscribe.vpn.constants.NotificationConstants

/**
 * Provides immediate foreground promotion for services BEFORE Dagger injection.
 *
 * Android enforces a strict timeout (~5s on Android 12+, ~10s on older) between
 * startForegroundService() and startForeground(). Dagger injection can exceed this
 * on slow devices or under memory pressure, causing:
 *   - ForegroundServiceDidNotStartInTimeException (Android 12+)
 *   - "Context.startForegroundService() did not then call Service.startForeground()" (older)
 *
 * Call [Service.startForegroundImmediately] as the FIRST line of onCreate(), before
 * any DI or heavy work. The notification will be replaced by the proper one after DI.
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
                val channel = NotificationChannel(
                    NotificationConstants.NOTIFICATION_CHANNEL_ID,
                    "WindScribe",
                    NotificationManager.IMPORTANCE_LOW
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
    fun buildMinimalNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, NotificationConstants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.connecting)
            .setContentTitle(context.getString(R.string.app_name))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}

/**
 * Immediately promotes this service to foreground with a minimal placeholder notification.
 * Call this as the VERY FIRST line in Service.onCreate(), before Dagger injection.
 *
 * After DI completes, call [startForegroundSafely] to replace with the full notification.
 *
 * @param notificationId The notification ID to use (must match the one used in startForegroundSafely)
 */
fun Service.startForegroundImmediately(notificationId: Int) {
    ForegroundServiceHelper.ensureNotificationChannel(applicationContext)
    val notification = ForegroundServiceHelper.buildMinimalNotification(applicationContext)
    startSafeForeground(notificationId, notification)
}
