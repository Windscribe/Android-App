/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.windscribe.tv.R

class ConnectButtonGlow : AppCompatImageView {
    private enum class State {
        Increasing, Decreasing
    }

    private var currentState = State.Decreasing
    private var glowRadius = 0
    private var glowWidth = 0f
    private var glowWidthMax = 0f
    private var glowWidthMin = 0f
    private var iconPadding = 0
    private var outerPaint: Paint? = null
    private var paint: Paint? = null

    constructor(context: Context) : super(context) {
        setUp()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setUp()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setUp()
    }

    override fun onDraw(canvas: Canvas) {
        if (currentState == State.Decreasing) {
            if (glowWidth <= glowWidthMin) {
                currentState = State.Increasing
            }
            glowWidth -= 0.5f
        } else {
            if (glowWidth >= glowWidthMax) {
                currentState = State.Decreasing
            }
            glowWidth += 0.2f
        }
        val x = width.toFloat()
        val y = height.toFloat()
        outerPaint?.let {
            canvas.drawCircle(x / 2, y / 2, glowRadius - iconPadding + glowWidth, it)
        }
        paint?.let {
            canvas.drawCircle(x / 2, y / 2, glowRadius - iconPadding + glowWidth, it)
        }
        invalidate()
    }

    private fun setUp() {
        glowRadius = context.resources.getDimension(R.dimen.reg_200).toInt() / 2
        val glowColor = context.resources.getColor(R.color.colorWhite16)
        iconPadding = context.resources.getDimension(R.dimen.reg_12dp).toInt()
        glowWidth = context.resources.getDimension(R.dimen.reg_8dp)
        glowWidthMax = context.resources.getDimension(R.dimen.reg_8dp)
        glowWidthMin = context.resources.getDimension(R.dimen.reg_4dp)
        paint = Paint()
        paint?.color = Color.TRANSPARENT
        paint?.style = Paint.Style.FILL
        paint?.isAntiAlias = true
        outerPaint = Paint()
        outerPaint?.color = glowColor
        outerPaint?.style = Paint.Style.STROKE
        outerPaint?.isAntiAlias = true
        outerPaint?.strokeWidth = glowWidthMax
    }
}