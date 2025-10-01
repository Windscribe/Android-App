package com.windscribe.mobile.ui.home

import NetworkNameSheet
import ServerListScreen
import android.annotation.SuppressLint
import android.os.Build
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.common.AnimatedIPAddress
import com.windscribe.mobile.ui.common.AppConnectButton
import com.windscribe.mobile.ui.common.LocationImage
import com.windscribe.mobile.ui.common.fitsInOneLine
import com.windscribe.mobile.ui.connection.ConnectionUIState
import com.windscribe.mobile.ui.connection.ConnectionViewmodel
import com.windscribe.mobile.ui.connection.LocationInfoState
import com.windscribe.mobile.ui.connection.ToastMessage
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.helper.getHeaderHeight
import com.windscribe.mobile.ui.helper.hapticClickable
import com.windscribe.mobile.ui.model.AccountStatusDialogData
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.mobile.ui.serverlist.ConfigViewmodel
import com.windscribe.mobile.ui.serverlist.SearchServerList
import com.windscribe.mobile.ui.serverlist.ServerViewModel
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.font26
import com.windscribe.mobile.ui.theme.font9
import com.windscribe.mobile.upgradeactivity.UpgradeActivity
import com.windscribe.vpn.backend.Util
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    serverViewModel: ServerViewModel?,
    connectionViewmodel: ConnectionViewmodel?,
    configViewmodel: ConfigViewmodel?,
    homeViewmodel: HomeViewmodel?
) {
    if (serverViewModel == null || connectionViewmodel == null || configViewmodel == null || homeViewmodel == null) {
        return
    }
    HandleToast(connectionViewmodel)
    HandleGoto(connectionViewmodel, homeViewmodel)
    CompactUI(serverViewModel, connectionViewmodel, configViewmodel, homeViewmodel)
}

@Composable
fun HandleGoto(connectionViewmodel: ConnectionViewmodel, homeViewmodel: HomeViewmodel) {
    val connectionGoto by connectionViewmodel.goto.collectAsState(initial = null)
    val homeGoto by homeViewmodel.goto.collectAsState(initial = null)
    HandleGotoAction(goto = connectionGoto, homeViewmodel, connectionViewmodel)
    HandleGotoAction(goto = homeGoto, homeViewmodel, connectionViewmodel)
}

@Composable
private fun HandleGotoAction(
    goto: HomeGoto?,
    homeViewmodel: HomeViewmodel,
    connectionViewmodel: ConnectionViewmodel
) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    var didNavigate = false
    when (goto) {
        HomeGoto.Banned -> {
            val bannedData = AccountStatusDialogData(
                title = stringResource(com.windscribe.vpn.R.string.you_ve_been_banned),
                icon = R.drawable.garry_account_ban,
                description = stringResource(com.windscribe.vpn.R.string.you_ve_violated_our_terms),
                showSecondaryButton = true,
                secondaryText = stringResource(com.windscribe.vpn.R.string.close),
                showPrimaryButton = false,
                primaryText = ""
            )
            navigateWithData(navController, Screen.AccountStatus.route, bannedData)
            didNavigate = true
        }

        HomeGoto.Downgraded -> {
            val downgradedData = AccountStatusDialogData(
                title = stringResource(com.windscribe.vpn.R.string.you_r_pro_plan_expired),
                icon = R.drawable.garry_downgraded,
                description = stringResource(com.windscribe.vpn.R.string.you_ve_been_downgraded_to_free_for_now),
                showSecondaryButton = true,
                secondaryText = stringResource(com.windscribe.vpn.R.string.back),
                showPrimaryButton = true,
                primaryText = stringResource(com.windscribe.vpn.R.string.renew_plan)
            )
            navigateWithData(navController, Screen.AccountStatus.route, downgradedData)
            didNavigate = true
        }

        is HomeGoto.Expired -> {
            val expireData = AccountStatusDialogData(
                title = stringResource(com.windscribe.vpn.R.string.you_re_out_of_data),
                icon = R.drawable.garry_account_no_data,
                description = stringResource(
                    com.windscribe.vpn.R.string.upgrade_to_stay_protected,
                    goto.date
                ),
                showSecondaryButton = true,
                secondaryText = stringResource(com.windscribe.vpn.R.string.back),
                showPrimaryButton = true,
                primaryText = stringResource(com.windscribe.vpn.R.string.upgrade_case_normal)
            )
            navigateWithData(navController, Screen.AccountStatus.route, expireData)
            didNavigate = true
        }

        is HomeGoto.EditCustomConfig -> {
            navController.currentBackStackEntry?.savedStateHandle?.set("config_id", goto.id)
            navController.currentBackStackEntry?.savedStateHandle?.set("connect", goto.connect)
            navController.navigate(Screen.EditCustomConfig.route)
            didNavigate = true
        }

        is HomeGoto.Upgrade -> {
            context.startActivity(UpgradeActivity.getStartIntent(context))
            didNavigate = true
        }

        HomeGoto.PowerWhitelist -> {
            navController.navigate(Screen.PowerWhitelist.route)
            didNavigate = true
        }

        HomeGoto.ShareAppLink -> {
            navController.navigate(Screen.ShareLink.route)
            didNavigate = true
        }

        HomeGoto.LocationMaintenance -> {
            navController.navigate(Screen.LocationUnderMaintenance.route)
            didNavigate = true
        }

        HomeGoto.MainMenu -> {
            navController.navigate(Screen.MainMenu.route)
            didNavigate = true
        }

        HomeGoto.None -> {}
        null -> {}
    }
    if (didNavigate) {
        connectionViewmodel.onGoToHandled()
        homeViewmodel.onGoToHandled()
    }
}

