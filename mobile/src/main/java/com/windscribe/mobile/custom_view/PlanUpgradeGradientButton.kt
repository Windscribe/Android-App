package com.windscribe.mobile.custom_view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.AppCompatButton
import androidx.core.animation.doOnRepeat

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
    private var clipPath: Path? = null
    private val density = resources.displayMetrics.density // Get screen density

    private val glarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = (0.7f * 255).toInt()
        strokeWidth = 2 * density
        style = Paint.Style.STROKE
    }

    private val glareBlurPaint = Paint(glarePaint).apply {
        maskFilter = BlurMaskFilter(2.5f * density, BlurMaskFilter.Blur.NORMAL)
    }

    private val glareBlendPaint = Paint(glarePaint).apply {
        maskFilter = BlurMaskFilter(3f * density, BlurMaskFilter.Blur.NORMAL)
        alpha = 180
    }
    private var glareAnimator: ValueAnimator? = null
    private var glareOffset = -1f

    init {
        isAllCaps = false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        glareAnimator?.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        glareAnimator?.cancel()
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
        animateGlare()
    }

    override fun onDraw(canvas: Canvas) {
        rect?.let {
            val cornerRadius = height / 2f
            canvas.drawRoundRect(it, cornerRadius, cornerRadius, gradientPaint)
            if (glareAnimator?.isRunning == true) {
                drawGlare(it, canvas)
            }
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

    private fun drawGlare(rect: RectF, canvas: Canvas) {
        canvas.save()
        clipPath?.let {
            canvas.clipPath(it)
        }
        val glareSize = 93 * density
        val left = glareOffset
        val top = rect.top
        val right = left + glareSize
        val bottom = top + glareSize

        // **Glare Base Layer (70% Opacity)**
        glarePaint.alpha = (0.7f * 255).toInt()
        canvas.drawLine(right, top, left, bottom, glarePaint)

        // **Blurred Glare Layer (2.5px Blur)**
        glareBlurPaint.maskFilter = BlurMaskFilter(2.5f * density, BlurMaskFilter.Blur.NORMAL)
        canvas.drawLine(right, top, left, bottom, glareBlurPaint)

        // **Final Glare (Plus-Lighter Blend)**
        glareBlendPaint.maskFilter = BlurMaskFilter(3f * density, BlurMaskFilter.Blur.NORMAL)
        canvas.drawLine(right, top, left, bottom, glareBlendPaint)

        canvas.restore()
    }

    private fun animateGlare() {
        val cornerRadius = height / 2f
        clipPath = Path().apply {
            addRoundRect(rect!!, cornerRadius, cornerRadius, Path.Direction.CW)
        }
        val end = rect?.width() ?: 0F
        glareAnimator = ValueAnimator.ofFloat(0f, end).apply {
            duration = 800
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART

            addUpdateListener { animation ->
                glareOffset = animation.animatedValue as Float
                invalidate()
            }

            doOnRepeat {
                pause()
                Handler(Looper.getMainLooper()).postDelayed({
                    resume()
                }, 8000)
            }

            Handler(Looper.getMainLooper()).postDelayed({
                start()
            }, 800)
        }
    }
}