package com.windscribe.mobile.ui.serverlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.windscribe.mobile.R
import com.windscribe.mobile.upgradeactivity.UpgradeActivity
import com.windscribe.mobile.ui.AppStartActivity
import com.windscribe.mobile.ui.helper.HandleScrollHaptic
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.font9
import com.windscribe.mobile.ui.common.FavouriteIcon
import com.windscribe.mobile.ui.common.LatencyIcon
import com.windscribe.mobile.ui.common.ServerListIcon
import com.windscribe.mobile.ui.common.ServerNodeName
import com.windscribe.mobile.ui.common.TenGIcon
import com.windscribe.mobile.ui.common.averageHealth
import com.windscribe.mobile.ui.common.healthColor
import com.windscribe.mobile.ui.connection.ConnectionViewmodel
import com.windscribe.mobile.ui.home.HomeViewmodel
import com.windscribe.mobile.ui.home.UserState
import com.windscribe.vpn.commonutils.FlagIconResource
import com.windscribe.vpn.serverlist.entity.City
import androidx.compose.foundation.lazy.items

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllServerList(
    viewModel: ServerViewModel,
    connectionViewModel: ConnectionViewmodel,
    homeViewmodel: HomeViewmodel,
    pullToRefreshState: PullToRefreshState = rememberPullToRefreshState()
) {
    val state by viewModel.serverListState.collectAsState()
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }
    val bestLocation by connectionViewModel.bestLocation.collectAsState()
    val isRefreshing by viewModel.refreshState.collectAsState()

    LaunchedEffect(pullToRefreshState.isRefreshing) {
        if (pullToRefreshState.isRefreshing) {
            viewModel.refresh(ServerListType.All)
        }
    }

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing && pullToRefreshState.isRefreshing) {
            pullToRefreshState.endRefresh()
        }
    }
    Box {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 0.dp)
                .nestedScroll(pullToRefreshState.nestedScrollConnection),
            verticalArrangement = Arrangement.Top

        ) {
            when (state) {
                is ListState.Loading -> {
                    ProgressIndicator()
                }

                is ListState.Error -> {
                    Text("Error loading server list")
                }

                is ListState.Success -> {
                    val lazyListState = rememberLazyListState()
                    HandleScrollHaptic(lazyListState, homeViewmodel)
                    LocationCount(viewModel)
                    val list = (state as ListState.Success).data
                    LazyColumn(state = lazyListState, modifier = Modifier.weight(1f)) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            bestLocation?.let { BestLocation(it, connectionViewModel) }
                        }
                        items(list, key = { it.id }) { item ->
                            ExpandableListItem(
                                viewModel,
                                connectionViewModel,
                                homeViewmodel,
                                item,
                                expanded = expandedStates[item.region.name] ?: false,
                                onExpandChange = { expandedStates[item.region.name] = it }
                            )
                        }
                    }
                    UpgradeBar(homeViewmodel)
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

@Composable
fun UpgradeBar(viewModel: HomeViewmodel?) {
    val activity = LocalContext.current as AppStartActivity
    val userState by viewModel?.userState?.collectAsState() ?: return
    if (userState is UserState.Free) {
        val angle = (userState as UserState.Free).dataLeftAngle
        val textColor = if (angle <= 0) {
            AppColors.red
        } else if (angle > 0 && angle <= 36) {
            Color.Yellow
        } else {
            AppColors.neonGreen
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = AppColors.homeBackground)
                    .border(
                        width = 1.dp,
                        color = AppColors.white5,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                Row(modifier = Modifier.clickable {
                    activity.startActivity(UpgradeActivity.getStartIntent(activity))
                }) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .size(40.dp)
                        ) {
                            val strokeWidth = 3.dp.toPx()

                            drawArc(
                                color = if (angle > 0 && angle <= 36) Color.Yellow else AppColors.neonGreen,
                                startAngle = 160f,
                                sweepAngle = angle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                                size = Size(size.width, size.height),
                                topLeft = Offset.Zero
                            )

                            drawArc(
                                color = AppColors.white20,
                                startAngle = 160f + angle,
                                sweepAngle = 360f - angle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                                size = Size(size.width, size.height),
                                topLeft = Offset.Zero
                            )
                        }
                        Text(
                            (userState as UserState.Free).dataLeft,
                            style = font9,
                            lineHeight = 9.sp,
                            textAlign = TextAlign.Center,
                            color = textColor,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            stringResource(R.string.unblock_full_access),
                            style = font16.copy(fontSize = 15.sp),
                            color = AppColors.white,
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            stringResource(R.string.go_pro_for_unlimited_everything),
                            style = font12,
                            color = AppColors.cyberBlue.copy(alpha = 0.7f),
                        )
                    }
                    Spacer(modifier = Modifier.weight(1.0f))
                    Image(
                        painter = painterResource(R.drawable.arrow_right),
                        contentDescription = "Upgrade",
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.CenterVertically)
                    )
                }
            }
        }
    }
}

