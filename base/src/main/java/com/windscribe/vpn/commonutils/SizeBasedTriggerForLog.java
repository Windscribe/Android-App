/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.commonutils;


import androidx.annotation.Keep;

import com.windscribe.vpn.constants.VpnPreferenceConstants;

import java.io.File;

import ch.qos.logback.core.util.FileSize;

@Keep
public class SizeBasedTriggerForLog<E> extends ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy<E> {

    @Override
    public boolean isTriggeringEvent(File activeFile, E event) {
        return (activeFile.length() >= FileSize.valueOf(VpnPreferenceConstants.DEBUG_FILE_SIZE_LOWER).getSize());
    }
}
