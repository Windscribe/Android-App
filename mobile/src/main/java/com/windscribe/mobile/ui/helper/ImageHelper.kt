package com.windscribe.mobile.ui.helper

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min

data class ImageDimensions(val width: Dp, val height: Dp)

@Composable
fun calculateImageDimensions(): ImageDimensions {
    val config = LocalConfiguration.current
    val screenWidthDp = config.screenWidthDp
    val screenHeightDp = config.screenHeightDp

    val screenWidth = screenWidthDp.dp
    val screenHeight = screenHeightDp.dp

    val minHeight = 273.dp
    val maxHeight = screenHeight * 0.45f

    // Determine dynamic height
    val height = if (screenWidth < minHeight * 2) {
        minHeight
    } else {
        min(screenWidth / 2, maxHeight)
    }

    // Width is the smaller of screen width or 2 × height
    val width = minOf(screenWidth, height * 2)

    return ImageDimensions(width, height)
}