private fun navigateWithData(
    navController: NavController,
    route: String,
    data: AccountStatusDialogData
) {
    navController.currentBackStackEntry?.savedStateHandle?.set("accountStatusDialogData", data)
    navController.navigate(route)
}

@Composable
fun HandleToast(connectionViewmodel: ConnectionViewmodel?) {
    val context = LocalContext.current
    val toastMessage by connectionViewmodel?.toastMessage?.collectAsState() ?: return
    LaunchedEffect(toastMessage) {
        when (toastMessage) {
            is ToastMessage.Localized -> {
                Toast.makeText(
                    context,
                    (toastMessage as ToastMessage.Localized).message,
                    Toast.LENGTH_SHORT
                ).show()
                connectionViewmodel?.clearToast()
            }

            is ToastMessage.Raw -> {
                Toast.makeText(
                    context,
                    (toastMessage as ToastMessage.Raw).message,
                    Toast.LENGTH_SHORT
                ).show()
                connectionViewmodel?.clearToast()
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
            .background(AppColors.midnightNavy)
            .navigationBarsPadding()
    ) {
        content()
    }
}

@Composable
private fun CompactUI(
    serverViewModel: ServerViewModel,
    connectionViewmodel: ConnectionViewmodel,
    configViewmodel: ConfigViewmodel,
    homeViewmodel: HomeViewmodel
) {
    val searchState by serverViewModel.showSearchView.collectAsState()
    Background {
        ConnectedBackground(connectionViewmodel)
        Column {
            Spacer(modifier = Modifier.height(36.dp))
            LocationImage(connectionViewmodel, homeViewmodel)
            ServerListScreen(serverViewModel, connectionViewmodel, configViewmodel, homeViewmodel)
        }
        Column {
            Header(connectionViewmodel, homeViewmodel)
            ConnectionStatusSheet(connectionViewmodel)
            Spacer(modifier = Modifier.height(32.dp))
            LocationName(connectionViewmodel)
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
            SearchServerList(serverViewModel, connectionViewmodel, homeViewmodel)
        }
    }
}

@SuppressLint("UnrememberedMutableInteractionSource")
@Composable
private fun ConnectionStatusSheet(connectionViewmodel: ConnectionViewmodel) {
    val state by connectionViewmodel.connectionUIState.collectAsState()
    val isDecoyTrafficEnabled by connectionViewmodel.isDecoyTrafficEnabled.collectAsState()
    val containerColor = when (state) {
        is ConnectionUIState.Connected -> AppColors.mintGreen
        else -> AppColors.white
    }
    Row(
        modifier = Modifier.padding(start = 12.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ConnectionStatus(state)
        val antiCensorshipEnabled by connectionViewmodel.isAntiCensorshipEnabled.collectAsState()
        val showDecoyTraffic = state is ConnectionUIState.Connected && isDecoyTrafficEnabled
        
        // Add initial spacing
        Spacer(modifier = Modifier.width(if (antiCensorshipEnabled || showDecoyTraffic) 4.dp else 12.dp))
        
        if (antiCensorshipEnabled) {
            Image(
                painter = painterResource(if (state is ConnectionUIState.Connected) R.drawable.ic_anti_censorship_enabled else R.drawable.ic_anti_censorship_disabled),
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp),
                contentScale = ContentScale.Inside,
                colorFilter = ColorFilter.tint(containerColor)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        
        if (showDecoyTraffic) {
            Text(stringResource(com.windscribe.vpn.R.string.decoy), style = font12, color = AppColors.neonGreen)
            Spacer(modifier = Modifier.width(4.dp))
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
                    .size(24.dp),
                contentScale = ContentScale.Inside,
                colorFilter = ColorFilter.tint(containerColor)
            )
        }
        Image(
            painter = painterResource(R.drawable.arrow_right_small),
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .hapticClickable() {
                    connectionViewmodel.onProtocolChangeClick()
                },
            contentScale = ContentScale.None,
            colorFilter = ColorFilter.tint(containerColor.copy(alpha = 0.4f))
        )
    }
}


@Composable
private fun LocationName(connectionViewmodel: ConnectionViewmodel) {
    val state by connectionViewmodel.connectionUIState.collectAsState()
    val locationInfo = (state.locationInfo as? LocationInfoState.Success)?.locationInfo
    val nodeName = locationInfo?.nodeName ?: ""
    val nickname = locationInfo?.nickName ?: ""
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val maxWidthPx = with(density) { screenWidthDp.dp.toPx() }
    val isSingleLine = fitsInOneLine("$nodeName $nickname", 26.0f, maxWidthPx, density)
    connectionViewmodel.setIsSingleLineLocationName(isSingleLine)
    if (isSingleLine) {
        Row {
            Text(
                text = nodeName,
                style = font26.copy(textAlign = TextAlign.Start),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 12.dp)
            )
            Text(
                text = " $nickname",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = font26.copy(fontWeight = FontWeight.Normal, textAlign = TextAlign.Start),
                modifier = Modifier.padding(end = 12.dp)
            )
        }
    } else {
        Column {
            Text(
                text = nodeName,
                style = font26.copy(textAlign = TextAlign.Start),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp)
            )
            Text(
                text = nickname,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = font26.copy(fontWeight = FontWeight.Normal, textAlign = TextAlign.Start),
                modifier = Modifier.padding(start = 12.dp, end = 12.dp)
            )
        }
    }
}

@SuppressLint("UnrememberedMutableInteractionSource")
@Composable
internal fun BoxScope.NetworkInfoSheet(
    connectionViewmodel: ConnectionViewmodel,
    homeViewmodel: HomeViewmodel
) {
    val showContextMenu by connectionViewmodel.ipContextMenuState.collectAsState()
    val hideIp by homeViewmodel.hideIp.collectAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .offset(y = (-66).dp)
            .padding(end = 12.dp)
    ) {
        NetworkNameSheet(connectionViewmodel, homeViewmodel)
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!showContextMenu.first) {
                Box(
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    homeViewmodel.onHideIpClick()
                                }
                            )
                        }
                ) {
                    AnimatedIPAddress(
                        connectionViewmodel = connectionViewmodel,
                        style = font16,
                        color = AppColors.white
                    )

                    // Overlay box for Android 10 and below - only covers the text
                    if (hideIp && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    color = AppColors.midnightNavy.copy(alpha = 1.0f),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }
//            Image(
//                painter = painterResource(R.drawable.ic_context),
//                contentDescription = null,
//                modifier = Modifier
//                    .size(24.dp)
//                    .onGloballyPositioned { layoutCoordinates ->
//                        connectionViewmodel.onIpContextMenuPosition(layoutCoordinates.boundsInWindow().topLeft)
//                    }
//                    .hapticClickable(hapticEnabled = isHapticEnabled) {
//                        connectionViewmodel.setContextMenuState(true)
//                    }
//            )
        }
    }
}

