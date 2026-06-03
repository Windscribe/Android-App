package com.windscribe.mobile.ui.upgrade

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.windscribe.mobile.ui.helper.hapticClickable
import com.windscribe.mobile.ui.theme.font18
import kotlinx.coroutines.delay

/**
 * Compose reimplementation of the legacy `PlanUpgradeGradientButton`: a fully-rounded pill filled
 * with the same diagonal multi-stop gradient, with a soft diagonal "glare" line that sweeps across
 * once and then pauses, on the same cadence as the original view.
 *
 * Original timings/geometry (PlanUpgradeGradientButton): gradient stops + colors verbatim; glare =
 * a 66dp-wide forward-slash drawn three times (a crisp pass + two blurred passes), animated 0..width
 * over 800ms, an 8000ms pause between sweeps, and a 1500ms initial delay.
 */

private val GradientColors =
    listOf(
        Color(0xFFD9D3FF),
        Color(0xFFDBCCF7),
        Color(0xFFF2E3F0),
        Color(0xFFDAD6EB),
        Color(0xFFC9E3F2),
        Color(0xFFC3E5ED),
        Color(0xFFBDEDED),
        Color(0xFFC2E8F0),
        Color(0xFFCADEF2),
    )

private val GradientStops =
    floatArrayOf(0.05f, 0.17f, 0.38f, 0.45f, 0.51f, 0.56f, 0.59f, 0.67f, 0.76f)

private const val SWEEP_DURATION_MS = 800
private const val PAUSE_BETWEEN_SWEEPS_MS = 8000L
private const val INITIAL_DELAY_MS = 1500L

@Composable
fun GradientGlareButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val density = LocalDensity.current
    val glareSizePx = with(density) { 66.dp.toPx() }
    val strokePx = with(density) { 2.dp.toPx() }
    val blurInnerPx = with(density) { 2.5.dp.toPx() }
    val blurOuterPx = with(density) { 3.dp.toPx() }

    // Glare position as a fraction 0..1 of the button width; -1f = idle (hidden). Sweeps over
    // 800ms, then waits 8s before the next sweep, after a 1500ms initial delay.
    val glare = remember { Animatable(-1f) }
    LaunchedEffect(enabled) {
        if (!enabled) {
            glare.snapTo(-1f)
            return@LaunchedEffect
        }
        delay(INITIAL_DELAY_MS)
        while (true) {
            glare.snapTo(0f)
            glare.animateTo(1f, animationSpec = tween(SWEEP_DURATION_MS, easing = LinearEasing))
            glare.snapTo(-1f)
            delay(PAUSE_BETWEEN_SWEEPS_MS)
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(50.dp)
                .alpha(if (enabled) 1f else 0.5f)
                .clip(RoundedCornerShape(percent = 50))
                .hapticClickable { if (enabled) onClick() }
                .drawWithCache {
                    val cornerRadius = size.height / 2f
                    val brush =
                        Brush.linearGradient(
                            colorStops = GradientStops.zip(GradientColors).toTypedArray(),
                            start = Offset(size.width * 0.21f, -size.height * 2.86f),
                            end = Offset(size.width * 0.77f, size.height * 4.21f),
                            tileMode = TileMode.Clamp,
                        )
                    onDrawBehind {
                        drawRoundRect(
                            brush = brush,
                            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                        )
                        val pos = glare.value
                        if (pos >= 0f) {
                            drawGlare(pos, glareSizePx, strokePx, blurInnerPx, blurOuterPx)
                        }
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = font18.copy(fontSize = 18.sp, textAlign = TextAlign.Center),
            color = Color(0xFF090E19),
        )
    }
}

/**
 * Draws the diagonal glare (a forward-slash from bottom-left to top-right) three times: a crisp
 * pass plus two blurred passes, matching the original view. The caller has already clipped to the
 * pill, so the line is masked to the button. [pos] is 0..1 across the width.
 */
private fun DrawScope.drawGlare(
    pos: Float,
    glareSizePx: Float,
    strokePx: Float,
    blurInnerPx: Float,
    blurOuterPx: Float,
) {
    val left = pos * size.width
    val right = left + glareSizePx
    val top = size.height
    val bottom = 0f
    drawIntoCanvas { canvas ->
        val nativeCanvas = canvas.nativeCanvas

        fun line(
            lineAlpha: Float,
            blurRadius: Float,
        ) {
            val paint =
                android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                    alpha = (lineAlpha * 255).toInt()
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = strokePx
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    if (blurRadius > 0f) {
                        maskFilter =
                            android.graphics.BlurMaskFilter(blurRadius, android.graphics.BlurMaskFilter.Blur.NORMAL)
                    }
                }
            nativeCanvas.drawLine(left, top, right, bottom, paint)
        }
        line(lineAlpha = 0.7f, blurRadius = 0f)
        line(lineAlpha = 0.7f, blurRadius = blurInnerPx)
        line(lineAlpha = 0.7f, blurRadius = blurOuterPx)
    }
}
