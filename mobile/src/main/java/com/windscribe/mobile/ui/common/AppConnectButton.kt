package com.windscribe.mobile.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.connection.ConnectionUIState
import com.windscribe.mobile.ui.connection.ConnectionViewmodel
import com.windscribe.mobile.ui.connection.LocationBackground
import com.windscribe.mobile.ui.connection.LocationInfo
import com.windscribe.mobile.ui.connection.LocationInfoState
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.vpn.autoconnection.ProtocolConnectionStatus
import com.windscribe.vpn.autoconnection.ProtocolInformation

@Composable
fun AppConnectButton(connectionViewmodel: ConnectionViewmodel) {
    val shouldPlay by connectionViewmodel.shouldPlayHapticFeedback.collectAsState()
    val state by connectionViewmodel.connectionUIState.collectAsState()
    AppConnectButtonContent(
        shouldPlayHapticFeedback = shouldPlay,
        state = state,
        onHapticFeedbackHandled = { connectionViewmodel.onHapticFeedbackHandled() },
        onConnectButtonClick = { connectionViewmodel.onConnectButtonClick() },
    )
}

@Composable
fun AppConnectButtonContent(
    shouldPlayHapticFeedback: Boolean,
    state: ConnectionUIState,
    onHapticFeedbackHandled: () -> Unit,
    onConnectButtonClick: () -> Unit,
) {
    val rotation by rememberInfiniteTransition(label = "rotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "ringRotation",
    )
    val haptics = LocalHapticFeedback.current
    if (shouldPlayHapticFeedback) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onHapticFeedbackHandled()
    }
    // Idle is only the state before the first connection update arrives, and it draws the off
    // button, so it is described as disconnected to match what is on screen.
    // Paused and Blocked have no tunnel, so the button reads and behaves as off.
    val isOff =
        state is ConnectionUIState.Disconnected ||
            state is ConnectionUIState.Paused ||
            state is ConnectionUIState.Blocked ||
            state is ConnectionUIState.Idle
    val name = stringResource(com.windscribe.vpn.R.string.accessibility_vpn_connection)
    val actionLabel =
        stringResource(
            if (isOff) com.windscribe.vpn.R.string.connect else com.windscribe.vpn.R.string.disconnect,
        )
    val status =
        when (state) {
            is ConnectionUIState.Connecting -> stringResource(com.windscribe.vpn.R.string.accessibility_status_connecting)
            is ConnectionUIState.Connected ->
                if (state.connectedUsingSplitRouting) {
                    stringResource(com.windscribe.vpn.R.string.accessibility_status_connected_split_routing)
                } else {
                    stringResource(com.windscribe.vpn.R.string.accessibility_status_connected)
                }

            is ConnectionUIState.Paused -> stringResource(com.windscribe.vpn.R.string.accessibility_status_paused)
            is ConnectionUIState.Blocked -> stringResource(com.windscribe.vpn.R.string.accessibility_status_blocked)
            else -> stringResource(com.windscribe.vpn.R.string.accessibility_status_disconnected)
        }
    Box(
        modifier =
            Modifier
                .size(95.dp)
                .testTag("home_connect_button")
                .semantics(mergeDescendants = true) {
                    contentDescription = name
                    stateDescription = status
                    // The connection can finish while the user is reading elsewhere on the screen.
                    liveRegion = LiveRegionMode.Polite
                }.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    // Names the action rather than the button, so it reads "double tap to connect".
                    onClickLabel = actionLabel,
                    role = Role.Button,
                ) {
                    onConnectButtonClick()
                },
    ) {
        when (state) {
            is ConnectionUIState.Connecting -> {
                Image(
                    painter = painterResource(R.drawable.ic_on_button),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .height(80.dp)
                            .width(80.dp)
                            .align(Alignment.Center),
                    contentScale = ContentScale.FillHeight,
                )
                Image(
                    painter = painterResource(R.drawable.ic_connecting_ring),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .height(95.dp)
                            .width(95.dp)
                            .align(Alignment.Center)
                            .graphicsLayer { rotationZ = rotation },
                    // Rotate continuously
                    contentScale = ContentScale.FillHeight,
                    colorFilter = ColorFilter.tint(color = AppColors.white.copy(alpha = 0.40f)),
                )
            }

            is ConnectionUIState.Connected -> {
                Image(
                    painter = painterResource(R.drawable.ic_on_button),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .height(80.dp)
                            .width(80.dp)
                            .align(Alignment.Center),
                    contentScale = ContentScale.FillHeight,
                )
                Image(
                    painter =
                        painterResource(
                            if (state.connectedUsingSplitRouting) {
                                R.drawable.ic_connected_split_ring
                            } else {
                                R.drawable.ic_connected_ring
                            },
                        ),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .height(95.dp)
                            .width(95.dp)
                            .align(Alignment.Center),
                    contentScale = ContentScale.FillHeight,
                    colorFilter = ColorFilter.tint(color = AppColors.mintGreen),
                )
            }

            else -> {
                Image(
                    painter = painterResource(R.drawable.ic_off_button),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .height(80.dp)
                            .width(80.dp)
                            .align(Alignment.Center),
                    contentScale = ContentScale.FillHeight,
                )
            }
        }
    }
}

/**
 * Feeds the three connection states into the preview so the pane shows the connect button
 * disconnected (off), connecting (spinning ring) and connected (green ring) side by side.
 */
private class ConnectButtonStateProvider : PreviewParameterProvider<ConnectionUIState> {
    private val protocol =
        ProtocolInformation(
            protocol = "WireGuard",
            port = "443",
            description = "",
            type = ProtocolConnectionStatus.Connected,
        )
    private val location =
        LocationInfoState.Success(
            LocationInfo(
                country = "United States",
                nodeName = "New York",
                nickName = "Liberty",
                locationBackground = LocationBackground.Flag(R.drawable.dummy_flag),
            ),
        )

    override val values =
        sequenceOf(
            ConnectionUIState.Disconnected(protocol, location),
            ConnectionUIState.Connecting(protocol, location),
            ConnectionUIState.Connected(protocol, location, connectedUsingSplitRouting = false),
        )
}

@Composable
@MultiDevicePreview
private fun AppConnectButtonContentPreview(
    @PreviewParameter(ConnectButtonStateProvider::class) state: ConnectionUIState,
) {
    PreviewWithNav {
        AppConnectButtonContent(
            shouldPlayHapticFeedback = false,
            state = state,
            onHapticFeedbackHandled = {},
            onConnectButtonClick = {},
        )
    }
}
