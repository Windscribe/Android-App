/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.connectionui

import android.content.Context
import android.graphics.drawable.Drawable
import com.windscribe.mobile.R
import com.windscribe.vpn.backend.utils.LastSelectedLocation

class DisconnectedState(
    lastSelectedLocation: LastSelectedLocation,
    connectionOptions: ConnectionOptions,
    context: Context
) : ConnectionUiState(lastSelectedLocation, connectionOptions, context) {

    override val connectionStatusBackground: Drawable?
        get() = getDrawable(R.drawable.ic_disconnected_status_bg)

    override val flagGradientEndColor: Int
        get() = if (isCustomBackgroundEnabled) {
            getColorResource(R.color.colorDeepBlue50)
        } else {
            getColorResource(R.color.colorDeepBlue0)
        }

    override val flagGradientStartColor: Int
        get() = if (isCustomBackgroundEnabled) {
            getColorResource(R.color.colorDeepBlue50)
        } else {
            getColorResource(R.color.colorDeepBlue0)
        }

    override val preferredProtocolStatusDrawable: Drawable?
        get() = if (connectionOptions.networkInfo?.isPreferredOn == true) {
            getDrawable(R.drawable.ic_preferred_protocol_status_disabled)
        } else null
}