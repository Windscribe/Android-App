/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.bootreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.workers.worker.BootWorker
import org.slf4j.LoggerFactory

class WindscribeBootReceiver : BroadcastReceiver() {
    private val logger = LoggerFactory.getLogger("boot_receiver")

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            logger.info("Device boot completed, checking auto-start preference")
            appContext.workManager.connectOnBoot(BootWorker.REASON_BOOT)
        } else if (Intent.ACTION_MY_PACKAGE_REPLACED == intent.action) {
            logger.info("App updated, checking if VPN reconnection needed")
            appContext.preference.migrationRequired = true
            appContext.workManager.updateSession()
            // Whether to reconnect is ConnectionPolicy's call, same as for a boot. Deciding it here
            // as well meant two places answering one question, and this one could not see the
            // difference between "the user was connected" and "the user asked to start on boot".
            appContext.workManager.connectOnBoot(BootWorker.REASON_APP_UPDATED)
        }
    }
}
