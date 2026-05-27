package com.windscribe.mobile.ui.serverlist

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.helper.hapticClickable
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.common.DataCenterFavouriteIcon
import com.windscribe.mobile.ui.common.DataCenterLatencyIcon
import com.windscribe.mobile.ui.common.DataCenterIcon
import com.windscribe.mobile.ui.common.DataCenterName
import com.windscribe.mobile.ui.common.DataCenterNoP2PIcon
import com.windscribe.mobile.ui.common.TenGIcon
import com.windscribe.mobile.ui.common.healthColor
import com.windscribe.mobile.ui.connection.ConnectionViewmodel
import com.windscribe.mobile.ui.helper.miniumHealthStart
import com.windscribe.mobile.ui.home.HomeViewmodel
import com.windscribe.mobile.ui.home.UserState
import com.windscribe.mobile.ui.theme.serverListBackgroundColor
import com.windscribe.mobile.ui.theme.serverListSecondaryColor
import com.windscribe.vpn.commonutils.FlagIconResource
import com.windscribe.vpn.serverlist.entity.Datacenter
import com.windscribe.vpn.serverlist.entity.DatacenterStatus
import com.windscribe.vpn.serverlist.entity.DatacenterStatusHelper

private const val MIN_HEALTH_VALUE = 50

@Composable
fun SearchServerList(viewModel: ServerViewModel, connectionViewModel: ConnectionViewmodel, homeViewmodel: HomeViewmodel) {
    val state by viewModel.searchListState.collectAsState()
    val expandedStates by viewModel.searchItemsExpandState.collectAsState()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("search_overlay")
            .background(MaterialTheme.colorScheme.serverListBackgroundColor)
            .statusBarsPadding().clickable {  }
    ) {
        SearchListNavigation(viewModel, homeViewmodel)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 54.dp)
                .verticalScroll(scrollState)
        ) {
            when (state) {
                is ListState.Loading -> {
                    ProgressIndicator()
                }

                is ListState.Error -> {
                    Text("Error loading server list", color = MaterialTheme.colorScheme.serverListSecondaryColor)
                }

                is ListState.Success -> {
                    LocationCount(viewModel)
                    Spacer(modifier = Modifier.height(8.dp))
                    (state as ListState.Success).data.forEach { item ->
                        LocationItem(
                            viewModel,
                            connectionViewModel,
                            homeViewmodel,
                            item,
                            expanded = expandedStates[item.region.name.orEmpty()] ?: false,
                            onExpandChange = {
                                viewModel.onExpandStateChanged(
                                    item.region.name.orEmpty(),
                                    it
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationItem(
    viewModel: ServerViewModel,
    connectionViewModel: ConnectionViewmodel,
    homeViewmodel: HomeViewmodel,
    item: ServerListItem,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit
) {
    val userState by homeViewmodel.userState.collectAsState()
    val locationHealth by viewModel.observeAverageRegionHealth(item.datacenters).collectAsState(initial = MIN_HEALTH_VALUE)
    val isLocationPremiumOnly by viewModel.observeRegionPremiumStatus(item.datacenters).collectAsState(initial = false)
    val showLocationLoad by homeViewmodel.showLocationLoad.collectAsState()
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable { onExpandChange(!expanded) }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LocationSplitBorderCircle(
                health = locationHealth,
                flagRes = FlagIconResource.getSmallFlag(item.region.countryCode),
                showProIcon = userState !is UserState.Pro && isLocationPremiumOnly,
                showLocationLoad = showLocationLoad
            )
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = item.region.name ?: "",
                style = font16.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f),
                color = if (expanded) MaterialTheme.colorScheme.serverListSecondaryColor else MaterialTheme.colorScheme.serverListSecondaryColor.copy(0.70f),
                textAlign = TextAlign.Start
            )
            Image(
                painter = painterResource(if (expanded) R.drawable.ic_server_list_open else R.drawable.ic_server_list_close),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.serverListSecondaryColor),
                contentDescription = "Expand",
                modifier = Modifier
                    .size(32.dp)
                    .padding(8.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(
                            bounded = false,
                            radius = 16.dp,
                            color = MaterialTheme.colorScheme.serverListSecondaryColor
                        )
                    ) {
                        onExpandChange(!expanded)
                    }
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                item.datacenters.forEach {
                    DataCenterItem(it, viewModel, connectionViewModel, homeViewmodel)
                }
            }
        }
    }
}

@Composable
private fun DataCenterItem(
    item: Datacenter,
    viewModel: ServerViewModel,
    connectionViewModel: ConnectionViewmodel,
    homeViewmodel: HomeViewmodel
) {
    val favouriteState by viewModel.favouriteListState.collectAsState()
    val showLocationLoad by homeViewmodel.showLocationLoad.collectAsState()
    var isFavorite = false
    if (favouriteState is ListState.Success) {
        isFavorite = (favouriteState as ListState.Success).data.any { it.city.id == item.id }
    }
    val health by viewModel.observeAverageHealth(item.id).collectAsState(initial = MIN_HEALTH_VALUE)
    val latencyState by viewModel.latencyListState.collectAsState()
    val latency by rememberUpdatedState(
        if (latencyState is ListState.Success) {
            (latencyState as ListState.Success).data.find { it.id == item.id }?.time ?: -1
        } else -1
    )
    val serverCount by viewModel.observeDatacenterServerCount(item.id).collectAsState(initial = 0)
    val isPro by viewModel.isPro.collectAsState()
    val isAvailable = DatacenterStatusHelper.getStatus(item, serverCount, isPro) == DatacenterStatus.Available
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(
                interactionSource,
                indication = ripple(bounded = true, color = MaterialTheme.colorScheme.serverListSecondaryColor)
            ) {
                viewModel.toggleSearch()
                connectionViewModel.onCityClick(item)
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DataCenterIcon(item, health, showLocationLoad, serverCount, isPro)
        Spacer(modifier = Modifier.width(8.dp))
        DataCenterName("${item.nodeName} ${item.nickName}", Modifier.weight(1f))
        if (item.p2p != 1) {
            DataCenterNoP2PIcon()
            Spacer(modifier = Modifier.width(12.dp))
        }
        if (isAvailable) {
            DataCenterLatencyIcon(latency)
            Spacer(modifier = Modifier.width(12.dp))
        }
        DataCenterFavouriteIcon(isFavorite) {
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
                .align(Alignment.Center), color = MaterialTheme.colorScheme.serverListSecondaryColor
        )
    }
}


@SuppressLint("UnrememberedMutableInteractionSource")
@Composable
private fun SearchListNavigation(viewModel: ServerViewModel, homeViewmodel: HomeViewmodel) {
    val query by viewModel.searchKeyword.collectAsState()
    val isHapticEnabled by homeViewmodel.hapticFeedbackEnabled.collectAsState()
   val stroke = MaterialTheme.colorScheme.serverListSecondaryColor.copy(0.10f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                drawLine(
                    color = stroke,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = strokeWidth
                )
            }
            .background(MaterialTheme.colorScheme.serverListBackgroundColor)
            .padding(horizontal = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(R.drawable.ic_location_search),
                contentDescription = "Search",
                modifier = Modifier.Companion.hapticClickable() {
                    viewModel.toggleSearch()
                },
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.serverListSecondaryColor.copy(alpha = 0.70f))
            )
            TextField(
                value = query,
                onValueChange = viewModel::onQueryTextChange,
                modifier = Modifier
                    .testTag("search_input")
                    .weight(1f)
                    .fillMaxHeight(),
                textStyle = font16.copy(textAlign = TextAlign.Start, color = MaterialTheme.colorScheme.serverListSecondaryColor),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.serverListSecondaryColor
                ),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            )
            Text(
                stringResource(com.windscribe.vpn.R.string.clear),
                style = font16,
                color = AppColors.cyberBlue.copy(0.7f),
                modifier = Modifier
                    .padding(8.dp)
                    .hapticClickable() {
                        viewModel.onQueryTextChange("")
                    })
            Image(
                painter = painterResource(R.drawable.ic_search_location_close),
                contentDescription = "Search",
                modifier = Modifier
                    .testTag("search_close")
                    .hapticClickable() {
                        viewModel.clearSearch()
                        viewModel.toggleSearch()
                    },
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.serverListSecondaryColor)
            )
        }
    }
}