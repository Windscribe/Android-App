package com.windscribe.mobile.ui.serverlist

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ripple
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
import com.windscribe.mobile.ui.AppStartActivity
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.common.AddButton
import com.windscribe.mobile.ui.common.AddButtonWithDetails
import com.windscribe.mobile.ui.common.LatencyIcon
import com.windscribe.mobile.ui.common.ServerNodeName
import com.windscribe.mobile.ui.common.openUrl
import com.windscribe.mobile.ui.connection.ConnectionViewmodel
import com.windscribe.mobile.ui.theme.expandedServerItemTextColor
import com.windscribe.mobile.ui.theme.serverListSecondaryColor
import com.windscribe.vpn.constants.NetworkKeyConstants
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import kotlin.collections.set


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaticIPServerList(
    viewModel: ServerViewModel,
    connectionViewModel: ConnectionViewmodel
) {
    val state by viewModel.staticListState.collectAsState()
    val activity = LocalContext.current as AppStartActivity
    val scrollState = rememberScrollState()
    val isRefreshing by viewModel.refreshState.collectAsState()


    when (state) {
        is ListState.Loading -> {
            ProgressIndicator()
        }

        is ListState.Error -> {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    "Error loading static IP list.",
                    style = font16,
                    color = MaterialTheme.colorScheme.serverListSecondaryColor
                )
            }
        }

        is ListState.Success -> {
            val list = (state as ListState.Success).data
            val lazyListState = rememberLazyListState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (list.isEmpty()) {
                    AddButtonWithDetails(
                        com.windscribe.vpn.R.string.add_static_ip,
                        com.windscribe.vpn.R.string.no_static_ip,
                        R.drawable.ic_location_static
                    ) {
                        activity.openUrl(NetworkKeyConstants.URL_ADD_STATIC_IP)
                    }
                } else {
                    val deviceName = list.first().staticItem.deviceName
                    Column(Modifier.fillMaxSize()) {
                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = {
                                viewModel.refresh(ServerListType.Static)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
                                item {
                                    Text(
                                        text = stringResource(com.windscribe.vpn.R.string.static_ip),
                                        style = font12,
                                        color = MaterialTheme.colorScheme.serverListSecondaryColor.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(start = 8.dp, top = 16.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                items(list, key = { it.id }) { item ->
                                    ListItemView(item, viewModel, connectionViewModel)
                                }
                            }
                        }
                        AddButton(com.windscribe.vpn.R.string.add_static_ip, deviceName) {
                            activity.openUrl(NetworkKeyConstants.URL_ADD_STATIC_IP)
                        }
                    }
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
                indication = ripple(bounded = true, color = MaterialTheme.colorScheme.serverListSecondaryColor)
            ) {
                connectionViewModel.onStaticIpClick(item.staticItem)
            }
            .padding(start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(staticIcon),
            contentDescription = "Static IP icon.",
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.expandedServerItemTextColor)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            ServerNodeName(item.staticItem.cityName, Modifier.padding(0.dp))
            Text(
                text = item.staticItem.staticIp,
                style = font12.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.expandedServerItemTextColor,
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
                .size(48.dp)
                .align(Alignment.Center), color = MaterialTheme.colorScheme.serverListSecondaryColor.copy(0.05f)
        )
    }
}