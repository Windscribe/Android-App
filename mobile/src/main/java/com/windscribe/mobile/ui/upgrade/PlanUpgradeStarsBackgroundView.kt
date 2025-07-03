package com.windscribe.mobile.ui.upgrade

import android.content.Context
import android.content.res.Resources
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.windscribe.mobile.R
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class PlanUpgradeStarsBackgroundView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    private var glowRect: RectF? = null
    private var starPadding = 0F
    private val stars = mutableListOf<Star>()
    private var innerRect: RectF? = null
    private val cornerRadius = resources.getDimension(R.dimen.reg_12dp)
    private val gradientTopColor = ContextCompat.getColor(context, R.color.colorPowderBlue14)
    private val gradientBottomColor = ContextCompat.getColor(context, R.color.colorPowderBlue0)
    private var gradient: LinearGradient? = null
    var active = true
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.colorPowderBlue)
        strokeWidth = 3f
        style = Paint.Style.STROKE
        maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL)
    }
    private val activeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.colorPaleBlue)
        style = Paint.Style.STROKE
        strokeWidth = resources.getDimensionPixelSize(R.dimen.reg_2dp).toFloat()
    }
    private val strokePaint = activeStrokePaint.apply {
        strokeWidth = resources.getDimensionPixelSize(R.dimen.reg_1dp).toFloat()
        color = ContextCompat.getColor(context, R.color.colorWhite50)
    }
    private val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        createRect()
        createGradient()
        generateStars()
    }

    private fun createRect() {
        val left = paddingLeft.toFloat()
        val top = paddingTop.toFloat()
        val right = width - paddingRight.toFloat()
        val bottom = height - paddingBottom.toFloat()
        val strokeWidth = resources.getDimensionPixelSize(R.dimen.reg_2dp).toFloat()
        glowRect = RectF(left, top, right, bottom)
        innerRect =
            RectF(left + strokeWidth, top + strokeWidth, right - strokeWidth, bottom - strokeWidth)
        starPadding = paddingLeft.toFloat() + strokeWidth
    }

    private fun createGradient() {
        gradient = LinearGradient(
            0f,
            0f,
            0f,
            height.toFloat(),
            gradientTopColor,
            gradientBottomColor,
            Shader.TileMode.CLAMP
        )
        gradientPaint.shader = gradient
    }

    private fun generateStars() {
        stars.clear()
        val starCount = getStartCount(resources, width, height)
        for (i in 0 until starCount) {
            val angle = Random.nextFloat() * 360 // Random movement direction
            val speed = Random.nextFloat() * 0.8f + 0.2f // Random speed

            val x =
                starPadding + Random.nextFloat() * (width - 2 * starPadding) // Keep within width bounds
            val y =
                starPadding + Random.nextFloat() * (height - 2 * starPadding) // Keep within height bounds

            stars.add(
                Star(
                    x = x,
                    y = y,
                    size = Random.nextFloat() * 1.2f,
                    alpha = Random.nextFloat() * 255,
                    dx = cos(Math.toRadians(angle.toDouble())).toFloat() * speed,
                    dy = sin(Math.toRadians(angle.toDouble())).toFloat() * speed
                )
            )
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
        if (active) {
            addGradient(canvas)
            addStroke(canvas, activeStrokePaint)
            addGlow(canvas)
            updateStarts(canvas)
        } else {
            addStroke(canvas, strokePaint)
        }
        invalidate()
    }

    private fun updateStarts(canvas: Canvas) {
        for (star in stars) {
            starPaint.alpha = star.alpha.toInt()
            canvas.drawCircle(star.x, star.y, star.size, starPaint)
            star.x += star.dx
            star.y += star.dy
            if (star.x <= starPadding || star.x >= width - starPadding) {
                star.dx = -star.dx // Reverse X direction
                star.x = star.x.coerceIn(starPadding, width - starPadding) // Keep inside bounds
            }
            if (star.y <= starPadding || star.y >= height - starPadding) {
                star.dy = -star.dy // Reverse Y direction
                star.y = star.y.coerceIn(starPadding, height - starPadding) // Keep inside bounds
            }
            star.alpha -= 0.5f
            if (star.alpha <= 50) star.alpha = 255f
        }
    }

    private fun addGlow(canvas: Canvas) {
        glowRect?.let {
            canvas.drawRoundRect(it, cornerRadius, cornerRadius, glowPaint)
        }
    }

    private fun addStroke(canvas: Canvas, paint: Paint) {
        innerRect?.let {
            canvas.drawRoundRect(it, cornerRadius, cornerRadius, paint)
        }
    }

    private fun addGradient(canvas: Canvas) {
        innerRect?.let {
            canvas.drawRoundRect(it, cornerRadius, cornerRadius, gradientPaint)
        }
    }

    data class Star(
        var x: Float, var y: Float, var size: Float, var alpha: Float, var dx: Float, var dy: Float
    )
}