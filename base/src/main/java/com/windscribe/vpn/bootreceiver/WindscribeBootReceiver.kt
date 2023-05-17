/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.bootreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.bootreceiver.BootSessionService.Companion.enqueueWork

class WindscribeBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            enqueueWork(context, intent)
        } else if (Intent.ACTION_MY_PACKAGE_REPLACED == intent.action) {
            appContext.preference.migrationRequired = true
            appContext.workManager.updateSession()
        }
    }
}
