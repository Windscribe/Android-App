/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.commonutils

import androidx.core.content.ContextCompat
import com.windscribe.vpn.Windscribe.Companion.appContext
import javax.inject.Inject

class ResourceHelper
    @Inject
    constructor() {
        fun getString(resourceId: Int): String = appContext.resources.getString(resourceId)

        fun getString(
            resourceId: Int,
            vararg formatArgs: Any,
        ): String = appContext.resources.getString(resourceId, *formatArgs)

        fun getStringArray(resourceId: Int): Array<String> = appContext.resources.getStringArray(resourceId)

        fun getColorResource(resourceId: Int): Int = ContextCompat.getColor(appContext, resourceId)

        fun getDataLeftString(
            resourceId: Int,
            dataRemaining: Float,
        ): String = appContext.resources.getString(resourceId, dataRemaining)
    }
