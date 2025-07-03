package com.windscribe.mobile.ui.upgrade

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class StarsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val stars = mutableListOf<Star>()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        generateStars()
    }

    private fun generateStars() {
        stars.clear()
        val starCount = getStartCount(resources, width, height)
        repeat(starCount) {
            val x = Random.nextFloat() * width
            val y = Random.nextFloat() * height
            val size = Random.nextFloat() * 1.2f
            stars.add(Star(x, y, size))
        }
    }

    private fun getStartCount(resources: Resources, width: Int, height: Int): Int {
        val density = resources.displayMetrics.density
        val referenceWidthPx = 163f * density
        val referenceHeightPx = 182f * density
        val starDensity = 60 / (referenceWidthPx * referenceHeightPx)
        return (starDensity * width * height).toInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (star in stars) {
            canvas.drawCircle(star.x, star.y, star.size, starPaint)
        }
    }

    data class Star(val x: Float, val y: Float, val size: Float)
}