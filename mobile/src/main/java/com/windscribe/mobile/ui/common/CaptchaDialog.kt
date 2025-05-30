package com.windscribe.mobile.ui.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.auth.CaptchaRequest
import java.io.ByteArrayInputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
private fun decodeBase64ToBitmap(base64: String): Bitmap? {
    try {
        val decodedBytes = Base64.decode(base64)
        val stream = ByteArrayInputStream(decodedBytes)
        return BitmapFactory.decodeStream(stream)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

@Composable
fun CaptchaDebugDialog(
    captchaRequest: CaptchaRequest,
    onCancel: () -> Unit,
    onSolutionSubmit: (Float, Map<String, List<Float>>) -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 8.dp,
        ) {
            CaptchaDebugView(captchaRequest, onSolutionSubmit)
        }
    }
}

@Composable
fun CaptchaDebugView(
    captcha: CaptchaRequest,
    onSolutionSubmit: (Float, Map<String, List<Float>>) -> Unit
) {
    val captchaBackground = decodeBase64ToBitmap(captcha.background)
    val slider = decodeBase64ToBitmap(captcha.slider)

    if (captchaBackground == null || slider == null) {
        Text("Failed to decode captcha image")
        return
    }

    val density = LocalDensity.current
    val sliderPositionX = remember { mutableFloatStateOf(0f) }
    val sliderPositionY = remember { mutableFloatStateOf(captcha.top.toFloat()) }

    val initialY = remember { mutableStateOf(captcha.top.toFloat()) }
    val dragHistory = remember { mutableStateListOf<Pair<Float, Float>>() }

    val backgroundSize =
        remember { mutableStateOf(IntSize(captchaBackground.width, captchaBackground.height)) }

    val backgroundBitmap = captchaBackground.asImageBitmap()
    val sliderBitmap = slider.asImageBitmap()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Box {
            Box {
                Image(
                    bitmap = backgroundBitmap,
                    contentDescription = "Captcha Background",
                    modifier = with(density) {
                        Modifier
                            .height(backgroundSize.value.height.toDp())
                            .width(backgroundSize.value.width.toDp())
                    },
                    contentScale = ContentScale.FillBounds
                )
            }

            // Draggable slider in pixel space
            Box(
                modifier = with(density) {
                    Modifier
                        .offset(x = sliderPositionX.value.toDp(), y = sliderPositionY.value.toDp())
                        .size(slider.width.toDp(), slider.height.toDp())
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()

                                val newX = (sliderPositionX.value + dragAmount.x).coerceIn(
                                    0f,
                                    backgroundSize.value.width - slider.width.toFloat()
                                )
                                val newY = (sliderPositionY.value + dragAmount.y).coerceIn(
                                    0f,
                                    backgroundSize.value.height - slider.height.toFloat()
                                )

                                dragHistory.add(Pair(newX, newY - initialY.value))

                                sliderPositionX.value = newX
                                sliderPositionY.value = newY
                            }
                        }
                }
            ) {
                Image(
                    bitmap = sliderBitmap,
                    contentDescription = "Captcha Slider",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Button(
            onClick = {
                val finalXOffset = sliderPositionX.floatValue
                val xPositions = dragHistory.map { it.first }
                val yPositions = dragHistory.map { it.second }
                val trail = mapOf("x" to xPositions, "y" to yPositions)
                onSolutionSubmit(finalXOffset, trail)
            },
            colors = ButtonColors(
                containerColor = Color.Transparent,
                contentColor = AppColors.white50,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = Color.Transparent
            ),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Submit", style = font16, textAlign = TextAlign.Start)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(top = 8.dp)
                .padding(8.dp)
        ) {
            Text(
                "Pixel Position: (${sliderPositionX.floatValue.toInt()}, ${sliderPositionY.floatValue.toInt()})",
                color = Color.White,
                fontSize = 12.sp
            )
            Text(
                "Y Movement: ${(sliderPositionY.floatValue - initialY.value).toInt()} px",
                color = Color.White,
                fontSize = 12.sp
            )
            Text(
                "Max Size: ${backgroundSize.value.width}px x ${backgroundSize.value.height}px",
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}