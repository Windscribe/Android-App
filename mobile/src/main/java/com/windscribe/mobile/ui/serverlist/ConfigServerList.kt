package com.windscribe.mobile.ui.serverlist

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.common.AddButton
import com.windscribe.mobile.ui.common.AddButtonWithDetails
import com.windscribe.mobile.ui.common.CustomConfigItem
import com.windscribe.mobile.ui.connection.ConnectionViewmodel
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.serverListSecondaryColor
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigServerList(
    viewModel: ServerViewModel,
    connectionViewModel: ConnectionViewmodel,
    configViewmodel: ConfigViewmodel
) {
    val state by viewModel.configListState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { configViewmodel.loadConfigFile(context, it) } }

    when (state) {
        is ListState.Loading -> ProgressIndicator()
        is ListState.Error -> ErrorView(filePickerLauncher)
        is ListState.Success -> SuccessView(
            (state as ListState.Success).data,
            filePickerLauncher,
            viewModel,
            connectionViewModel,
            configViewmodel
        )
    }
}

@Composable
private fun ErrorView(filePickerLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>) {
    Box(modifier = Modifier.fillMaxSize()) {
        Icon(
            imageVector = Icons.Default.Add,
            tint = MaterialTheme.colorScheme.serverListSecondaryColor,
            contentDescription = "",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .clickable { filePickerLauncher.launch(arrayOf("*/*")) }
        )
        Text(
            "Error loading config server list",
            style = font16,
            color = MaterialTheme.colorScheme.serverListSecondaryColor,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuccessView(
    list: List<ConfigListItem>,
    filePickerLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>,
    viewModel: ServerViewModel,
    connectionViewModel: ConnectionViewmodel,
    configViewmodel: ConfigViewmodel,
) {
    val isRefreshing by viewModel.refreshState.collectAsState()
    val lazyListState = rememberLazyListState()
    Box(modifier = Modifier.fillMaxSize()) {
        if (list.isEmpty()) {
            AddButtonWithDetails(
                com.windscribe.vpn.R.string.add_vpn_config,
                com.windscribe.vpn.R.string.no_custom_configs,
                R.drawable.ic_location_config
            ) {
                filePickerLauncher.launch(arrayOf("*/*"))
            }
        } else {
            Column(Modifier
                .fillMaxSize()) {
                Text(
                    text = stringResource(com.windscribe.vpn.R.string.custom_configs),
                    style = font12,
                    color = MaterialTheme.colorScheme.serverListSecondaryColor.copy(alpha = 0.70f),
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        viewModel.refresh(ServerListType.Config)
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
                        items(list, key = { it.id }) { item ->
                            CustomConfigItem(item, viewModel, connectionViewModel, configViewmodel)
                        }
                    }
                }
                AddButton(com.windscribe.vpn.R.string.add_vpn_config) {
                    filePickerLauncher.launch(arrayOf("*/*"))
                }
            }
        }
    }
}

@Composable
private fun ProgressIndicator() {
    Box(modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center),
            color = AppColors.white
        )
    }
}