/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.bootreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.windscribe.vpn.Windscribe.Companion.appContext

class WindscribeBootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            appContext.workManager.connectOnBoot()
        } else if (Intent.ACTION_MY_PACKAGE_REPLACED == intent.action) {
            appContext.preference.migrationRequired = true
            appContext.workManager.updateSession()
        }
    }
}
