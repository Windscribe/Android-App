/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.wireguard

import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import org.slf4j.LoggerFactory

class WireguardContextWrapper(context: Context) : ContextWrapper(context) {

    private val logger = LoggerFactory.getLogger("vpn")

    override fun startService(serviceIntent: Intent?): ComponentName? {
        val ourIntent = Intent(this, WireGuardWrapperService::class.java)
        return try {
            if (Build.VERSION.SDK_INT >= 26) {
                baseContext.startForegroundService(ourIntent)
            } else {
                baseContext.startService(ourIntent)
            }
        } catch (e: Exception) {
            logger.error("Failed to start WireGuard service: ${e.message}")
            null
        }
    }
}
