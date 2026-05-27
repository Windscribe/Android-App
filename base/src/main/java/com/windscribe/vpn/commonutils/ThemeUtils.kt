/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.commonutils

import android.content.Context
import android.util.TypedValue
import androidx.core.content.ContextCompat

object ThemeUtils {
    @JvmStatic
    fun getColor(context: Context, attributeResId: Int, defaultValue: Int): Int {
        val tv = TypedValue()
        val found = context.theme.resolveAttribute(attributeResId, tv, true)
        val id = if (found) tv.resourceId else defaultValue
        return ContextCompat.getColor(context, id)
    }

    @JvmStatic
    fun getResourceId(context: Context, attributeResId: Int, defaultValue: Int): Int {
        val tv = TypedValue()
        val found = context.theme.resolveAttribute(attributeResId, tv, true)
        return if (found) tv.resourceId else defaultValue
    }
}