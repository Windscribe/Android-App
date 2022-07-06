/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.customviews

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.windscribe.tv.R

class FocusAwareConstraintLayout : ConstraintLayout {
    interface OnWindowResizeListener {
        fun focusEnterToHeader()
    }

    private var listener: OnWindowResizeListener? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(
        context, attrs
    )

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    )

    override fun focusSearch(focused: View, direction: Int): View? {
        val id = focused.id
        if (direction == FOCUS_DOWN) { // Open browse overlay window
            if (R.id.vpn == id) {
                listener?.focusEnterToHeader()
                return null
            }
        }
        return super.focusSearch(focused, direction)
    }

    fun setListener(listener: OnWindowResizeListener) {
        this.listener = listener
    }
}