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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.common.AddButtonWithDetails
import com.windscribe.mobile.ui.common.FavouriteIcon
import com.windscribe.mobile.ui.common.LatencyIcon
import com.windscribe.mobile.ui.common.ServerListIcon
import com.windscribe.mobile.ui.common.ServerNodeName
import com.windscribe.mobile.ui.common.TenGIcon
import com.windscribe.mobile.ui.common.healthColor
import com.windscribe.mobile.ui.connection.ConnectionViewmodel
import com.windscribe.mobile.ui.helper.HandleScrollHaptic
import com.windscribe.mobile.ui.helper.miniumHealthStart
import com.windscribe.mobile.ui.home.HomeViewmodel
import com.windscribe.mobile.ui.home.UserState
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.serverListSecondaryColor
import com.windscribe.vpn.commonutils.FlagIconResource
import kotlin.collections.set

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouriteList(viewModel: ServerViewModel, connectionViewmodel: ConnectionViewmodel, homeViewmodel: HomeViewmodel, pullToRefreshState: PullToRefreshState = rememberPullToRefreshState()) {
    val state by viewModel.favouriteListState.collectAsState()
    val lazyListState = rememberLazyListState()
    HandleScrollHaptic(lazyListState, homeViewmodel)
    when (state) {
        is ListState.Loading -> ProgressIndicator()

        is ListState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error loading favourite list.", style = font16, color = MaterialTheme.colorScheme.serverListSecondaryColor)
            }
        }

        is ListState.Success -> {
            val list = (state as ListState.Success).data

            if (list.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(R.drawable.ic_location_fav),
                            contentDescription = "No Favourites",
                            modifier = Modifier.size(32.dp),
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.serverListSecondaryColor.copy(alpha = 0.70f))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(com.windscribe.vpn.R.string.no_favourites), style = font16, color = MaterialTheme.colorScheme.serverListSecondaryColor)
                    }
                }
                AddButtonWithDetails(null, com.windscribe.vpn.R.string.no_favourites, R.drawable.ic_location_fav) { }
            } else {
                val isRefreshing by viewModel.refreshState.collectAsState()
                Box {
                    Column(Modifier.fillMaxSize()) {
                        Text(
                            text = stringResource(com.windscribe.vpn.R.string.favourite),
                            style = font12,
                            color = MaterialTheme.colorScheme.serverListSecondaryColor.copy(alpha = 0.70f),
                            modifier = Modifier.padding(start = 8.dp, top = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = {
                                viewModel.refresh(ServerListType.Fav)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxSize().padding(start = 8.dp), lazyListState) {
                                items(list, key = { it.id }) { item ->
                                    ListItemView(item, viewModel, connectionViewmodel, homeViewmodel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListItemView(
    item: FavouriteListItem,
    viewModel: ServerViewModel,
    connectionViewmodel: ConnectionViewmodel,
    homeViewmodel: HomeViewmodel
) {
    val userState by homeViewmodel.userState.collectAsState()
    var health = item.city.health
    if (health < miniumHealthStart){
        health = miniumHealthStart
    }
    val color = colorResource(healthColor(health))
    val angle = (health / 100f) * 360f
    val latencyState by viewModel.latencyListState.collectAsState()
    val showLocationLoad by homeViewmodel.showLocationLoad.collectAsState()
    val latency by rememberUpdatedState(
        if (latencyState is ListState.Success) {
            (latencyState as ListState.Success).data.find { it.id == item.id }?.time ?: -1
        } else -1
    )
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(
                interactionSource,
                indication = ripple(bounded = true, color = MaterialTheme.colorScheme.serverListSecondaryColor)
            ) {
                connectionViewmodel.onCityClick(item.city, true)
            }.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SplitBorderCircle(
            angle,
            color,
            MaterialTheme.colorScheme.serverListSecondaryColor.copy(alpha = 0.20f),
            FlagIconResource.getSmallFlag(item.countryCode),
            userState !is UserState.Pro && item.city.pro == 1,
            showLocationLoad
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            ServerNodeName("${item.city.nodeName} ${item.city.nickName}", Modifier)
            Text(
                text = item.pinnedIp ?: "Random IP",
                style = font12.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.serverListSecondaryColor.copy(alpha = 0.60f)
            )
        }
        LatencyIcon(latency)
        Spacer(modifier = Modifier.width(12.dp))
        FavouriteIcon(true) {
            viewModel.deleteFavourite(item.id)
        }
    }
}

@Composable
private fun ProgressIndicator() {
    Box(modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center), color = MaterialTheme.colorScheme.serverListSecondaryColor
        )
    }
}