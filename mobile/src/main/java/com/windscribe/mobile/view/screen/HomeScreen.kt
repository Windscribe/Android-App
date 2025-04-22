package com.windscribe.mobile.view.screen

import NetworkNameSheet
import ServerListScreen
import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.windscribe.mobile.R
import com.windscribe.mobile.connectionsettings.ConnectionSettingsActivity
import com.windscribe.mobile.mainmenu.MainMenuActivity
import com.windscribe.mobile.upgradeactivity.UpgradeActivity
import com.windscribe.mobile.view.AppStartActivity
import com.windscribe.mobile.view.LocalNavController
import com.windscribe.mobile.view.NavigationStack
import com.windscribe.mobile.view.theme.AppColors
import com.windscribe.mobile.view.theme.Dimen
import com.windscribe.mobile.view.theme.font12
import com.windscribe.mobile.view.theme.font16
import com.windscribe.mobile.view.theme.font26
import com.windscribe.mobile.view.theme.font9
import com.windscribe.mobile.view.ui.AppConnectButton
import com.windscribe.mobile.view.ui.LocationImage
import com.windscribe.mobile.viewmodel.ConfigViewmodel
import com.windscribe.mobile.viewmodel.ConnectionUIState
import com.windscribe.mobile.viewmodel.ConnectionViewmodel
import com.windscribe.mobile.viewmodel.HomeGoto
import com.windscribe.mobile.viewmodel.LocationInfoState
import com.windscribe.mobile.viewmodel.ServerViewModel
import com.windscribe.mobile.viewmodel.ToastMessage
import com.windscribe.vpn.backend.Util
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    serverViewModel: ServerViewModel,
    connectionViewmodel: ConnectionViewmodel,
    configViewmodel: ConfigViewmodel
) {
    HandleToast(connectionViewmodel)
    HandleGoto(connectionViewmodel)
    CompactUI(serverViewModel, connectionViewmodel, configViewmodel)
}

@Composable
fun HandleGoto(connectionViewmodel: ConnectionViewmodel) {
    val goto by connectionViewmodel.goto.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(goto) {
        when (goto) {
            HomeGoto.Banned -> {

            }

            is HomeGoto.Expired, HomeGoto.Upgrade -> {
            context.startActivity(UpgradeActivity.getStartIntent(context))
            }
            HomeGoto.None -> {

            }
        }
        connectionViewmodel.clearGoTo()
    }
}

@Composable
fun HandleToast(connectionViewmodel: ConnectionViewmodel) {
    val context = LocalContext.current
    val toastMessage by connectionViewmodel.toastMessage.collectAsState()
    LaunchedEffect(toastMessage) {
        when (toastMessage) {
            is ToastMessage.Localized -> {
                Toast.makeText(
                    context,
                    (toastMessage as ToastMessage.Localized).message,
                    Toast.LENGTH_SHORT
                ).show()
                connectionViewmodel.clearToast()
            }

            is ToastMessage.Raw -> {
                Toast.makeText(
                    context,
                    (toastMessage as ToastMessage.Raw).message,
                    Toast.LENGTH_SHORT
                ).show()
                connectionViewmodel.clearToast()
            }

            else -> {}
        }
    }
}

@Composable
private fun Background(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.homeBackground)
            .navigationBarsPadding()
    ) {
        content()
    }
}

@Composable
private fun CompactUI(
    serverViewModel: ServerViewModel,
    connectionViewmodel: ConnectionViewmodel,
    configViewmodel: ConfigViewmodel
) {
    val searchState by serverViewModel.showSearchView.collectAsState()
    Background {
        ConnectedBackground(connectionViewmodel)
        Column {
            Spacer(modifier = Modifier.height(36.dp))
            LocationImage(connectionViewmodel)
            ServerListScreen(serverViewModel, connectionViewmodel, configViewmodel)
        }
        Column {
            Header(connectionViewmodel)
            ConnectionStatusSheet(connectionViewmodel)
            Spacer(modifier = Modifier.height(32.dp))
            LocationName(connectionViewmodel)
            Spacer(modifier = Modifier.height(8.dp))
            NetworkInfoSheet(connectionViewmodel)
            Spacer(modifier = Modifier.weight(1.0f))
        }
        IPContextMenu(connectionViewmodel)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 61.dp, end = 16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            AppConnectButton(connectionViewmodel)
        }
        if (searchState) {
            SearchServerList(serverViewModel, connectionViewmodel)
        }
    }
}

