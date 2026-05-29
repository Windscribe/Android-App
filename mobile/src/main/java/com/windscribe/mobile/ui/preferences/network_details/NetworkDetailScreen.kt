package com.windscribe.mobile.ui.preferences.network_details

import PreferencesNavBar
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.windscribe.mobile.ui.common.CustomDropDown
import com.windscribe.mobile.ui.common.Description
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.common.SwitchItemView
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.model.DropDownStringItem
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.R
import com.windscribe.vpn.localdatabase.tables.NetworkInfo

/**
 * Snapshot of the network-detail UI state. Hoisted so the stateless [NetworkDetailContent]
 * never touches [NetworkDetailViewModel] — previews feed it directly.
 */
data class NetworkDetailState(
    val networkDetail: NetworkInfo? = null,
    val isMyNetwork: Boolean = false,
    val protocols: List<DropDownStringItem> = emptyList(),
    val ports: List<DropDownStringItem> = emptyList(),
)

/**
 * Callbacks the network-detail UI can raise.
 */
class NetworkDetailActions(
    val onAutoSecureChanged: () -> Unit = {},
    val onProtocolSelected: (DropDownStringItem) -> Unit = {},
    val onPortSelected: (DropDownStringItem) -> Unit = {},
    val onPreferredChanged: () -> Unit = {},
    val onForgetNetwork: () -> Unit = {},
)

/**
 * Stateful entry point. Owns the [NetworkDetailViewModel], reads the network name passed through
 * the previous back-stack entry's saved state, collects flows, then delegates rendering to
 * [NetworkDetailContent].
 */
@Composable
fun NetworkDetailScreen(viewModel: NetworkDetailViewModel = hiltViewModel<NetworkDetailViewModelImpl>()) {
    val navController = LocalNavController.current
    val networkName = navController.previousBackStackEntry?.savedStateHandle?.get<String>("network_name")

    LaunchedEffect(networkName) {
        networkName?.let { viewModel.setNetworkName(it) }
    }

    val networkDetail by viewModel.networkDetail.collectAsState()
    val isMyNetwork by viewModel.isMyNetwork.collectAsState()
    val protocols by viewModel.protocols.collectAsState()
    val ports by viewModel.ports.collectAsState()

    NetworkDetailContent(
        state =
            NetworkDetailState(
                networkDetail = networkDetail,
                isMyNetwork = isMyNetwork,
                protocols = protocols,
                ports = ports,
            ),
        actions =
            NetworkDetailActions(
                onAutoSecureChanged = viewModel::onAutoSecureChanged,
                onProtocolSelected = viewModel::onProtocolSelected,
                onPortSelected = viewModel::onPortSelected,
                onPreferredChanged = viewModel::onPreferredChanged,
                onForgetNetwork = viewModel::forgetNetwork,
            ),
    )
}

/**
 * Stateless network-detail UI. Everything it needs is passed in, so it renders identically in
 * the app and in `@Preview`. This is the composable previews target.
 */
@Composable
fun NetworkDetailContent(
    state: NetworkDetailState,
    actions: NetworkDetailActions,
) {
    val navController = LocalNavController.current
    PreferenceBackground {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(vertical = 16.dp, horizontal = 16.dp),
        ) {
            PreferencesNavBar(stringResource(R.string.network_options)) {
                navController.popBackStack()
            }

            val networkDetail = state.networkDetail
            if (networkDetail == null) {
                // Loading state - keeps the screen visible during animation
                Spacer(modifier = Modifier.weight(1f))
            } else {
                Spacer(modifier = Modifier.height(20.dp))
                SwitchItemView(
                    title = R.string.auto_secure,
                    icon = com.windscribe.mobile.R.drawable.ic_wifi,
                    description = R.string.auto_secure_description,
                    networkDetail.isAutoSecureOn,
                    onSelect = {
                        actions.onAutoSecureChanged()
                    },
                )
                Spacer(modifier = Modifier.height(16.dp))
                PreferredProtocol(
                    networkDetail,
                    state.protocols,
                    state.ports,
                    actions.onProtocolSelected,
                    actions.onPortSelected,
                    actions.onPreferredChanged,
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (!state.isMyNetwork) {
                    ForgetNetwork(actions.onForgetNetwork)
                }
            }
        }
    }
}

