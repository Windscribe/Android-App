package com.windscribe.mobile.view.ui

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import coil.imageLoader
import coil.request.ImageRequest
import com.windscribe.mobile.R
import com.windscribe.mobile.view.theme.AppColors
import com.windscribe.mobile.view.theme.Dimen
import com.windscribe.mobile.viewmodel.ConnectionUIState
import com.windscribe.mobile.viewmodel.ConnectionViewmodel
import com.windscribe.mobile.viewmodel.LocationBackground
import com.windscribe.mobile.viewmodel.LocationInfoState

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LocationImage(connectionViewmodel: ConnectionViewmodel) {
    val connectionState by connectionViewmodel.connectionUIState.collectAsState()
    val locationBackground =
        (connectionState.locationInfo as? LocationInfoState.Success)?.locationInfo?.locationBackground
    val resource = locationBackground?.resource ?: com.windscribe.vpn.R.drawable.dummy_flag
    AnimatedContent(
        targetState = resource,
        transitionSpec = {
            slideInVertically(animationSpec = tween(durationMillis = 800)) { it } + fadeIn(
                animationSpec = tween(800)
            ) with
                    slideOutVertically(animationSpec = tween(durationMillis = 800)) { -it } + fadeOut(
                animationSpec = tween(800)
            )
        },
        label = "Flag Transition"
    ) { targetCountryCode ->
        Box(
            modifier = Modifier
                .height(Dimen.dp273)
                .alpha(if (locationBackground is LocationBackground.Wallpaper || locationBackground is LocationBackground.Custom) 1.0f else 0.30f)
                .fillMaxWidth()
                .graphicsLayer(alpha = 1.0f)
                .drawWithContent {
                    drawContent()
                    if (connectionState !is ConnectionUIState.Connected) {
                        drawRect(
                            brush = FlagMask,
                            blendMode = BlendMode.Modulate
                        )
                    }
                }
        ) {
            val context = LocalContext.current
            val aspectRatio by connectionViewmodel.aspectRatio.collectAsState()
            val imageData: Any = when (locationBackground) {
                is LocationBackground.Custom -> locationBackground.file
                else -> targetCountryCode.takeIf { it != 0 } ?: R.drawable.dummy_flag
            }
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(context)
                    .data(imageData)
                    .crossfade(true)
                    .build()
            )
            Box(modifier = Modifier.fillMaxSize()) {
                if (aspectRatio == 3) {
                    // TILE MODE (repeat pattern)
                    val imageBitmapState = produceState<ImageBitmap?>(initialValue = null) {
                        val imageLoader = context.imageLoader
                        val request = ImageRequest.Builder(context)
                            .data(imageData)
                            .allowHardware(false)
                            .build()

                        val result = imageLoader.execute(request)
                        val drawable = result.drawable
                        val bitmap = (drawable as? BitmapDrawable)?.bitmap
                        value = bitmap?.asImageBitmap()
                    }

                    imageBitmapState.value?.let { img ->
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val frameworkPaint = Paint().asFrameworkPaint().apply {
                                isAntiAlias = true
                                shader = android.graphics.BitmapShader(
                                    img.asAndroidBitmap(),
                                    android.graphics.Shader.TileMode.REPEAT,
                                    android.graphics.Shader.TileMode.REPEAT
                                )
                            }
                            drawContext.canvas.nativeCanvas.drawRect(
                                0f, 0f, size.width, size.height, frameworkPaint
                            )
                        }
                    }
                } else {
                    // NORMAL IMAGE MODES
                    val contentScale = when (aspectRatio) {
                        1 -> ContentScale.FillBounds // Fill
                        2 -> ContentScale.Fit        // Fit
                        else -> ContentScale.FillHeight // Default
                    }

                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = contentScale
                    )
                }
            }
            if (connectionState is ConnectionUIState.Connected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    AppColors.connectedGradient.copy(alpha = 1.0f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }
    }
}

private val FlagMask = Brush.verticalGradient(
    colorStops = arrayOf(
        0.0f to Color.White.copy(alpha = 0.0f),
        0.2f to Color.White.copy(alpha = 0.35f),
        0.3f to Color.White.copy(alpha = 0.5f),
        0.4f to Color.White.copy(alpha = 0.75f),
        0.5f to Color.White.copy(alpha = 1.0f),
    )
)