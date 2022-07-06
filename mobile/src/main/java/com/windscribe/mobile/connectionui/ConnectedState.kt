/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.connectionui

import android.content.Context
import androidx.constraintlayout.widget.ConstraintSet
import com.windscribe.mobile.R
import com.windscribe.vpn.backend.utils.LastSelectedLocation

class ConnectedState(
    lastSelectedLocation: LastSelectedLocation,
    connectionOptions: ConnectionOptions,
    context: Context
) : ConnectedAnimationState(lastSelectedLocation, connectionOptions, context) {
    override val badgeViewAlpha: Float
        get() = 1.0f
    override val connectedCenterIconVisibility: Int
        get() = ConstraintSet.VISIBLE
    override val lockIconResource: Int
        get() = R.drawable.ic_safe
    override val progressRingVisibility: Int
        get() = ConstraintSet.VISIBLE
}