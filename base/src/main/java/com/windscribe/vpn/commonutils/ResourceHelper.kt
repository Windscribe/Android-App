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
        fun getString(resourceId: Int): String = appContext.localized.resources.getString(resourceId)

        fun getString(
            resourceId: Int,
            vararg formatArgs: Any,
        ): String = appContext.localized.resources.getString(resourceId, *formatArgs)

        fun getStringArray(resourceId: Int): Array<String> = appContext.localized.resources.getStringArray(resourceId)

        fun getColorResource(resourceId: Int): Int = ContextCompat.getColor(appContext, resourceId)

        fun getDataLeftString(
            resourceId: Int,
            dataRemaining: Float,
        ): String = appContext.localized.resources.getString(resourceId, dataRemaining)
    }
