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
import com.windscribe.mobile.ui.connection.ConnectionViewmodel
import kotlinx.coroutines.delay

/**
 * Animated IP Address display with slot-machine style digit animation.
 * Animates transitions between valid IPs, ignoring placeholder states.
 */
@Composable
fun AnimatedIPAddress(
    connectionViewmodel: ConnectionViewmodel,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(fontSize = 20.sp),
    color: Color = Color.Black,
) {
    val ipAddress by connectionViewmodel.ipState.collectAsState()
    val shouldAnimate by connectionViewmodel.shouldAnimateIp.collectAsState()
    if (ipAddress.contains("--")) {
        Text(text = ipAddress, style = style, color = color, modifier = modifier)
    } else {
        Row(modifier = modifier) {
            var digitIndex = 0
            val totalDigits = ipAddress.count { it.isDigit() }
            ipAddress.forEachIndexed { index, char ->
                key("ip-$index") {
                    if (char.isDigit()) {
                        val currentDigitIndex = digitIndex++
                        val isLastDigit = currentDigitIndex == totalDigits - 1
                        AnimatedDigit(
                            targetDigit = char.digitToInt(),
                            shouldAnimate = shouldAnimate,
                            digitIndex = currentDigitIndex,
                            style = style,
                            color = color,
                            onAnimationComplete = if (isLastDigit) {
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

@Composable
private fun AnimatedDigit(
    targetDigit: Int,
    shouldAnimate: Boolean,
    digitIndex: Int,
    style: TextStyle,
    color: Color,
    onAnimationComplete: (() -> Unit)? = null
) {
    var currentDigit by remember { mutableIntStateOf(targetDigit) }
    val offsetY = remember { Animatable(0f) }
    val itemHeight = style.fontSize.value * 1.5f

    LaunchedEffect(targetDigit, shouldAnimate) {
        if (shouldAnimate) {
            delay(digitIndex * 65L)

            repeat(5) {
                currentDigit = (0..9).random()
                offsetY.snapTo(itemHeight)
                offsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = 78,
                        easing = LinearOutSlowInEasing
                    )
                )
                offsetY.animateTo(
                    targetValue = -itemHeight,
                    animationSpec = tween(
                        durationMillis = 78,
                        easing = LinearOutSlowInEasing
                    )
                )
            }

            currentDigit = targetDigit
            offsetY.snapTo(itemHeight)
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 390,
                    easing = LinearOutSlowInEasing
                )
            )

            // Call completion callback for the last digit
            onAnimationComplete?.invoke()
        } else {
            currentDigit = targetDigit
        }
    }

    Box(
        modifier = Modifier
            .height(itemHeight.dp)
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = currentDigit.toString(),
            style = style,
            color = color,
            modifier = Modifier.offset(y = offsetY.value.dp)
        )
    }
}