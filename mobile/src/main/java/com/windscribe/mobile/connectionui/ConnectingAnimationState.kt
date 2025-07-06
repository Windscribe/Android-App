/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.connectionui

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.constraintlayout.widget.ConstraintSet
import com.windscribe.mobile.R
import com.windscribe.vpn.backend.utils.LastSelectedLocation

open class ConnectingAnimationState(
    lastSelectedLocation: LastSelectedLocation,
    connectionOptions: ConnectionOptions,
    context: Context
) : ConnectionUiState(lastSelectedLocation, connectionOptions, context) {
    override val connectingIconVisibility: Int
        get() = ConstraintSet.VISIBLE
    override val connectionStateStatusEndColor: Int
        get() = getColorResource(R.color.colorLightBlue)
    override val connectionStateStatusText: String
        get() = "   "
    override val connectionStatusBackground: Drawable?
        get() = getDrawable(R.drawable.ic_connecting_status_bg)
    override val flagGradientEndColor: Int
        get() = if (isCustomBackgroundEnabled) {
            getColorResource(R.color.colorFlagGradient50)
        } else {
            getColorResource(R.color.colorFlagGradient)
        }
    override val headerBackgroundLeft: Drawable?
        get() = getDrawable(if (isCustomBackgroundEnabled) R.drawable.header_left_connected_custom else R.drawable.header_left_connected)
    override val headerBackgroundRight: Drawable?
        get() = getDrawable(if (isCustomBackgroundEnabled) R.drawable.header_right_connected_custom else R.drawable.header_right_connected)
    override val onOffButtonResource: Int
        get() = R.drawable.on_button
    override val portAndProtocolEndTextColor: Int
        get() = getColorResource(R.color.colorLightBlue)
    override val preferredProtocolStatusDrawable: Drawable?
        get() {
            return if (connectionOptions.isPreferred) {
                getDrawable(R.drawable.ic_preferred_protocol_status_enabling)
            } else null
        }
    override val progressRingVisibility: Int
        get() = ConstraintSet.VISIBLE

    override val antiCensorShipStatusDrawable
        get() = getDrawable(R.drawable.ic_anti_censorship_enabling)
}