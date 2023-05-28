/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.connectionui

import android.content.Context
import android.graphics.drawable.Drawable
import com.windscribe.mobile.R
import com.windscribe.vpn.backend.utils.LastSelectedLocation

class UnsecuredProtocol(
    lastSelectedLocation: LastSelectedLocation,
    connectionOptions: ConnectionOptions,
    context: Context
) : FailedProtocol(lastSelectedLocation, connectionOptions, context) {

    override val connectionStatusIcon: Drawable?
        get() = getDrawable(R.drawable.ic_wifi_unsecure_yellow)
}