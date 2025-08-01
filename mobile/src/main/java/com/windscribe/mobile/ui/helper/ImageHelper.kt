package com.windscribe.mobile.ui.helper

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min

data class ImageDimensions(val width: Dp, val height: Dp)

@Composable
fun getStatusBarHeight(): Dp {
    val insets = WindowInsets.statusBars.asPaddingValues()
    return insets.calculateTopPadding()
}

@Composable
fun getHeaderHeight(): Dp {
    return getStatusBarHeight() + 60.dp
}

const val latencyArcStart = 270f
const val miniumHealthStart = 10
@Composable
fun calculateImageDimensions(isSingleLineLocationName: Boolean): ImageDimensions {
    val config = LocalConfiguration.current
    val screenWidthDp = config.screenWidthDp
    val screenHeightDp = config.screenHeightDp
    val screenWidth = screenWidthDp.dp
    val screenHeight = screenHeightDp.dp
    var minImageHeight = 233.dp
    if (!isSingleLineLocationName) {
        minImageHeight = 275.dp
    }
    val minHeight = minImageHeight + getStatusBarHeight()
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