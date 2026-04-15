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
import com.windscribe.mobile.ui.common.DataCenterFavouriteIcon
import com.windscribe.mobile.ui.common.DataCenterLatencyIcon
import com.windscribe.mobile.ui.common.DataCenterName
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
import com.windscribe.vpn.serverlist.entity.DatacenterStatusHelper
import kotlin.collections.set

private const val MIN_HEALTH_VALUE = 50

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
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        viewModel.refresh(ServerListType.Fav)
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(start = 8.dp), lazyListState) {
                        item {
                            Text(
                                text = stringResource(com.windscribe.vpn.R.string.favourite),
                                style = font12,
                                color = MaterialTheme.colorScheme.serverListSecondaryColor.copy(alpha = 0.70f),
                                modifier = Modifier.padding(start = 0.dp, top = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(list, key = { it.id }) { item ->
                            ListItemView(item, viewModel, connectionViewmodel, homeViewmodel)
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
    val health by viewModel.observeAverageHealth(item.city.id).collectAsState(initial = MIN_HEALTH_VALUE)
    val serverCount by viewModel.observeDatacenterServerCount(item.city.id).collectAsState(initial = 0)
    val latencyState by viewModel.latencyListState.collectAsState()
    val showLocationLoad by homeViewmodel.showLocationLoad.collectAsState()
    val latency by rememberUpdatedState(
        if (latencyState is ListState.Success) {
            (latencyState as ListState.Success).data.find { it.id == item.id }?.time ?: -1
        } else -1
    )
    val requiresPro = DatacenterStatusHelper.requiresPro(item.city, serverCount)
    val isAvailable = DatacenterStatusHelper.isAvailable(item.city, serverCount)
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
            }.padding(start = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LocationSplitBorderCircle(
            health = health,
            flagRes = FlagIconResource.getSmallFlag(item.countryCode),
            showProIcon = requiresPro,
            showLocationLoad = showLocationLoad
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            DataCenterName("${item.city.nodeName} ${item.city.nickName}", Modifier)
            Text(
                text = item.pinnedIp ?: "Random IP",
                style = font12.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.serverListSecondaryColor.copy(alpha = 0.60f)
            )
        }
        if (isAvailable) {
            DataCenterLatencyIcon(latency)
            Spacer(modifier = Modifier.width(12.dp))
        }
        DataCenterFavouriteIcon(true) {
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