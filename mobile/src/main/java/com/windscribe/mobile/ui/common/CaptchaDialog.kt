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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AppColors.charcoalBlue,
            modifier = Modifier.padding(16.dp),
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
    val configuration = LocalConfiguration.current

    // Calculate available width: screen width - total padding (16dp + 24dp on each side = 80dp total)
    val totalPaddingDp = 80.dp // 16dp (dialog) + 24dp (content) on each side
    val screenWidthDp = configuration.screenWidthDp.dp
    val availableWidthDp = screenWidthDp - totalPaddingDp
    val availableWidthPx = with(density) { availableWidthDp.toPx() }
    
    // Calculate image sizing
    val originalWidth = captchaBackground.width.toFloat()
    val originalHeight = captchaBackground.height.toFloat()
    val aspectRatio = originalWidth / originalHeight
    
    val (finalWidth, finalHeight, scaleFactor) = if (originalWidth > availableWidthPx) {
        // Image is larger than available space, resize to fit
        val newWidth = availableWidthPx
        val newHeight = newWidth / aspectRatio
        val scale = newWidth / originalWidth
        Triple(newWidth.toInt(), newHeight.toInt(), scale)
    } else {
        // Image fits, use original size
        Triple(originalWidth.toInt(), originalHeight.toInt(), 1f)
    }
    

    // Initialize slider position with scaling applied
    val sliderPositionX = remember { mutableFloatStateOf(0f) }
    val sliderPositionY = remember { mutableFloatStateOf(captcha.top.toFloat() * scaleFactor) }
    val initialY = remember { mutableFloatStateOf(captcha.top.toFloat()) }
    val dragHistory = remember { mutableStateListOf<Pair<Float, Float>>() }

    val backgroundSize = remember { mutableStateOf(IntSize(finalWidth, finalHeight)) }

    val backgroundBitmap = captchaBackground.asImageBitmap()
    val sliderBitmap = slider.asImageBitmap()
    val scaledSliderWidth = (slider.width * scaleFactor).toInt()
    val scaledSliderHeight = (slider.height * scaleFactor).toInt()
    
    fun submit() {
        // Convert scaled position back to original image coordinates for API
        val originalXOffset = sliderPositionX.floatValue / scaleFactor
        val xPositions = dragHistory.map { it.first / scaleFactor }
        val yPositions = dragHistory.map { it.second / scaleFactor }
        val trail = mapOf("x" to xPositions, "y" to yPositions)
        onSolutionSubmit(originalXOffset, trail)
    }

    var dragJob: Job? = remember { null }
    val coroutineScope = remember { CoroutineScope(Dispatchers.Main) }
    val shape = RoundedCornerShape(8.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 16.dp)
    ) {
        Text(
            stringResource(com.windscribe.vpn.R.string.complete_puzzle_to_continue),
            color = Color.White,
            style = font18.copy(fontWeight = FontWeight.Medium)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Box {
            Box(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = AppColors.white.copy(alpha = 0.05f),
                        shape = shape
                    )
                    .clip(shape)
                    .background(Color.Transparent)
                    .padding(1.dp)
            ) {
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

            // Draggable slider
            Box(
                modifier = with(density) {
                    Modifier
                        .offset(x = sliderPositionX.floatValue.toDp(), y = sliderPositionY.floatValue.toDp())
                        .size(scaledSliderWidth.toDp(), scaledSliderHeight.toDp())
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val newX = (sliderPositionX.floatValue + dragAmount.x).coerceIn(
                                        0f,
                                        backgroundSize.value.width - scaledSliderWidth.toFloat()
                                    )
                                    val newY = (sliderPositionY.floatValue + dragAmount.y).coerceIn(
                                        0f,
                                        backgroundSize.value.height - scaledSliderHeight.toFloat()
                                    )
                                    dragHistory.add(Pair(newX, newY - (initialY.floatValue * scaleFactor)))
                                    if (dragHistory.size > 50) {
                                        dragHistory.removeAt(0)
                                    }
                                    sliderPositionX.floatValue = newX
                                    sliderPositionY.floatValue = newY
                                    dragJob?.cancel()
                                },
                                onDragEnd = {
                                    dragJob?.cancel()
                                    dragJob = coroutineScope.launch {
                                        delay(700)
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
        Spacer(modifier = Modifier.height(24.dp))
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
                style = font12.copy(fontWeight = FontWeight.Normal),
                textAlign = TextAlign.Center,
                color = AppColors.white.copy(alpha = 0.50f),
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(stringResource(id = com.windscribe.vpn.R.string.cancel), onClick = {
            onCancel()
        })
    }
}