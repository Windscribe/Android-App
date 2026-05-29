package com.windscribe.mobile.ui.preferences.network_options

import PreferencesNavBar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.common.SwitchItemView
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.helper.hapticClickable
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.R
import com.windscribe.vpn.constants.FeatureExplainer
import com.windscribe.vpn.localdatabase.tables.NetworkInfo

/**
 * Snapshot of the network-options UI state. Hoisted so the stateless [NetworkOptionsContent]
 * never touches [NetworkOptionsViewModel] — previews feed it directly.
 */
data class NetworkOptionsState(
    val autoSecureEnabled: Boolean = false,
    val currentNetwork: NetworkInfo? = null,
    val allNetworks: List<NetworkInfo> = emptyList(),
)

/**
 * Stateful entry point. Owns the [NetworkOptionsViewModel], collects its flows, then delegates
 * rendering to [NetworkOptionsContent].
 */
@Composable
fun NetworkOptionsScreen(viewModel: NetworkOptionsViewModel = hiltViewModel<NetworkOptionsViewModelImpl>()) {
    val autoSecureEnabled by viewModel.autoSecureEnabled.collectAsState()
    val currentNetwork by viewModel.currentNetwork.collectAsState()
    val allNetworks by viewModel.allNetworks.collectAsState()

    NetworkOptionsContent(
        state =
            NetworkOptionsState(
                autoSecureEnabled = autoSecureEnabled,
                currentNetwork = currentNetwork,
                allNetworks = allNetworks,
            ),
        onAutoSecureChanged = viewModel::onAutoSecureChanged,
    )
}

/**
 * Stateless network-options UI. Everything it needs is passed in, so it renders identically in
 * the app and in `@Preview`. This is the composable previews target.
 */
@Composable
fun NetworkOptionsContent(
    state: NetworkOptionsState,
    onAutoSecureChanged: () -> Unit,
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
            Spacer(modifier = Modifier.height(20.dp))
            SwitchItemView(
                title = R.string.auto_secure_new_networks,
                icon = com.windscribe.mobile.R.drawable.ic_wifi,
                description = R.string.auto_secure_new_networks_description,
                state.autoSecureEnabled,
                explainer = FeatureExplainer.NETWORK_OPTIONS,
                onSelect = {
                    onAutoSecureChanged()
                },
            )
            val currentNetwork = state.currentNetwork
            if (currentNetwork != null) {
                Spacer(modifier = Modifier.height(16.dp))
                CurrentNetwork(currentNetwork)
            }
            Spacer(modifier = Modifier.height(16.dp))
            OtherNetworks(state.allNetworks)
        }
    }
}

@Composable
private fun CurrentNetwork(networkInfo: NetworkInfo) {
    Column {
        Text(
            text = stringResource(R.string.current_network),
            color = MaterialTheme.colorScheme.preferencesSubtitleColor,
            style = font12.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Network(networkInfo)
    }
}

@Composable
private fun Network(networkInfo: NetworkInfo) {
    val navController = LocalNavController.current
    Row(
        modifier =
            Modifier
                .height(48.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                ).hapticClickable {
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        "network_name",
                        networkInfo.networkName,
                    )
                    navController.navigate(Screen.NetworkDetails.route)
                }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = networkInfo.networkName,
            color = MaterialTheme.colorScheme.primaryTextColor,
            style = font16.copy(fontWeight = FontWeight.Medium),
            textAlign = TextAlign.Start,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(if (networkInfo.isAutoSecureOn) R.string.network_secured else R.string.network_unsecured),
            color = MaterialTheme.colorScheme.preferencesSubtitleColor,
            style = font16.copy(fontWeight = FontWeight.Normal),
            textAlign = TextAlign.Start,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            painterResource(com.windscribe.mobile.R.drawable.arrow_right),
            contentDescription = "",
            tint = MaterialTheme.colorScheme.primaryTextColor,
        )
    }
}

@Composable
private fun OtherNetworks(allNetworks: List<NetworkInfo>) {
    if (allNetworks.isNotEmpty()) {
        Column {
            Text(
                text = stringResource(R.string.other_networks),
                color = MaterialTheme.colorScheme.preferencesSubtitleColor,
                style = font12.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(allNetworks.size) { index ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Network(allNetworks[index])
                }
            }
        }
    }
}

/**
 * Feeds representative [NetworkOptionsState] values into the preview.
 */
private class NetworkOptionsStateProvider : PreviewParameterProvider<NetworkOptionsState> {
    override val values =
        sequenceOf(
            NetworkOptionsState(autoSecureEnabled = false),
            NetworkOptionsState(
                autoSecureEnabled = true,
                currentNetwork = NetworkInfo("Home WiFi", true, false, "wstunnel", "443"),
                allNetworks =
                    listOf(
                        NetworkInfo("Office WiFi", false, false, "wstunnel", "443"),
                    ),
            ),
        )
}

@Composable
@MultiDevicePreview
private fun NetworkOptionsContentPreview(
    @PreviewParameter(NetworkOptionsStateProvider::class) state: NetworkOptionsState,
) {
    PreviewWithNav {
        NetworkOptionsContent(
            state = state,
            onAutoSecureChanged = {},
        )
    }
}