@Composable
fun LocationCount(viewModel: ServerViewModel) {
    val serverListState by viewModel.serverListState.collectAsState()
    if (serverListState is ListState.Success) {
        Text(
            text = "All locations (${(serverListState as ListState.Success).data.count()})",
            style = font12,
            color = AppColors.white70,
            modifier = Modifier.padding(start = 8.dp, top = 16.dp)
        )
    }
}

@Composable
fun SplitBorderCircle(
    firstSectionAngle: Float,
    firstColor: Color,
    secondColor: Color,
    flagRes: Int
) {
    Box(
        modifier = Modifier
            .size(24.dp)
    ) {
        Canvas(
            modifier = Modifier
                .size(24.dp)
                .padding(2.dp)
        ) {
            val strokeWidth = 1.dp.toPx()

            drawArc(
                color = firstColor,
                startAngle = 160f, // Start from top
                sweepAngle = firstSectionAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                size = Size(size.width, size.height),
                topLeft = Offset.Zero
            )

            drawArc(
                color = secondColor,
                startAngle = 160f + firstSectionAngle,
                sweepAngle = 360f - firstSectionAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                size = Size(size.width, size.height),
                topLeft = Offset.Zero
            )
        }
        Image(
            painter = painterResource(id = flagRes),
            contentDescription = "Flag",
            modifier = Modifier
                .size(36.dp)
                .padding(4.dp)
                .align(Alignment.Center)
        )
    }
}

@Composable
private fun BestLocation(item: ServerListItem, connectionViewModel: ConnectionViewmodel) {
    val health = averageHealth(item)
    val color = colorResource(healthColor(health))
    val angle = (health / 100f) * 360f
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(start = 12.dp, end = 8.dp)
            .clickable {
                connectionViewModel.onCityClick(item.cities.first())
            }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SplitBorderCircle(
                angle,
                color,
                AppColors.white70,
                FlagIconResource.getSmallFlag(item.region.countryCode)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.best_location),
                style = font16.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f),
                color = AppColors.white70,
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.weight(1.0f))
            Image(
                painter = painterResource(R.drawable.arrow_right),
                contentDescription = "Expand",
                modifier = Modifier
                    .size(32.dp)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun ExpandableListItem(
    viewModel: ServerViewModel,
    connectionViewModel: ConnectionViewmodel,
    homeViewmodel: HomeViewmodel,
    item: ServerListItem,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit
) {
    val health = averageHealth(item)
    val color = colorResource(healthColor(health))
    val angle = (health / 100f) * 360f
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable { onExpandChange(!expanded) }
                .padding(start = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SplitBorderCircle(
                angle,
                color,
                AppColors.white70,
                FlagIconResource.getSmallFlag(item.region.countryCode)
            )
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = item.region.name,
                style = font16.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f),
                color = if (expanded) AppColors.white else AppColors.white70,
                textAlign = TextAlign.Start
            )
            if (item.region.p2p == 0) {
                Image(
                    painter = painterResource(R.drawable.p2p),
                    contentDescription = "P2P",
                    colorFilter = ColorFilter.tint(AppColors.white70),
                    modifier = Modifier
                        .size(16.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
            }
            Image(
                painter = painterResource(if (expanded) R.drawable.ic_server_list_open else R.drawable.ic_server_list_close),
                contentDescription = "Expand",
                modifier = Modifier
                    .size(32.dp)
                    .padding(8.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(
                            bounded = false,
                            radius = 16.dp,
                            color = AppColors.white
                        )
                    ) {
                        onExpandChange(!expanded)
                    }
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                item.cities.forEach {
                    ServerListItemView(it, viewModel, connectionViewModel, homeViewmodel)
                }
            }
        }
    }
}

@Composable
private fun ServerListItemView(
    item: City,
    viewModel: ServerViewModel,
    connectionViewModel: ConnectionViewmodel,
    homeViewmodel: HomeViewmodel
) {
    val userState by homeViewmodel.userState.collectAsState()
    val favouriteState by viewModel.favouriteListState.collectAsState()
    var isFavorite = false
    if (favouriteState is ListState.Success) {
        isFavorite = (favouriteState as ListState.Success).data.any { it.city.id == item.id }
    }
    val color = colorResource(healthColor(item.health))
    val angle = (item.health / 100f) * 360f
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
                connectionViewModel.onCityClick(item)
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ServerListIcon(item, userState, angle, color)
        Spacer(modifier = Modifier.width(8.dp))
        ServerNodeName("${item.nodeName} ${item.nickName}", Modifier.weight(1f))
        if (item.linkSpeed == "10000") {
            TenGIcon()
            Spacer(modifier = Modifier.width(12.dp))
        }
        LatencyIcon(latency)
        Spacer(modifier = Modifier.width(12.dp))
        FavouriteIcon(isFavorite) {
            viewModel.toggleFavorite(item)
        }
    }
}

@Composable
private fun ProgressIndicator() {
    Box(modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.Center), color = AppColors.white
        )
    }
}