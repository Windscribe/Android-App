/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.commonutils

import androidx.core.content.ContextCompat
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.Windscribe.Companion.appContext
import javax.inject.Inject

class ResourceHelper @Inject constructor() {

    fun getString(resourceId: Int): String {
        return appContext.resources.getString(resourceId)
    }

    fun getString(resourceId: Int, vararg formatArgs: Any): String {
        return appContext.resources.getString(resourceId, *formatArgs)
    }
    fun getStringArray(resourceId: Int): Array<String> {
        return appContext.resources.getStringArray(resourceId)
    }

    fun getColorResource(resourceId: Int): Int {
        return ContextCompat.getColor(appContext, resourceId)
    }

    fun getDataLeftString(resourceId: Int, dataRemaining: Float): String {
        return appContext.resources.getString(resourceId, dataRemaining)
    }
}
