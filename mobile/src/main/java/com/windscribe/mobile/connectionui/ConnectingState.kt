/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.connectionui

import android.content.Context
import com.windscribe.vpn.backend.utils.LastSelectedLocation

class ConnectingState(
    lastSelectedLocation: LastSelectedLocation,
    connectionOptions: ConnectionOptions,
    context: Context
) : ConnectingAnimationState(lastSelectedLocation, connectionOptions, context)