@SuppressLint("UnrememberedMutableInteractionSource")
@Composable
private fun ConnectionStatusSheet(connectionViewmodel: ConnectionViewmodel) {
    val state by connectionViewmodel.connectionUIState.collectAsState()
    val containerColor = when (state) {
        is ConnectionUIState.Connected -> AppColors.connectedColor
        else -> AppColors.white
    }
    Row(
        modifier = Modifier.padding(start = Dimen.dp12, top = Dimen.dp16),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ConnectionStatus(state)
        val activity = LocalContext.current as AppStartActivity
        val antiCensorshipProtocolInteractionSource = remember { MutableInteractionSource() }
        val antiCensorshipEnabled by connectionViewmodel.isAntiCensorshipEnabled.collectAsState()
        if (antiCensorshipEnabled) {
            Spacer(modifier = Modifier.width(4.dp))
            Image(
                painter = painterResource(if (state is ConnectionUIState.Connected) R.drawable.ic_anti_censorship_enabled else R.drawable.ic_anti_censorship_disabled),
                contentDescription = null,
                modifier = Modifier
                    .size(Dimen.dp24)
                    .clickable(
                        antiCensorshipProtocolInteractionSource,
                        indication = rememberRipple(bounded = false, color = AppColors.white)
                    ) {
                        activity.startActivity(ConnectionSettingsActivity.getStartIntent(activity))
                    },
                contentScale = ContentScale.Inside,
                colorFilter = ColorFilter.tint(containerColor)
            )
            Spacer(modifier = Modifier.width(4.dp))
        } else {
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = "${Util.getProtocolLabel(state.protocolInfo?.protocol ?: "")}  ${state.protocolInfo?.port}",
            style = font12.copy(fontWeight = FontWeight.Bold),
            color = containerColor
        )
        Spacer(modifier = Modifier.width(4.dp))
        val preferredProtocolEnabled by connectionViewmodel.isPreferredProtocolEnabled.collectAsState()
        if (preferredProtocolEnabled) {
            Image(
                painter = painterResource(if (state is ConnectionUIState.Connected) R.drawable.ic_preferred_protocol_status_enabled else R.drawable.ic_preferred_protocol_status_disabled),
                contentDescription = null,
                modifier = Modifier
                    .size(Dimen.dp24),
                contentScale = ContentScale.Inside,
                colorFilter = ColorFilter.tint(containerColor)
            )
        }
        val changeProtocolInteractionSource = remember { MutableInteractionSource() }
        Image(
            painter = painterResource(R.drawable.arrowright),
            contentDescription = null,
            modifier = Modifier
                .size(Dimen.dp24)
                .clickable(
                    changeProtocolInteractionSource,
                    indication = rememberRipple(bounded = false, color = AppColors.white)
                ) {
                    connectionViewmodel.onProtocolChangeClick()
                },
            contentScale = ContentScale.None,
            colorFilter = ColorFilter.tint(containerColor)
        )
    }
}


@Composable
private fun LocationName(connectionViewmodel: ConnectionViewmodel) {
    val state by connectionViewmodel.connectionUIState.collectAsState()
    val locationInfo = (state.locationInfo as? LocationInfoState.Success)?.locationInfo
    val nodeName = locationInfo?.nodeName ?: ""
    val nickname = locationInfo?.nickName ?: ""
    Row {
        Text(
            text = nodeName,
            style = font26,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp)
        )
        Text(
            text = " $nickname",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = font26.copy(fontWeight = FontWeight.Normal),
            modifier = Modifier.padding(end = 12.dp)
        )
    }
}

@SuppressLint("UnrememberedMutableInteractionSource")
@Composable
private fun NetworkInfoSheet(connectionViewmodel: ConnectionViewmodel) {
    val ip by connectionViewmodel.ipState.collectAsState()
    val showContextMenu by connectionViewmodel.ipContextMenuState.collectAsState()
    val hideIp = remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        NetworkNameSheet(connectionViewmodel)
        Row(
            modifier = Modifier
                .weight(1.0f)
                .padding(start = Dimen.dp12, end = Dimen.dp8),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!showContextMenu.first) {
                Text(
                    text = ip,
                    style = font16.copy(fontWeight = FontWeight.Medium),
                    color = AppColors.white70, modifier = Modifier
                        .clickable {
                            hideIp.value = !hideIp.value
                        }
                        .graphicsLayer {
                            renderEffect = if (hideIp.value) {
                                BlurEffect(15f, 15f)
                            } else {
                                null
                            }
                        }
                )
            }
            val interactionSource = MutableInteractionSource()
            Image(
                painter = painterResource(R.drawable.ic_context),
                contentDescription = null,
                modifier = Modifier
                    .size(Dimen.dp24)
                    .onGloballyPositioned { layoutCoordinates ->
                        connectionViewmodel.onIpContextMenuPosition(layoutCoordinates.boundsInWindow().topLeft)
                    }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = rememberRipple(bounded = false, color = AppColors.white)
                    ) {
                        connectionViewmodel.setContextMenuState(true)
                    }
            )
        }
    }
}

