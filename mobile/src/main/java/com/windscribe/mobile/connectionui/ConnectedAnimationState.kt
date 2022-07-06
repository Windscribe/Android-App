/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.connectionui

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import androidx.constraintlayout.widget.ConstraintSet
import com.windscribe.mobile.R
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.backend.utils.LastSelectedLocation

open class ConnectedAnimationState(
    lastSelectedLocation: LastSelectedLocation,
    connectionOptions: ConnectionOptions,
    context: Context
) : ConnectionUiState(lastSelectedLocation, connectionOptions, context) {
    override val badgeViewAlpha: Float
        get() = 1.0f
    override val connectionStateStatusEndColor: Int
        get() = getColorResource(R.color.colorNeonGreen)
    override val connectionStateStatusStartColor: Int
        get() = getColorResource(R.color.colorLightBlue)
    override val connectionStateStatusText: String
        get() = "ON"
    override val connectionStatusBackground: Drawable?
        get() = getDrawable(R.drawable.ic_connected_status_bg)
    override val flagGradientEndColor: Int
        get() = getColorResource(R.color.colorPrimary)
    override val flagGradientStartColor: Int
        get() = getColorResource(R.color.colorFlagGradient)
    override val headerBackgroundLeft: Drawable?
        get() = getDrawable(if (isCustomBackgroundEnabled) R.drawable.header_left_connected_custom else R.drawable.header_left_connected)
    override val headerBackgroundRight: Drawable?
        get() = getDrawable(if (isCustomBackgroundEnabled) R.drawable.header_right_connected_custom else R.drawable.header_right_connected)
    override val lockIconResource: Int
        get() = R.drawable.ic_safe
    override val onOffButtonResource: Int
        get() = R.drawable.on_button
    override val portAndProtocolEndTextColor: Int
        get() = getColorResource(R.color.colorNeonGreen)
    override val preferredProtocolStatusDrawable: Drawable?
        get() {
            val networkInfo = connectionOptions.networkInfo
            return if (networkInfo?.isPreferredOn == true) {
                getDrawable(R.drawable.ic_preferred_protocol_status_enabled)
            } else null
        }
    override val progressRingResource: Drawable?
        get() {
            val splitRouting = appContext.preference.lastConnectedUsingSplit
            return getDrawable(if (splitRouting) R.drawable.ic_connected_split_ring else R.drawable.ic_connected_ring)
        }

    override val progressRingTag : Int
    get() {
        val splitRouting = appContext.preference.lastConnectedUsingSplit
        return if (splitRouting) R.drawable.ic_connected_split_ring else R.drawable.ic_connected_ring
    }
    override val progressRingVisibility: Int
        get() = ConstraintSet.VISIBLE

    override val decoyTrafficBadgeVisibility: Int
        get(){
            val decoyTrafficOn = appContext.preference.isDecoyTrafficOn
            return if(decoyTrafficOn){
                View.VISIBLE
            } else {
                View.GONE
            }
        }
}