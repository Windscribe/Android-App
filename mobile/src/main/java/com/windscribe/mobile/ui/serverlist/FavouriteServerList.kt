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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.helper.HandleScrollHaptic
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.common.AddButtonWithDetails
import com.windscribe.mobile.ui.common.FavouriteIcon
import com.windscribe.mobile.ui.common.LatencyIcon
import com.windscribe.mobile.ui.common.ServerListIcon
import com.windscribe.mobile.ui.common.ServerNodeName
import com.windscribe.mobile.ui.common.TenGIcon
import com.windscribe.mobile.ui.common.healthColor
import com.windscribe.mobile.ui.connection.ConnectionViewmodel
import com.windscribe.mobile.ui.home.HomeViewmodel
import androidx.compose.foundation.lazy.items

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
                Text("Error loading favourite list.", style = font16, color = AppColors.white)
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
                            colorFilter = ColorFilter.tint(AppColors.white70)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.no_favourites), style = font16, color = AppColors.white70)
                    }
                }
                AddButtonWithDetails(null, R.string.no_favourites, R.drawable.ic_location_fav) { }
            } else {
                val isRefreshing by viewModel.refreshState.collectAsState()

                LaunchedEffect(pullToRefreshState.isRefreshing) {
                    if (pullToRefreshState.isRefreshing) {
                        viewModel.refresh(ServerListType.Fav)
                    }
                }

                LaunchedEffect(isRefreshing) {
                    if (!isRefreshing && pullToRefreshState.isRefreshing) {
                        pullToRefreshState.endRefresh()
                    }
                }
                Box {
                    Column(Modifier.fillMaxSize().nestedScroll(pullToRefreshState.nestedScrollConnection)) {
                        Text(
                            text = stringResource(R.string.favourite),
                            style = font12,
                            color = AppColors.white70,
                            modifier = Modifier.padding(start = 8.dp, top = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.fillMaxSize(), lazyListState) {
                            items(list, key = { it.id }) { item ->
                                ListItemView(item, viewModel, connectionViewmodel, homeViewmodel)
                            }
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
    item: FavouriteListItem,
    viewModel: ServerViewModel,
    connectionViewmodel: ConnectionViewmodel,
    homeViewmodel: HomeViewmodel?
) {
    val userState by homeViewmodel?.userState?.collectAsState() ?: return
    val color = colorResource(healthColor(item.city.health))
    val angle = (item.city.health / 100f) * 360f
    val latencyState by viewModel.latencyListState.collectAsState()
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
                indication = rememberRipple(bounded = true, color = AppColors.white)
            ) {
                connectionViewmodel.onCityClick(item.city)
            }.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ServerListIcon(item.city, userState, angle, color)
        Spacer(modifier = Modifier.width(8.dp))
        ServerNodeName("${item.city.nodeName} ${item.city.nickName}", Modifier.weight(1f))
        if (item.city.linkSpeed == "10000") {
            TenGIcon()
            Spacer(modifier = Modifier.width(12.dp))
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
                .align(Alignment.Center), color = AppColors.white
        )
    }
}