@Composable
private fun PreferredProtocol(
    networkInfo: NetworkInfo?,
    protocols: List<DropDownStringItem>,
    ports: List<DropDownStringItem>,
    onProtocolSelected: (DropDownStringItem) -> Unit,
    onPortSelected: (DropDownStringItem) -> Unit,
    onPreferredChanged: () -> Unit,
) {
    Column {
        Header(networkInfo, onPreferredChanged)
        Spacer(modifier = Modifier.height(1.dp))
        CustomDropDown(
            R.string.protocol,
            protocols,
            networkInfo?.protocol ?: "",
            onSelect = {
                onProtocolSelected(it)
            },
            shape = RoundedCornerShape(0.dp),
        )
        Spacer(modifier = Modifier.height(1.dp))
        CustomDropDown(
            R.string.port,
            ports,
            networkInfo?.port ?: "",
            onSelect = {
                onPortSelected(it)
            },
            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
        )
    }
}

@Composable
private fun Header(
    networkDetail: NetworkInfo?,
    onPreferredChanged: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                ).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(com.windscribe.mobile.R.drawable.ic_connection_mode_icon),
                contentDescription = "",
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primaryTextColor),
            )
            Spacer(modifier = Modifier.padding(8.dp))
            Text(
                stringResource(R.string.preferred_protocol),
                style = font16,
                color = MaterialTheme.colorScheme.primaryTextColor,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (networkDetail?.isPreferredOn == true) {
                Image(
                    painter = painterResource(id = com.windscribe.mobile.R.drawable.ic_toggle_button_on),
                    contentDescription = null,
                    modifier =
                        Modifier.clickable {
                            onPreferredChanged()
                        },
                )
            } else {
                Image(
                    painter = painterResource(id = com.windscribe.mobile.R.drawable.ic_toggle_button_off),
                    contentDescription = null,
                    modifier =
                        Modifier.clickable {
                            onPreferredChanged()
                        },
                )
            }
        }
        Spacer(modifier = Modifier.padding(8.dp))
        Description(stringResource(R.string.preferred_protocol_description))
    }
}

@Composable
private fun ForgetNetwork(onForgetNetwork: () -> Unit) {
    val navController = LocalNavController.current
    Row(
        modifier =
            Modifier
                .background(
                    color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(size = 12.dp),
                ).clickable {
                    onForgetNetwork()
                    navController.popBackStack()
                }.padding(vertical = 14.dp, horizontal = 14.dp),
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            stringResource(R.string.forget_network),
            style =
                font16.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primaryTextColor,
                ),
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * Feeds representative [NetworkDetailState] values into the preview.
 */
private class NetworkDetailStateProvider : PreviewParameterProvider<NetworkDetailState> {
    override val values =
        sequenceOf(
            NetworkDetailState(networkDetail = null),
            NetworkDetailState(
                networkDetail = NetworkInfo("Home WiFi", true, true, "wstunnel", "443"),
                isMyNetwork = false,
                protocols =
                    listOf(
                        DropDownStringItem("wstunnel", "WSTunnel"),
                        DropDownStringItem("udp", "UDP"),
                    ),
                ports =
                    listOf(
                        DropDownStringItem("443", "443"),
                        DropDownStringItem("80", "80"),
                    ),
            ),
        )
}

@Composable
@MultiDevicePreview
private fun NetworkDetailContentPreview(
    @PreviewParameter(NetworkDetailStateProvider::class) state: NetworkDetailState,
) {
    PreviewWithNav {
        NetworkDetailContent(
            state = state,
            actions = NetworkDetailActions(),
        )
    }
}
