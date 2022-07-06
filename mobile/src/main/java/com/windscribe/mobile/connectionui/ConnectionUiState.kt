/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.connectionui

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat
import com.windscribe.mobile.R
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.backend.utils.LastSelectedLocation
import com.windscribe.vpn.commonutils.FlagIconResource

open class ConnectionUiState internal constructor(
    private val savedLocation: LastSelectedLocation?,
    var connectionOptions: ConnectionOptions,
    private val context: Context
) {
    private val flagIcons: Map<String, Int> = FlagIconResource.flagIcons
    open val badgeViewAlpha: Float
        get() = 0.3f

    fun getColorResource(color: Int): Int {
        return context.resources.getColor(color)
    }

    open val connectedCenterIconVisibility: Int
        get() = ConstraintSet.GONE
    val connectedFlagPath: String?
        get() = appContext.preference.connectedFlagPath
    val disconnectedFlagPath: String?
        get() = appContext.preference.disConnectedFlagPath
    open val connectingIconVisibility: Int
        get() = View.GONE
    open val connectionStateStatusEndColor: Int
        get() = getColorResource(R.color.colorWhite)
    open val connectionStateStatusStartColor: Int
        get() = getColorResource(R.color.colorWhite50)
    open val connectionStateStatusText: String
        get() = "OFF"
    open val connectionStatusBackground: Drawable?
        get() = getDrawable(R.drawable.ic_disconnected_status_bg)
    open val connectionStatusIcon: Drawable?
        get() = getDrawable(R.drawable.connection_icon_drawable)

    fun getDrawable(drawable: Int): Drawable? {
        return ResourcesCompat.getDrawable(context.resources, drawable, context.theme)
    }

    val flag: Int
        get() = if (savedLocation != null && flagIcons.containsKey(
                savedLocation.countryCode
            )
        ) {
            flagIcons[savedLocation.countryCode]!!
        } else R.drawable.dummy_flag
    open val flagGradientEndColor: Int
        get() = getColorResource(R.color.colorDeepBlue)
    open val flagGradientStartColor: Int
        get() = getColorResource(R.color.colorDeepBlue)
    open val headerBackgroundLeft: Drawable?
        get() = getDrawable(if (isCustomBackgroundEnabled) R.drawable.header_left_disconnected_custom else R.drawable.header_left_disconnected)
    open val headerBackgroundRight: Drawable?
        get() = getDrawable(if (isCustomBackgroundEnabled) R.drawable.header_right_disconnected_custom else R.drawable.header_right_disconnected)
    open val lockIconResource: Int
        get() = R.drawable.ic_unsafe
    open val onOffButtonResource: Int
        get() = R.drawable.off_button
    open val portAndProtocolEndTextColor: Int
        get() = getColorResource(R.color.colorWhite50)
    open val preferredProtocolStatusDrawable: Drawable?
        get() {
            return if (connectionOptions.networkInfo?.isPreferredOn == true) {
                getDrawable(R.drawable.ic_preferred_protocol_status_disabled)
            } else null
        }
    open val preferredProtocolStatusVisibility: Int
        get() {
            return if (connectionOptions.networkInfo?.isPreferredOn == true) {
                View.VISIBLE
            } else View.GONE
        }
    open val progressRingResource: Drawable?
        get() = getDrawable(R.drawable.progressbardrawble)
    open val progressRingVisibility: Int
        get() = View.INVISIBLE

    open val progressRingTag : Int = R.drawable.progressbardrawble

    open fun rotateConnectingIcon(): Boolean {
        return true
    }
    open fun setConnectedPortAndProtocol(protocol: TextView, port: TextView) {}

    val isCustomBackgroundEnabled: Boolean
    get() = appContext.preference.isCustomBackground

    open val decoyTrafficBadgeVisibility: Int = View.GONE
}