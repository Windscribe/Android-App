package com.windscribe.mobile.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
    animationDurationMs: Int = 1144
) {
    val ipAddress by connectionViewmodel.ipState.collectAsState()

    var lastValidIp by remember { mutableStateOf("--.--.--.--") }

    val shouldAnimate = remember(ipAddress) {
        val isValid = !ipAddress.contains("--")
        val animate = isValid && lastValidIp != ipAddress
        if (isValid) lastValidIp = ipAddress
        animate
    }

    if (ipAddress.contains("--")) {
        Text(text = ipAddress, style = style, color = color, modifier = modifier)
    } else {
        Row(modifier = modifier) {
            var digitIndex = 0
            ipAddress.forEachIndexed { index, char ->
                key("ip-$index") {
                    if (char.isDigit()) {
                        AnimatedDigit(
                            targetDigit = char.digitToInt(),
                            shouldAnimate = shouldAnimate,
                            digitIndex = digitIndex++,
                            style = style,
                            color = color
                        )
                    } else {
                        Text(text = char.toString(), style = style, color = color)
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
    color: Color
) {
    var currentDigit by remember { mutableIntStateOf(targetDigit) }
    val offsetY = remember { Animatable(0f) }
    val itemHeight = style.fontSize.value * 1.5f

    LaunchedEffect(targetDigit) {
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