package com.windscribe.mobile.view.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.view.AppStartActivity
import com.windscribe.mobile.view.theme.AppColors
import com.windscribe.mobile.view.theme.Dimen
import com.windscribe.mobile.view.theme.font12
import com.windscribe.mobile.view.theme.font16
import com.windscribe.mobile.view.ui.AddButton
import com.windscribe.mobile.view.ui.AddButtonWithDetails
import com.windscribe.mobile.view.ui.LatencyIcon
import com.windscribe.mobile.view.ui.ServerNodeName
import com.windscribe.mobile.viewmodel.ConnectionViewmodel
import com.windscribe.mobile.viewmodel.ListState
import com.windscribe.mobile.viewmodel.ServerListType
import com.windscribe.mobile.viewmodel.ServerViewModel
import com.windscribe.mobile.viewmodel.StaticListItem
import com.windscribe.vpn.constants.NetworkKeyConstants
import com.windscribe.vpn.constants.NetworkKeyConstants.getWebsiteLink


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaticIPServerList(viewModel: ServerViewModel, connectionViewModel: ConnectionViewmodel, pullToRefreshState: PullToRefreshState = rememberPullToRefreshState()) {
    val state by viewModel.staticListState.collectAsState()
    val activity = LocalContext.current as AppStartActivity
    val scrollState = rememberScrollState()
    val isRefreshing by viewModel.refreshState.collectAsState()

    LaunchedEffect(pullToRefreshState.isRefreshing) {
        if (pullToRefreshState.isRefreshing) {
            viewModel.refresh(ServerListType.Static)
        }
    }

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing && pullToRefreshState.isRefreshing) {
            pullToRefreshState.endRefresh()
        }
    }

    when (state) {
        is ListState.Loading -> {
            ProgressIndicator()
        }

        is ListState.Error -> {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    "Error loading static ip list.",
                    style = font16,
                    color = AppColors.white
                )
            }
        }

        is ListState.Success -> {
            Box(modifier = Modifier.fillMaxSize()) {
                val list = (state as ListState.Success).data
                if (list.isEmpty()) {
                    AddButtonWithDetails(
                        R.string.add_static_ip,
                        R.string.no_static_ip,
                        R.drawable.ic_location_static
                    ) {
                        activity.openURLInBrowser(getWebsiteLink(NetworkKeyConstants.URL_ADD_STATIC_IP))
                    }
                } else {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .nestedScroll(pullToRefreshState.nestedScrollConnection)
                    ) {
                        Text(
                            text = stringResource(R.string.static_ip),
                            style = font12,
                            color = AppColors.white70,
                            modifier = Modifier.padding(start = 8.dp, top = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(modifier = Modifier.verticalScroll(scrollState)) {
                            list.forEach { item ->
                                ListItemView(item, viewModel, connectionViewModel)
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        val deviceName = list.first().staticItem.deviceName
                        AddButton(R.string.add_static_ip, deviceName) {
                            activity.openURLInBrowser(getWebsiteLink(NetworkKeyConstants.URL_ADD_STATIC_IP))
                        }
                    }
                    PullToRefreshContainer(
                        state = pullToRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter),
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ListItemView(
    item: StaticListItem,
    viewModel: ServerViewModel,
    connectionViewModel: ConnectionViewmodel
) {
    val latencyState by viewModel.latencyListState.collectAsState()
    val latency by rememberUpdatedState(
        if (latencyState is ListState.Success) {
            (latencyState as ListState.Success).data.find { it.id == item.id }?.time ?: -1
        } else -1
    )
    val staticIcon = if (item.staticItem.status == 0) {
        R.drawable.ic_under_construction
    } else {
        R.drawable.ic_location_static
    }
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(
                interactionSource,
                indication = rememberRipple(bounded = true, color = AppColors.white)
            ) {
                connectionViewModel.onStaticIpClick(item.staticItem)
            }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(staticIcon),
            contentDescription = "Static IP icon.",
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(AppColors.white)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            ServerNodeName(item.staticItem.cityName, Modifier.padding(0.dp))
            Text(
                text = item.staticItem.staticIp,
                style = font12.copy(fontWeight = FontWeight.Medium),
                color = AppColors.white,
                textAlign = TextAlign.Start
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        LatencyIcon(latency)
    }
}

@Composable
private fun ProgressIndicator() {
    Box(modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(Dimen.dp48)
                .align(Alignment.Center), color = AppColors.white
        )
    }
}