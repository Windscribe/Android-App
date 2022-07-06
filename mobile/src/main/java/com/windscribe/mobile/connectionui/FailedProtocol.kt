/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.connectionui

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintSet
import com.windscribe.mobile.R
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.backend.utils.LastSelectedLocation

open class FailedProtocol(
    lastSelectedLocation: LastSelectedLocation,
    connectionOptions: ConnectionOptions,
    context: Context
) : ConnectionUiState(lastSelectedLocation, connectionOptions, context) {

    override val badgeViewAlpha: Float
        get() = 0.3f

    override val connectedCenterIconVisibility: Int
        get() = ConstraintSet.VISIBLE

    override val connectingIconVisibility: Int
        get() = ConstraintSet.VISIBLE

    override val connectionStateStatusText: String
        get() = ""

    override val connectionStatusBackground: Drawable?
        get() = getDrawable(R.drawable.ic_disconnected_status_bg)

    override val connectionStatusIcon: Drawable?
        get() = getDrawable(R.drawable.failed_protcol_icon_drawable)

    override val lockIconResource: Int
        get() = R.drawable.ic_unsafe

    override val onOffButtonResource: Int
        get() = R.drawable.on_button

    override val portAndProtocolEndTextColor: Int
        get() = getColorResource(R.color.colorYellow)

    override val progressRingResource: Drawable?
        get() = getDrawable(R.drawable.progressbardrawble_error)

    override val progressRingTag : Int = R.drawable.progressbardrawble_error

    override val progressRingVisibility: Int
        get() = ConstraintSet.VISIBLE

    override fun rotateConnectingIcon(): Boolean {
        return false
    }

    override fun setConnectedPortAndProtocol(protocol: TextView, port: TextView) {
        val selectedProtocol = appContext.preference.selectedProtocol
        val selectedPort = "FAILED"
        protocol.text = selectedProtocol
        port.text = selectedPort
    }
}