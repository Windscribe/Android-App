/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.bootreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.windscribe.vpn.Windscribe.Companion.appContext
import org.slf4j.LoggerFactory

class WindscribeBootReceiver : BroadcastReceiver() {
    private val logger = LoggerFactory.getLogger("boot_receiver")

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            logger.info("Device boot completed, checking auto-start preference")
            appContext.workManager.connectOnBoot()
        } else if (Intent.ACTION_MY_PACKAGE_REPLACED == intent.action) {
            logger.info("App updated, checking if VPN reconnection needed")
            appContext.preference.migrationRequired = true
            appContext.workManager.updateSession()
            // Reconnect VPN after app upgrade if user was previously connected
            if (appContext.preference.globalUserConnectionPreference) {
                logger.info("User was connected before update, reconnecting VPN")
                appContext.workManager.connectOnBoot()
            } else {
                logger.info("User was not connected before update, skipping reconnection")
            }
        }
    }
}
