/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.customviews

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.LinearLayoutCompat
import com.windscribe.tv.R
import kotlin.math.roundToInt

class HomeUpgradeButton : LinearLayoutCompat {
    private var viewHeight = 0
    private var padding = 0
    private var sidePadding = 0
    private var stroke = 0

    constructor(context: Context) : super(context) {
        setDimens()
        setState()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setDimens()
        setState()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setDimens()
        setState()
    }

    fun setState() {
        val halfHeight = height / 2
        if (hasFocus()) {
            val gradientDrawable = GradientDrawable()
            gradientDrawable.shape = GradientDrawable.RECTANGLE
            gradientDrawable.cornerRadius = halfHeight.toFloat().roundToInt().toFloat()
            gradientDrawable.setColor(context.resources.getColor(R.color.colorWhite24))
            setPadding(sidePadding, padding, sidePadding, padding)
            background = gradientDrawable
        } else {
            val gradientDrawable = GradientDrawable()
            gradientDrawable.shape = GradientDrawable.RECTANGLE
            gradientDrawable.cornerRadius = halfHeight.toFloat().roundToInt().toFloat()
            gradientDrawable.setStroke(stroke, resources.getColor(R.color.colorWhite24))
            setPadding(sidePadding, padding, sidePadding, padding)
            background = gradientDrawable
        }
    }

    override fun onFocusChanged(
        gainFocus: Boolean,
        direction: Int,
        previouslyFocusedRect: Rect?
    ) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        setState()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        viewHeight = heightMeasureSpec
        setState()
    }

    private fun setDimens() {
        padding = context.resources.getDimension(R.dimen.reg_14dp).toInt()
        stroke = context.resources.getDimension(R.dimen.reg_2dp).toInt()
        sidePadding = context.resources.getDimension(R.dimen.reg_16dp).toInt()
        viewHeight = measuredHeight
    }
}