@Composable
private fun ConnectionStatus(connectionUIState: ConnectionUIState) {
    val containerColor =
        if (connectionUIState is ConnectionUIState.Connected) AppColors.mintGreen else AppColors.white
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
                indication = ripple(bounded = false, color = AppColors.white),
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
private fun ConnectedBackground(connectionViewmodel: ConnectionViewmodel?) {
    val state by connectionViewmodel?.connectionUIState?.collectAsState() ?: return
    if (state is ConnectionUIState.Connected) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((273 + 36).dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            AppColors.primaryBlue,
                            AppColors.midnightNavy
                        )
                    )
                )
        )
    }

}


@Composable
private fun Header(connectionViewmodel: ConnectionViewmodel, homeViewmodel: HomeViewmodel) {
    val navController = LocalNavController.current
    val state by connectionViewmodel.connectionUIState.collectAsState()
    val leftHeaderAsset = if (state is ConnectionUIState.Connected) {
        R.drawable.header_left
    } else {
        R.drawable.header_left_deep
    }

    val rigtHeaderAsset = if (state is ConnectionUIState.Connected) {
        R.drawable.header_right
    } else {
        R.drawable.header_right_deep
    }
    val height = getHeaderHeight()
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .height(height)
                .fillMaxWidth()
                .clipToBounds()
        ) {

            // Left
            Box(
                modifier = Modifier
                    .height(height)
                    .weight(1.0f)
            ) {
                Image(
                    painter = painterResource(leftHeaderAsset),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
            }

            // Right
            Box(
                modifier = Modifier
                    .height(height)
                    .width(163.dp)
            ) {
                Image(
                    painter = painterResource(rigtHeaderAsset),
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
                modifier = Modifier.hapticClickable() {
                    homeViewmodel.onMainMenuClick()
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
                        color = AppColors.midnightNavy,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
@MultiDevicePreview
private fun HomeScreenPreview() {
    PreviewWithNav {
        HomeScreen(null, null, null, null)
    }
}
