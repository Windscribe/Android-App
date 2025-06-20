package com.windscribe.mobile.ui.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.windscribe.mobile.ui.auth.CaptchaRequest
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font18
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
            shape = RoundedCornerShape(16.dp),
            color = AppColors.charcoalBlue,
            border = BorderStroke(1.dp, AppColors.white.copy(alpha = 0.05f)),
            tonalElevation = 8.dp,
        ) {
            CaptchaDebugView(captchaRequest, onSolutionSubmit, onCancel)
        }
    }
}

@Composable
fun CaptchaDebugView(
    captcha: CaptchaRequest,
    onSolutionSubmit: (Float, Map<String, List<Float>>) -> Unit,
    onCancel: () -> Unit
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
    fun submit() {
        val finalXOffset = sliderPositionX.floatValue
        val xPositions = dragHistory.map { it.first }
        val yPositions = dragHistory.map { it.second }
        val trail = mapOf("x" to xPositions, "y" to yPositions)
        onSolutionSubmit(finalXOffset, trail)
    }

    var dragJob: Job? = remember { null }
    val coroutineScope = remember { CoroutineScope(Dispatchers.Main) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp)
    ) {
        Text(
            stringResource(com.windscribe.vpn.R.string.complete_puzzle_to_continue),
            color = Color.White,
            style = font18
        )
        Spacer(modifier = Modifier.height(24.dp))
        Box {
            Box {
                Image(
                    bitmap = backgroundBitmap,
                    contentDescription = "Captcha Background",
                    modifier = with(density) {
                        Modifier
                            .height(backgroundSize.value.height.toDp())
                            .width(backgroundSize.value.width.toDp())
                    }.background(color = Color.Transparent, shape = RoundedCornerShape(12.dp)),
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
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
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
                                    dragJob?.cancel()
                                },
                                onDragEnd = {
                                    dragJob?.cancel()
                                    dragJob = coroutineScope.launch {
                                        delay(100)
                                        submit()
                                    }
                                },
                                onDragStart = {
                                    dragJob?.cancel()
                                }
                            )
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
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = AppColors.deepBlue, shape = RoundedCornerShape(10.dp))
                .border(
                    1.dp,
                    AppColors.white.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
        ) {
            Text(
                stringResource(com.windscribe.vpn.R.string.slide_puzzle_piece_into_place),
                style = font12,
                textAlign = TextAlign.Center,
                color = AppColors.white.copy(alpha = 0.50f),
                modifier = Modifier.align(Alignment.Center)
            )
        }
        TextButton(stringResource(id = com.windscribe.vpn.R.string.cancel), onClick = {
            onCancel()
        })
    }
}