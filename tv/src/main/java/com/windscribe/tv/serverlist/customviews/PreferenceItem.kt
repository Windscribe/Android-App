/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.customviews

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import com.windscribe.tv.R
import com.windscribe.tv.serverlist.customviews.State.MenuButtonState

class PreferenceItem : AppCompatButton {
    private var radius = 0
    private var state = MenuButtonState.NotSelected
    private var stroke = 0

    constructor(context: Context) : super(context) {
        setUnits()
        setState(MenuButtonState.NotSelected)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setUnits()
        setState(MenuButtonState.NotSelected)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setUnits()
        setState(MenuButtonState.NotSelected)
    }

    fun setState(state: MenuButtonState) {
        this.state = state
        when (state) {
            MenuButtonState.Selected -> {
                setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check_black_24dp, 0)
                background = if (hasFocus()) {
                    shapeWithFocus
                } else {
                    shapeWithOutFocus
                }
            }
            MenuButtonState.NotSelected -> {
                setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                if (hasFocus()) {
                    background = shapeWithFocus
                    setTextColor(context.resources.getColor(R.color.colorWhite))
                } else {
                    setTextColor(context.resources.getColor(R.color.colorWhite50))
                    background = null
                }
            }
        }
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        setState(state)
    }

    private val shapeWithFocus: GradientDrawable
        get() {
            val shape = GradientDrawable()
            shape.shape = GradientDrawable.RECTANGLE
            shape.cornerRadius = radius.toFloat()
            shape.setColor(context.resources.getColor(R.color.colorWhite16))
            return shape
        }
    private val shapeWithOutFocus: GradientDrawable
        get() {
            val shape = GradientDrawable()
            shape.shape = GradientDrawable.RECTANGLE
            shape.cornerRadius = radius.toFloat()
            shape.setStroke(stroke, context.resources.getColor(R.color.colorWhite16))
            return shape
        }

    private fun setUnits() {
        stroke = context.resources.getDimension(R.dimen.reg_2dp).toInt()
        radius = context.resources.getDimension(R.dimen.reg_4dp).toInt()
    }
}