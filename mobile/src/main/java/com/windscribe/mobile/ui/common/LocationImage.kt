package com.windscribe.mobile.ui.common

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.connection.ConnectionUIState
import com.windscribe.mobile.ui.connection.ConnectionViewmodel
import com.windscribe.mobile.ui.connection.LocationBackground
import com.windscribe.mobile.ui.connection.LocationInfoState
import com.windscribe.mobile.ui.helper.calculateImageDimensions
import com.windscribe.mobile.ui.home.HomeViewmodel
import com.windscribe.mobile.ui.home.NetworkInfoSheet
import com.windscribe.mobile.ui.theme.AppColors

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LocationImage(connectionViewmodel: ConnectionViewmodel, homeViewmodel: HomeViewmodel) {
    val connectionState by connectionViewmodel.connectionUIState.collectAsState()
    val locationBackground =
        (connectionState.locationInfo as? LocationInfoState.Success)?.locationInfo?.locationBackground
    val resource = locationBackground?.resource ?: com.windscribe.vpn.R.drawable.dummy_flag
    val isSingleLineLocationName by connectionViewmodel.isSingleLineLocationName.collectAsState()
    val imageDimen = calculateImageDimensions(isSingleLineLocationName)
    Box {
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
                    .height(imageDimen.height)
                    .fillMaxSize()
                    .alpha(if (locationBackground is LocationBackground.Wallpaper || locationBackground is LocationBackground.Custom) 1.0f else 0.30f)
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
                if (locationBackground is LocationBackground.Custom) {
                    val imageData = locationBackground.file
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
                            SubcomposeAsyncImage(
                                model = imageData,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = contentScale,
                                loading = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Gray)
                                    )
                                },
                                error = {
                                    Image(
                                        painterResource(id = R.drawable.dummy_flag),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = contentScale
                                    )
                                }
                            )
                        }
                    }
                } else {
                    val imageDrawable =
                        targetCountryCode.takeIf { it != 0 } ?: R.drawable.dummy_flag
                    Image(
                        painter = painterResource(id = imageDrawable),
                        contentDescription = null,
                        modifier = Modifier
                            .width(imageDimen.width)
                            .height(imageDimen.height)
                            .align(Alignment.Center),
                        contentScale = ContentScale.Crop
                    )
                }
                if (connectionState is ConnectionUIState.Connected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        AppColors.darkBlueAccent.copy(alpha = 1.0f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }
        }
        NetworkInfoSheet(connectionViewmodel, homeViewmodel)
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