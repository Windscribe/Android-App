/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.commonutils

import androidx.annotation.Keep
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy
import ch.qos.logback.core.util.FileSize
import com.windscribe.vpn.constants.VpnPreferenceConstants
import java.io.File

@Keep
class SizeBasedTriggerForLog<E> : SizeBasedTriggeringPolicy<E>() {

    override fun isTriggeringEvent(activeFile: File, event: E): Boolean {
        return activeFile.length() >= FileSize.valueOf(VpnPreferenceConstants.DEBUG_FILE_SIZE_LOWER).size
    }
}
