package com.windscribe.mobile.custom_view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton

class PlanUpgradeGradientButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : AppCompatButton(context, attrs, defStyle) {

    private val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gradientColors = intArrayOf(
        Color.rgb(217, 211, 255), // #D9D3FF
        Color.rgb(219, 204, 247), // #DBCCF7
        Color.rgb(242, 227, 240), // #F2E3F0
        Color.rgb(218, 214, 235), // #DAD6EB
        Color.rgb(201, 227, 242), // #C9E3F2
        Color.rgb(195, 229, 237), // #C3E5ED
        Color.rgb(189, 237, 237), // #BDEDED
        Color.rgb(194, 232, 240), // #C2E8F0
        Color.rgb(202, 222, 242)  // #CADEF2
    )

    private var rect: RectF? = null

    private val gradientPositions =
        floatArrayOf(0.05f, 0.17f, 0.38f, 0.45f, 0.51f, 0.56f, 0.59f, 0.67f, 0.76f)

    private var gradientShader: LinearGradient? = null

    init {
        isAllCaps = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        gradientShader = LinearGradient(
            width * 0.21f, -height * 2.86f,
            width * 0.77f, height * 4.21f,
            gradientColors, gradientPositions,
            Shader.TileMode.CLAMP
        )
        gradientPaint.shader = gradientShader
        rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        rect?.let {
            val cornerRadius = height / 2f
            canvas.drawRoundRect(it, cornerRadius, cornerRadius, gradientPaint)
        }
        super.onDraw(canvas)
    }

    override fun setPressed(pressed: Boolean) {
        super.setPressed(pressed)
        alpha = if (pressed) 0.8f else 1.0f
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if (enabled) 1.0f else 0.5f
    }
}