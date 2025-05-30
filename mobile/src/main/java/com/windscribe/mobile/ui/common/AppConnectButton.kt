package com.windscribe.mobile.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.connection.ConnectionUIState
import com.windscribe.mobile.ui.connection.ConnectionViewmodel

@Composable
fun AppConnectButton(connectionViewmodel: ConnectionViewmodel) {
    val rotation by rememberInfiniteTransition(label = "rotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "ringRotation"
    )
    val state by connectionViewmodel.connectionUIState.collectAsState()
    Box(modifier = Modifier.size(95.dp).clickable {
        connectionViewmodel.onConnectButtonClick()
    }) {
        when (state) {
            is ConnectionUIState.Connecting -> {
                Image(
                    painter = painterResource(R.drawable.ic_on_button),
                    contentDescription = null,
                    modifier = Modifier
                        .height(80.dp)
                        .width(80.dp)
                        .align(Alignment.Center),
                    contentScale = ContentScale.FillHeight
                )
                Image(
                    painter = painterResource(R.drawable.ic_connecting_ring),
                    contentDescription = null,
                    modifier = Modifier
                        .height(95.dp)
                        .width(95.dp)
                        .align(Alignment.Center)
                        .graphicsLayer { rotationZ = rotation }, // Rotate continuously
                    contentScale = ContentScale.FillHeight,
                    colorFilter = ColorFilter.tint(color = AppColors.white40)
                )
            }

            is ConnectionUIState.Connected -> {
                Image(
                    painter = painterResource(R.drawable.ic_on_button),
                    contentDescription = null,
                    modifier = Modifier
                        .height(80.dp)
                        .width(80.dp)
                        .align(Alignment.Center),
                    contentScale = ContentScale.FillHeight
                )
                Image(
                    painter = painterResource(R.drawable.ic_connected_ring),
                    contentDescription = null,
                    modifier = Modifier
                        .height(95.dp)
                        .width(95.dp)
                        .align(Alignment.Center),
                    contentScale = ContentScale.FillHeight,
                    colorFilter = ColorFilter.tint(color = AppColors.connectedColor)
                )
            }

            else -> {
                Image(
                    painter = painterResource(R.drawable.ic_off_button),
                    contentDescription = null,
                    modifier = Modifier
                        .height(80.dp)
                        .width(80.dp)
                        .align(Alignment.Center),
                    contentScale = ContentScale.FillHeight
                )
            }
        }
    }
}