/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.customviews

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.ResourcesCompat
import com.windscribe.tv.R

class ConnectButtonView : AppCompatImageView {
    private var padding = 0
    private var radius = 0
    private var state = 1
    private var stroke = 0

    constructor(context: Context) : super(context) {
        setDimens()
        setState(state)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setDimens()
        setState(state)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setDimens()
        setState(state)
    }

    private fun setDefaultState() {
        if (hasFocus()) {
            val gradientDrawable = GradientDrawable()
            gradientDrawable.shape = GradientDrawable.OVAL
            gradientDrawable.cornerRadius = radius.toFloat()
            gradientDrawable.setStroke(stroke, resources.getColor(R.color.colorWhite20))
            gradientDrawable.setSize(
                context.resources.getDimension(R.dimen.reg_56).toInt(),
                context.resources.getDimension(R.dimen.reg_56).toInt()
            )
            setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources, R.drawable.ic_connect_icon_focused,
                    context.theme
                )
            )
            setPadding(0, 0, 0, 0)
            scaleType = ScaleType.FIT_CENTER
            background = gradientDrawable
        } else {
            val gradientDrawable = GradientDrawable()
            gradientDrawable.shape = GradientDrawable.OVAL
            gradientDrawable.cornerRadius = radius.toFloat()
            gradientDrawable.setStroke(stroke, resources.getColor(R.color.colorWhite20))
            setImageDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.ic_connect_icon, context.theme)
            )
            setPadding(padding, padding, padding, padding)
            scaleType = ScaleType.FIT_CENTER
            background = gradientDrawable
        }
    }

    fun setState(currentState: Int) {
        state = currentState
        setDefaultState()
    }

    override fun onFocusChanged(
        gainFocus: Boolean,
        direction: Int,
        previouslyFocusedRect: Rect?
    ) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        setDefaultState()
    }

    private fun setDimens() {
        padding = context.resources.getDimension(R.dimen.reg_14dp).toInt()
        radius = context.resources.getDimension(R.dimen.reg_56).toInt() / 2
        stroke = context.resources.getDimension(R.dimen.reg_2dp).toInt()
    }
}