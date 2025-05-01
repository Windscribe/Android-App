package com.windscribe.mobile.view.screen

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.view.theme.AppColors
import com.windscribe.mobile.view.theme.Dimen
import com.windscribe.mobile.view.theme.font12
import com.windscribe.mobile.view.theme.font16
import com.windscribe.mobile.view.ui.AddButton
import com.windscribe.mobile.view.ui.AddButtonWithDetails
import com.windscribe.mobile.view.ui.CustomConfigItem
import com.windscribe.mobile.viewmodel.ConfigListItem
import com.windscribe.mobile.viewmodel.ConfigViewmodel
import com.windscribe.mobile.viewmodel.ConnectionViewmodel
import com.windscribe.mobile.viewmodel.ListState
import com.windscribe.mobile.viewmodel.ServerListType
import com.windscribe.mobile.viewmodel.ServerViewModel

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
            scrollState,
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
            tint = Color.White,
            contentDescription = "",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .clickable { filePickerLauncher.launch(arrayOf("*/*")) }
        )
        Text(
            "Error loading config server list",
            style = font16,
            color = AppColors.white,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuccessView(
    list: List<ConfigListItem>,
    scrollState: androidx.compose.foundation.ScrollState,
    filePickerLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>,
    viewModel: ServerViewModel,
    connectionViewModel: ConnectionViewmodel,
    configViewmodel: ConfigViewmodel,
    pullToRefreshState: PullToRefreshState = rememberPullToRefreshState()
) {
    val isRefreshing by viewModel.refreshState.collectAsState()

    LaunchedEffect(pullToRefreshState.isRefreshing) {
        if (pullToRefreshState.isRefreshing) {
            viewModel.refresh(ServerListType.Config)
        }
    }

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing && pullToRefreshState.isRefreshing) {
            pullToRefreshState.endRefresh()
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        if (list.isEmpty()) {
            AddButtonWithDetails(
                R.string.add_vpn_config,
                R.string.no_custom_configs,
                R.drawable.ic_location_config
            ) {
                filePickerLauncher.launch(arrayOf("*/*"))
            }
        } else {
            Column(Modifier
                .fillMaxSize()
                .nestedScroll(pullToRefreshState.nestedScrollConnection)) {
                Text(
                    text = stringResource(R.string.custom_configs),
                    style = font12,
                    color = AppColors.white70,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(list, key = { it.id }) { item ->
                        CustomConfigItem(item, viewModel, connectionViewModel, configViewmodel)
                    }
                }
                Spacer(modifier = Modifier.weight(1.0f))
                AddButton(R.string.add_vpn_config) {
                    filePickerLauncher.launch(arrayOf("*/*"))
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

@Composable
private fun ProgressIndicator() {
    Box(modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(Dimen.dp48)
                .align(Alignment.Center),
            color = AppColors.white
        )
    }
}