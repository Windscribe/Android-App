package com.windscribe.mobile.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.windscribe.mobile.ui.connection.BridgeApiViewModel
import com.windscribe.mobile.ui.connection.ConnectionViewmodel
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun AnimatedIPAddress(
    connectionViewmodel: ConnectionViewmodel,
    bridgeApiViewModel: BridgeApiViewModel,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(fontSize = 20.sp),
    color: Color = Color.Black,
) {
    val ipAddress by connectionViewmodel.ipState.collectAsState()
    val shouldAnimate by connectionViewmodel.shouldAnimateIp.collectAsState()
    val isRotatingIp by bridgeApiViewModel.isRotatingIp.collectAsState()

    val animationTrigger = remember { mutableIntStateOf(0) }

    LaunchedEffect(shouldAnimate) {
        if (shouldAnimate) animationTrigger.intValue++
    }

    // Fast-path: no animation, static IP
    if (!isRotatingIp && ipAddress.contains("--")) {
        Text(ipAddress, style = style, color = color, modifier = modifier)
        return
    }

    val displayIp = remember(ipAddress, isRotatingIp) {
        if (isRotatingIp && ipAddress.contains("--")) {
            "000.000.000.000"
        } else ipAddress
    }

    // Stable callback (VERY important)
    val onAnimationCompleteStable = remember(connectionViewmodel) {
        { connectionViewmodel.onIpAnimationComplete() }
    }

    // Precompute digit positions (no mutation in composition)
    val digitIndices = remember(displayIp) {
        displayIp.mapIndexedNotNull { index, c ->
            if (c.isDigit()) index else null
        }
    }
    val lastDigitIndex = digitIndices.lastOrNull()

    Row(modifier = modifier) {
        displayIp.forEachIndexed { index, char ->
            key(index) {
                if (char.isDigit()) {
                    AnimatedDigit(
                        targetDigit = char.digitToInt(),
                        animationTrigger = animationTrigger.intValue,
                        style = style,
                        color = color,
                        isRotating = isRotatingIp,
                        onAnimationComplete =
                            if (index == lastDigitIndex && !isRotatingIp)
                                onAnimationCompleteStable
                            else null
                    )
                } else {
                    Separator(char, style, color)
                }
            }
        }
    }
}

@Composable
private fun Separator(
    char: Char,
    style: TextStyle,
    color: Color
) {
    Box(
        modifier = Modifier.height((style.fontSize.value * 1.5f).dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = char.toString(),
            style = style,
            color = color,
            modifier = Modifier.offset(y = (-1).dp)
        )
    }
}

@Composable
private fun AnimatedDigit(
    targetDigit: Int,
    animationTrigger: Int,
    style: TextStyle,
    color: Color,
    isRotating: Boolean,
    onAnimationComplete: (() -> Unit)?
) {
    val animatedValue = remember { Animatable(targetDigit.toFloat()) }
    var previousDigit by remember { mutableIntStateOf(targetDigit) }

    val itemHeight = style.fontSize.value * 1.5f
    val randomDelay = remember(animationTrigger) { Random.nextLong(0, 40) }

    // Continuous rotation mode
    LaunchedEffect(isRotating) {
        if (!isRotating) return@LaunchedEffect

        while (isRotating) {
            animatedValue.animateTo(
                animatedValue.value + 10f,
                animationSpec = tween(300, easing = LinearOutSlowInEasing)
            )
        }
    }

    // Final settle after rotation OR normal change animation
    LaunchedEffect(animationTrigger, isRotating) {
        if (isRotating || animationTrigger == 0) return@LaunchedEffect

        delay(randomDelay)

        val from = animatedValue.value
        val currentMod = from.toInt() % 10
        val diff =
            if (targetDigit >= currentMod)
                targetDigit - currentMod
            else
                10 - currentMod + targetDigit

        animatedValue.animateTo(
            from + 30f + diff,
            animationSpec = tween(750, easing = LinearOutSlowInEasing)
        )

        animatedValue.snapTo(targetDigit.toFloat())
        previousDigit = targetDigit
        onAnimationComplete?.invoke()
    }

    // Snap-only update when digit changes without animation
    LaunchedEffect(targetDigit, isRotating) {
        if (!isRotating && targetDigit != previousDigit) {
            animatedValue.snapTo(targetDigit.toFloat())
            previousDigit = targetDigit
        }
    }

    Box(
        modifier = Modifier
            .height(itemHeight.dp)
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        val value = animatedValue.value
        val floor = value.toInt()
        val delta = value - floor

        Text(
            text = (floor % 10).toString(),
            style = style,
            color = color,
            modifier = Modifier.offset(y = (-delta * itemHeight).dp)
        )

        Text(
            text = ((floor + 1) % 10).toString(),
            style = style,
            color = color,
            modifier = Modifier.offset(y = ((1 - delta) * itemHeight).dp)
        )
    }
}