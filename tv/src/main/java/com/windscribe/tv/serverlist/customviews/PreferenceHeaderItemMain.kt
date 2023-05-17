/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatTextView
import com.windscribe.tv.R
import com.windscribe.tv.serverlist.customviews.State.TwoState

class PreferenceHeaderItemMain : AppCompatTextView {
    private var marginForLine = 0
    private val paint = Paint()
    private var state = TwoState.NOT_SELECTED

    constructor(context: Context) : super(context) {
        setPaintToDraw()
        setDefaultState()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setPaintToDraw()
        setDefaultState()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setPaintToDraw()
        setDefaultState()
    }

    fun setState(state: TwoState) {
        this.state = state
        setCommonProperties()
        when (state) {
            TwoState.SELECTED -> setTextColor(context.resources.getColor(R.color.colorWhite))
            TwoState.NOT_SELECTED -> setTextColor(context.resources.getColor(R.color.colorWhite40))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (state === TwoState.SELECTED) {
            if (width > minWidth) {
                canvas.drawLine(
                    0f,
                    marginForLine.toFloat(),
                    0f,
                    (height - marginForLine).toFloat(),
                    paint
                )
                invalidate()
            }
        }
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        setState(state)
    }

    private val backgroundGradient: GradientDrawable
        get() {
            val gradientDrawable = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(
                    context.resources.getColor(R.color.colorWhite16),
                    context.resources.getColor(android.R.color.transparent)
                )
            )
            gradientDrawable.gradientType = GradientDrawable.LINEAR_GRADIENT
            return gradientDrawable
        }

    private fun setCommonProperties() {
        setTextColor(context.resources.getColor(R.color.colorDeepBlue40))
        setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            context.resources.getDimension(R.dimen.text_size_21)
        )
        setPadding(
            context.resources.getDimension(R.dimen.reg_24dp).toInt(),
            context.resources.getDimension(R.dimen.reg_16dp).toInt(),
            context.resources.getDimension(R.dimen.reg_16dp).toInt(),
            context.resources.getDimension(R.dimen.reg_16dp).toInt()
        )
        background = if (hasFocus()) {
            backgroundGradient
        } else {
            null
        }
    }

    private fun setDefaultState() {
        if (id == R.id.general || id == R.id.header_item_all) {
            setState(TwoState.SELECTED)
        } else {
            setState(TwoState.NOT_SELECTED)
        }
    }

    private fun setPaintToDraw() {
        paint.color = context.resources.getColor(R.color.colorWhite)
        paint.isAntiAlias = true
        val width = context.resources.getDimension(R.dimen.reg_4dp).toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = (width * 2).toFloat()
        paint.strokeCap = Paint.Cap.ROUND
        marginForLine = context.resources.getDimension(R.dimen.reg_12dp).toInt()
        minWidth = context.resources.getDimension(R.dimen.reg_16dp).toInt()
    }
}