@Composable
private fun ConnectionStatus(connectionUIState: ConnectionUIState) {
    val containerColor =
        if (connectionUIState is ConnectionUIState.Connected) AppColors.connectedColor else AppColors.white
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = containerColor.copy(alpha = 0.10f),
                shape = RoundedCornerShape(size = 34.dp)
            )
            .width(39.dp)
            .height(21.dp)
            .background(
                color = containerColor.copy(alpha = 0.10f),
                shape = RoundedCornerShape(size = 34.dp)
            )
            .padding(start = 8.dp, top = 2.dp, end = 8.dp, bottom = 3.dp)
    ) {
        when (connectionUIState) {
            is ConnectionUIState.Connected -> Text(
                text = "ON",
                style = font12.copy(fontWeight = FontWeight.SemiBold),
                color = containerColor,
                modifier = Modifier.align(Alignment.Center)
            )

            is ConnectionUIState.Connecting -> Dots(modifier = Modifier.align(Alignment.Center))
            else -> Text(
                text = "OFF",
                style = font12.copy(fontWeight = FontWeight.SemiBold),
                color = containerColor,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun IPContextMenu(connectionViewModel: ConnectionViewmodel) {
    val ipContextMenuState by connectionViewModel.ipContextMenuState.collectAsState()
    val width = 80.dp

    AnimatedVisibility(
        visible = ipContextMenuState.first,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Row(
            modifier = Modifier
                .offset {
                    IntOffset(
                        ipContextMenuState.second.x.roundToInt() - width.roundToPx(),
                        ipContextMenuState.second.y.roundToInt()
                    )
                }
                .fillMaxWidth()
        ) {
            IPMenuItem(
                icon = R.drawable.ic_fav,
                contentDescription = "Mark as Favourite",
                onClick = {
                    connectionViewModel.onFavouriteIpClick()
                }
            )
            IPMenuItem(
                icon = R.drawable.refresh,
                contentDescription = "Refresh IP",
                onClick = {
                    connectionViewModel.onRotateIpClick()
                }
            )
            IPMenuItem(
                icon = R.drawable.ic_search_location_close,
                contentDescription = "Close Menu",
                onClick = { connectionViewModel.setContextMenuState(false) }
            )
        }
    }
}

@Composable
private fun IPMenuItem(icon: Int, contentDescription: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Image(
        painter = painterResource(id = icon),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(AppColors.white),
        modifier = Modifier
            .size(24.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = false, color = AppColors.white),
                onClick = onClick
            )
    )
    Spacer(modifier = Modifier.width(16.dp))
}

@Composable
private fun Dots(modifier: Modifier) {
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(600)
            currentIndex = (currentIndex + 1) % 3
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
    ) {
        repeat(3) { index ->
            val alpha = animateFloatAsState(
                targetValue = if (index == currentIndex) 1f else 0.4f,
                animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                label = "dotAlpha"
            )

            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = alpha.value))
            )
        }
    }
}

@Composable
private fun ConnectedBackground(connectionViewmodel: ConnectionViewmodel) {
    val state by connectionViewmodel.connectionUIState.collectAsState()
    if (state is ConnectionUIState.Connected) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((273 + 36).dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            AppColors.connectedBlue,
                            AppColors.homeBackground
                        )
                    )
                )
        )
    }

}

@Composable
private fun Header(connectionViewmodel: ConnectionViewmodel) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .height(100.dp)
                .fillMaxWidth()
                .clipToBounds()
        ) {

            Box(
                modifier = Modifier
                    .height(100.dp)
                    .weight(1.0f)
                    .zIndex(0f)
                    .background(AppColors.homeHeaderBackground)
                    .drawBehind {
                        val strokeWidth = 1.dp.toPx() // Stroke thickness
                        drawLine(
                            color = Color.White.copy(alpha = 0.10f), // Stroke color
                            start = Offset(0f, size.height - strokeWidth / 2),
                            end = Offset(size.width, size.height - strokeWidth / 2),
                            strokeWidth = strokeWidth
                        )
                    }
            )

            // Second Image (Right, fixed width and overlapping)
            Box(
                modifier = Modifier
                    .height(100.dp)
                    .width(163.dp)
                    .zIndex(1f)
                    .clip(RectangleShape)
            ) {
                Image(
                    painter = painterResource(R.drawable.header_right),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart) // Ensures it starts from the bottom left
                .padding(start = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_ham_button),
                contentDescription = null,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(
                        bounded = false,
                        radius = 16.dp,
                        color = AppColors.white
                    )
                ) {
                    context.startActivity(MainMenuActivity.getStartIntent(context))
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Image(
                painter = painterResource(R.drawable.logo_home),
                contentDescription = null,
                modifier = Modifier
                    .height(18.dp)
                    .clickable {
                        navController.navigate(Screen.Newsfeed.route)
                    },
                contentScale = ContentScale.FillHeight,
            )
            val newsfeedCount by connectionViewmodel.newFeedCount.collectAsState()
            if (newsfeedCount > 0) {
                Box(
                    modifier = Modifier
                        .offset(y = ((-12).dp))
                        .size(14.dp)
                        .background(
                            color = AppColors.neonGreen,
                            shape = CircleShape
                        )
                ) {
                    Text(
                        "$newsfeedCount",
                        style = font9.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        ),
                        color = AppColors.homeBackground,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
@Preview(showSystemUi = true)
@PreviewScreenSizes
private fun HomeScreenPreview() {
    NavigationStack(Screen.Home)
}
