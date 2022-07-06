/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.fragments

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.leanback.widget.VerticalGridView
import com.windscribe.tv.R

class CustomVerticalGridView : VerticalGridView {
    interface CustomFocusListener {
        fun onExit()
    }

    private var customFocusListener: CustomFocusListener? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    override fun focusSearch(focused: View, direction: Int): View {
        if (direction == FOCUS_UP && focused.id == R.id.server_item && nextFocusUpId != R.id.server_item && selectedPosition <= 3) {
            customFocusListener?.onExit()
        }
        return super.focusSearch(focused, direction)
    }

    fun setCustomFocusListener(customFocusListener: CustomFocusListener) {
        this.customFocusListener = customFocusListener
    }
}