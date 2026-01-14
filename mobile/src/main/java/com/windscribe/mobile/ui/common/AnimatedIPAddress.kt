package com.windscribe.mobile.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
        if (shouldAnimate) {
            animationTrigger.intValue++
        }
    }

    if (ipAddress.contains("--") && !isRotatingIp) {
        Text(text = ipAddress, style = style, color = color, modifier = modifier)
    } else {
        val displayIp = if (isRotatingIp && ipAddress.contains("--")) {
            "000.000.000.000" // Placeholder to animate
        } else {
            ipAddress
        }

        key(displayIp, isRotatingIp) {
            Row(modifier = modifier) {
                var digitIndex = 0
                val totalDigits = displayIp.count { it.isDigit() }
                displayIp.forEachIndexed { index, char ->
                    key("$displayIp-$index-$char-$isRotatingIp") {
                        if (char.isDigit()) {
                            val currentDigitIndex = digitIndex++
                            val isLastDigit = currentDigitIndex == totalDigits - 1
                            AnimatedDigit(
                                targetDigit = char.digitToInt(),
                                animationTrigger = animationTrigger.intValue,
                                style = style,
                                color = color,
                                isRotating = isRotatingIp,
                                onAnimationComplete = if (isLastDigit && !isRotatingIp) {
                                    { connectionViewmodel.onIpAnimationComplete() }
                                } else null
                            )
                        } else {
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
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedDigit(
    targetDigit: Int,
    animationTrigger: Int,
    style: TextStyle,
    color: Color,
    isRotating: Boolean = false,
    onAnimationComplete: (() -> Unit)? = null
) {
    var previousDigit by remember { mutableIntStateOf(targetDigit) }
    val animatedValue = remember { Animatable(targetDigit.toFloat()) }
    val itemHeight = style.fontSize.value * 1.5f
    val randomDelay = remember(animationTrigger) { (0..40).random().toLong() }

    // Continuous animation when rotating
    LaunchedEffect(isRotating) {
        if (isRotating) {
            while (isRotating) {
                val currentValue = animatedValue.value
                animatedValue.animateTo(
                    targetValue = currentValue + 10f,
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = LinearOutSlowInEasing
                    )
                )
            }
            // When rotation stops, animate to the final target digit
            val from = animatedValue.value
            val currentMod = from.toInt() % 10
            val diff = if (targetDigit >= currentMod) {
                targetDigit - currentMod
            } else {
                (10 - currentMod) + targetDigit
            }
            animatedValue.animateTo(
                targetValue = from + diff,
                animationSpec = tween(
                    durationMillis = 500,
                    easing = LinearOutSlowInEasing
                )
            )
            animatedValue.snapTo(targetDigit.toFloat())
            previousDigit = targetDigit
            onAnimationComplete?.invoke()
        }
    }

    LaunchedEffect(animationTrigger) {
        if (animationTrigger > 0 && !isRotating) {
            delay(randomDelay)

            val from = animatedValue.value
            val currentMod = from.toInt() % 10
            val diff = if (targetDigit >= currentMod) {
                targetDigit - currentMod
            } else {
                (10 - currentMod) + targetDigit
            }

            animatedValue.animateTo(
                targetValue = from + 30f + diff,
                animationSpec = tween(
                    durationMillis = 750,
                    easing = LinearOutSlowInEasing
                )
            )

            animatedValue.snapTo(targetDigit.toFloat())
            previousDigit = targetDigit
            onAnimationComplete?.invoke()
        } else if (targetDigit != previousDigit && !isRotating) {
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
        val currentValue = animatedValue.value
        val currentFloor = currentValue.toInt()
        val delta = currentValue - currentFloor

        Text(
            text = (currentFloor % 10).toString(),
            style = style,
            color = color,
            modifier = Modifier.offset(y = (-delta * itemHeight).dp)
        )

        Text(
            text = ((currentFloor + 1) % 10).toString(),
            style = style,
            color = color,
            modifier = Modifier.offset(y = ((1 - delta) * itemHeight).dp)
        )
    }
}