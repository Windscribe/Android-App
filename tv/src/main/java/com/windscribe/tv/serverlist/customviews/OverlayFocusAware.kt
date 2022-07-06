/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.customviews

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.gms.common.util.ArrayUtils
import com.windscribe.tv.R

class OverlayFocusAware : ConstraintLayout {
    private val contentIds = intArrayOf(
        R.id.server_item, R.id.connect, R.id.all_server_view,
        R.id.all_server_view_static
    )
    private val headerIds = intArrayOf(
        R.id.header_item_all, R.id.header_item_fav, R.id.header_item_wind,
        R.id.header_item_static
    )
    private var currentFragment = 0

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(
        context, attrs
    )

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    )

    override fun focusSearch(focused: View, direction: Int): View {
        var viewId: Int? = null
        if (direction == FOCUS_LEFT && ArrayUtils.contains(contentIds, focused.id)) {
            if (nextFocusLeftId == -1) {
                viewId = headerIds[currentFragment]
            }
        }
        if (direction == FOCUS_RIGHT && ArrayUtils.contains(headerIds, focused.id)) {
            if (nextFocusRightId == -1) {
                viewId = contentIds[currentFragment]
            }
        }
        headerIds.firstOrNull {
            it == focused.id
        }?.let {
            val index = headerIds.indexOf(it)
            if (index in 0..2 && direction == FOCUS_DOWN) {
                viewId = headerIds[index + 1]
            }
            if (index in 1..3 && direction == FOCUS_UP) {
                viewId = headerIds[index - 1]
            }
        }
        return viewId?.let {
            return findViewById(it)
        } ?: focused
    }

    fun setCurrentFragment(currentFragment: Int) {
        this.currentFragment = currentFragment
    }
}