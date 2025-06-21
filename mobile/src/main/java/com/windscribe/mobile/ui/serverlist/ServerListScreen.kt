
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.ui.serverlist.AllServerList
import com.windscribe.mobile.ui.serverlist.ConfigServerList
import com.windscribe.mobile.ui.serverlist.FavouriteList
import com.windscribe.mobile.ui.serverlist.ServerListNavigation
import com.windscribe.mobile.ui.serverlist.StaticIPServerList
import com.windscribe.mobile.ui.serverlist.ConfigViewmodel
import com.windscribe.mobile.ui.connection.ConnectionViewmodel
import com.windscribe.mobile.ui.home.HomeViewmodel
import com.windscribe.mobile.ui.serverlist.ServerListType
import com.windscribe.mobile.ui.serverlist.ServerViewModel
import com.windscribe.mobile.ui.theme.serverListBackgroundColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ServerListScreen(
    viewModel: ServerViewModel,
    connectionViewModel: ConnectionViewmodel,
    configViewmodel: ConfigViewmodel,
    homeViewmodel: HomeViewmodel
) {
    val selectedType by viewModel.selectedServerListType.collectAsState()
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0.0f,
        pageCount = { 4 },
    )
    LaunchedEffect(selectedType) {
        pagerState.animateScrollToPage(selectedType.toPageIndex())
    }
    LaunchedEffect(pagerState.targetPage) {
        viewModel.setSelectedType(pagerState.targetPage.toServerListType())
    }
    val coroutineScope = rememberCoroutineScope()
    Box {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.serverListBackgroundColor)
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.Top
        ) {
            HorizontalPager(
                beyondViewportPageCount = 4,
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopStart
                ) {
                    when (pageIndex) {
                        0 -> AllServerList(viewModel, connectionViewModel, homeViewmodel)
                        1 -> FavouriteList(viewModel, connectionViewModel, homeViewmodel)
                        2 -> StaticIPServerList(viewModel, connectionViewModel)
                        3 -> ConfigServerList(viewModel, connectionViewModel, configViewmodel)
                    }
                }
            }
        }
        ServerListNavigation(
            modifier = Modifier.offset(y = (-54.0f).dp),
            viewModel = viewModel,
            homeViewmodel = homeViewmodel,
            onTabSelected = { index ->
                coroutineScope.launch {
                    viewModel.setSelectedType(index.toServerListType())
                }
            }
        )
    }
}


private fun ServerListType.toPageIndex(): Int = when (this) {
    ServerListType.All -> 0
    ServerListType.Fav -> 1
    ServerListType.Static -> 2
    ServerListType.Config -> 3
}

private fun Int.toServerListType(): ServerListType = when (this) {
    0 -> ServerListType.All
    1 -> ServerListType.Fav
    2 -> ServerListType.Static
    3 -> ServerListType.Config
    else -> ServerListType.All
}