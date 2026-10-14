/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.constants

object VpnPreferenceConstants {
    const val STUNNEL_CONFIG_FILE = "stconfig"
    const val STUNNEL_PID = "pid"

    /**
     * When applog.txt rolls. [com.windscribe.vpn.commonutils.SizeBasedTriggerForLog] reads this, not
     * the maxFileSize in logback.xml.
     *
     * A report carries this plus the rolled backup, so the window is twice this value. Raising it
     * grows the upload by twice as much again, base64-encoded whole in memory.
     */
    const val DEBUG_FILE_SIZE_LOWER = "1024 KB"
}
