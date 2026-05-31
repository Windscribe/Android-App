package com.windscribe.mobile.ui.preferences.connection

import PreferencesNavBar
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.windscribe.mobile.ui.AppStartActivity
import com.windscribe.mobile.ui.common.CustomDropDown
import com.windscribe.mobile.ui.common.DescriptionWithLearnMore
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.common.RequestLocationPermissions
import com.windscribe.mobile.ui.common.SwitchItemView
import com.windscribe.mobile.ui.connection.ToastMessage
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.helper.hapticClickable
import com.windscribe.mobile.ui.model.DropDownStringItem
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.backgroundColor
import com.windscribe.mobile.ui.theme.font14
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.R
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.CONNECTION_MODE_AUTO
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.CONNECTION_MODE_MANUAL
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.DNS_MODE_CUSTOM
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.DNS_MODE_ROBERT
import com.windscribe.vpn.constants.FeatureExplainer
import com.windscribe.vpn.mocklocation.MockLocationManager.Companion.isAppSelectedInMockLocationList
import com.windscribe.vpn.mocklocation.MockLocationManager.Companion.isDevModeOn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Callbacks the connection-preferences UI can raise. Hoisted out of the composables so the
 * stateless [ConnectionContent] never needs to know about [ConnectionViewModel] — previews supply
 * no-op lambdas.
 */
class ConnectionActions(
    val onAutoConnectToggleClicked: () -> Unit = {},
    val onAllowLanToggleClicked: () -> Unit = {},
    val onStartOnBootToggleClicked: () -> Unit = {},
    val onGPSSpoofingToggleClicked: () -> Unit = {},
    val onDecoyTrafficToggleClicked: () -> Unit = {},
    val onFakeTrafficVolumeSelected: (DropDownStringItem) -> Unit = {},
    val onIpStackEgressModeSelected: (DropDownStringItem) -> Unit = {},
    val onModeSelected: (String) -> Unit = {},
    val onProtocolSelected: (DropDownStringItem) -> Unit = {},
    val onPortSelected: (DropDownStringItem) -> Unit = {},
    val onDNSModeSelected: (String) -> Unit = {},
    val onCustomDNSAddressChanged: (String) -> Unit = {},
    val onSaveCustomDNSAddress: () -> Unit = {},
    val onPacketSizeModeSelected: (Boolean) -> Unit = {},
    val onPacketSizedChanged: (Int) -> Unit = {},
    val onPacketSizeSaved: () -> Unit = {},
    val onAutoDetectClicked: () -> Unit = {},
)

/**
 * Immutable snapshot of all [ConnectionViewModel] state the UI renders. Bundled so the stateless
 * [ConnectionContent] takes a single state object plus [ConnectionActions].
 */
data class ConnectionUiState(
    val autoConnect: Boolean = false,
    val allowLan: Boolean = false,
    val startOnBoot: Boolean = false,
    val gpsSpoofing: Boolean = false,
    val decoyTraffic: Boolean = false,
    val trafficMultiplier: String = "",
    val trafficMultipliers: List<DropDownStringItem> = emptyList(),
    val potentialDataUse: String = "",
    val ipStackEgressMode: String = "auto",
    val ipStackEgressModes: List<DropDownStringItem> = emptyList(),
    val mode: String = CONNECTION_MODE_AUTO,
    val protocols: List<DropDownStringItem> = listOf(DropDownStringItem("IKEv2")),
    val ports: List<DropDownStringItem> = listOf(DropDownStringItem("500")),
    val selectedProtocol: String = "IKEv2",
    val selectedPort: String = "500",
    val dnsMode: String = DNS_MODE_CUSTOM,
    val customDnsAddress: String = "1.1.1.1",
    val packetSizeAuto: Boolean = true,
    val packetSize: Int = 1500,
    val autoDetecting: Boolean = false,
)

/**
 * Stateful entry point. Owns the [ConnectionViewModel], collects its flows and wires the
 * preference refresh, then delegates rendering to [ConnectionContent].
 */
@Composable
fun ConnectionScreen(viewModel: ConnectionViewModel = hiltViewModel<ConnectionViewModelImpl>()) {
    val autoConnect by viewModel.autoConnect.collectAsState()
    val allowLan by viewModel.allowLan.collectAsState()
    val startOnBoot by viewModel.startOnBoot.collectAsState()
    val gpsSpoofing by viewModel.gpsSpoofing.collectAsState()
    val decoyTraffic by viewModel.decoyTraffic.collectAsState()
    val trafficMultiplier by viewModel.trafficMultiplier.collectAsState()
    val trafficMultipliers by viewModel.trafficMultipliers.collectAsState()
    val potentialDataUse by viewModel.potentialDataUse.collectAsState()
    val ipStackEgressMode by viewModel.ipStackEgressMode.collectAsState()
    val ipStackEgressModes by viewModel.ipStackEgressModes.collectAsState()
    val mode by viewModel.mode.collectAsState()
    val protocols by viewModel.protocols.collectAsState()
    val ports by viewModel.ports.collectAsState()
    val selectedProtocol by viewModel.selectedProtocol.collectAsState()
    val selectedPort by viewModel.selectedPort.collectAsState()
    val dnsMode by viewModel.dnsMode.collectAsState()
    val customDnsAddress by viewModel.customDnsAddress.collectAsState()
    val packetSizeAuto by viewModel.packetSizeAuto.collectAsState()
    val packetSize by viewModel.packetSize.collectAsState()
    val autoDetecting by viewModel.autoDetecting.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshPreferences()
    }

    val state =
        ConnectionUiState(
            autoConnect = autoConnect,
            allowLan = allowLan,
            startOnBoot = startOnBoot,
            gpsSpoofing = gpsSpoofing,
            decoyTraffic = decoyTraffic,
            trafficMultiplier = trafficMultiplier,
            trafficMultipliers = trafficMultipliers,
            potentialDataUse = potentialDataUse,
            ipStackEgressMode = ipStackEgressMode,
            ipStackEgressModes = ipStackEgressModes,
            mode = mode,
            protocols = protocols,
            ports = ports,
            selectedProtocol = selectedProtocol,
            selectedPort = selectedPort,
            dnsMode = dnsMode,
            customDnsAddress = customDnsAddress,
            packetSizeAuto = packetSizeAuto,
            packetSize = packetSize,
            autoDetecting = autoDetecting,
        )

    ConnectionContent(
        state = state,
        toastMessage = viewModel.toastMessage,
        actions =
            ConnectionActions(
                onAutoConnectToggleClicked = viewModel::onAutoConnectToggleClicked,
                onAllowLanToggleClicked = viewModel::onAllowLanToggleClicked,
                onStartOnBootToggleClicked = viewModel::onStartOnBootToggleClicked,
                onGPSSpoofingToggleClicked = viewModel::onGPSSpoofingToggleClicked,
                onDecoyTrafficToggleClicked = viewModel::onDecoyTrafficToggleClicked,
                onFakeTrafficVolumeSelected = viewModel::onFakeTrafficVolumeSelected,
                onIpStackEgressModeSelected = viewModel::onIpStackEgressModeSelected,
                onModeSelected = viewModel::onModeSelected,
                onProtocolSelected = viewModel::onProtocolSelected,
                onPortSelected = viewModel::onPortSelected,
                onDNSModeSelected = viewModel::onDNSModeSelected,
                onCustomDNSAddressChanged = viewModel::onCustomDNSAddressChanged,
                onSaveCustomDNSAddress = viewModel::saveCustomDNSAddress,
                onPacketSizeModeSelected = viewModel::onPacketSizeModeSelected,
                onPacketSizedChanged = viewModel::onPacketSizedChanged,
                onPacketSizeSaved = viewModel::onPacketSizeSaved,
                onAutoDetectClicked = viewModel::onAutoDetectClicked,
            ),
    )
}

/**
 * Stateless connection-preferences UI. Everything it needs is passed in, so it renders identically
 * in the app and in `@Preview`. This is the composable previews target.
 */
@Composable
fun ConnectionContent(
    state: ConnectionUiState,
    toastMessage: Flow<ToastMessage>,
    actions: ConnectionActions,
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    PreferenceBackground {
        Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)) {
            PreferencesNavBar(stringResource(R.string.connection)) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.height(20.dp))
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .navigationBarsPadding()
                        .verticalScroll(scrollState),
            ) {
                ConnectionItem(
                    R.string.network_options,
                    Screen.NetworkOptions,
                )
                Spacer(modifier = Modifier.height(16.dp))
                ConnectionItem(
                    R.string.split_tunneling,
                    Screen.SplitTunnel,
                )
                Spacer(modifier = Modifier.height(16.dp))
                ConnectionItem(
                    R.string.anti_censorship_settings,
                    Screen.AntiCensorship,
                )
                Spacer(modifier = Modifier.height(16.dp))
                AlwaysOnVPN()
                Spacer(modifier = Modifier.height(16.dp))
                SwitchItemView(
                    title = R.string.auto_connect,
                    icon = com.windscribe.mobile.R.drawable.auto_connect,
                    description = R.string.auto_connect_explainer,
                    state.autoConnect,
                    onSelect = { actions.onAutoConnectToggleClicked() },
                )
                Spacer(modifier = Modifier.height(16.dp))
                ConnectionMode(state, toastMessage, actions)
                Spacer(modifier = Modifier.height(16.dp))
                PacketSize(state, actions)
                Spacer(modifier = Modifier.height(16.dp))
                IPVersionMode(state, actions)
                Spacer(modifier = Modifier.height(16.dp))
                CustomDNS(state, actions)
                Spacer(modifier = Modifier.height(16.dp))
                SwitchItemView(
                    title = R.string.lan_by_pass,
                    icon = com.windscribe.mobile.R.drawable.ic_lan_icon,
                    description = R.string.allow_lan_description,
                    state.allowLan,
                    explainer = FeatureExplainer.ALLOW_LAN,
                    onSelect = { actions.onAllowLanToggleClicked() },
                )
                Spacer(modifier = Modifier.height(16.dp))
                SwitchItemView(
                    title = R.string.start_on_boot,
                    icon = com.windscribe.mobile.R.drawable.ic_auto_connect_boot,
                    description = R.string.auto_connect_on_boot_description,
                    state.startOnBoot,
                    onSelect = { actions.onStartOnBootToggleClicked() },
                )
                Spacer(modifier = Modifier.height(16.dp))
                SwitchItemView(
                    title = R.string.gps_spoofing,
                    icon = com.windscribe.mobile.R.drawable.ic_gps_spoof_icon,
                    description = R.string.gps_spoofing_description,
                    state.gpsSpoofing,
                    explainer = FeatureExplainer.GPS_SPOOFING,
                    onSelect = {
                        if (state.gpsSpoofing.not() &&
                            (
                                !isDevModeOn(context) ||
                                    !isAppSelectedInMockLocationList(
                                        context,
                                    )
                            )
                        ) {
                            navController.navigate(Screen.GpsSpoofing.route)
                        } else {
                            actions.onGPSSpoofingToggleClicked()
                        }
                    },
                )
                Spacer(modifier = Modifier.height(16.dp))
                DecoyTrafficMode(state, actions)
            }
        }
    }
}

@Composable
private fun DecoyTrafficMode(
    state: ConnectionUiState,
    actions: ConnectionActions,
) {
    val navController = LocalNavController.current
    val decoyTraffic = state.decoyTraffic
    val shape =
        if (decoyTraffic) {
            RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        } else {
            RoundedCornerShape(size = 12.dp)
        }
    Column {
        SwitchItemView(
            title = R.string.decoy_traffic,
            icon = com.windscribe.mobile.R.drawable.ic_decoy_icon,
            description = R.string.decoy_caution_description,
            decoyTraffic,
            shape = shape,
            explainer = FeatureExplainer.DECOY_TRAFFIC,
            onSelect = {
                if (decoyTraffic.not()) {
                    navController.navigate(Screen.ExtraDataUseWarning.route)
                } else {
                    actions.onDecoyTrafficToggleClicked()
                }
            },
        )
        if (decoyTraffic) {
            Spacer(modifier = Modifier.height(1.dp))
            CustomDropDown(
                R.string.fake_traffic_volume,
                state.trafficMultipliers,
                state.trafficMultiplier,
                shape = RoundedCornerShape(0.dp),
            ) {
                actions.onFakeTrafficVolumeSelected(it)
            }
            Spacer(modifier = Modifier.height(1.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .background(
                            MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                        ).padding(14.dp)
                        .fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.potential_data_use),
                    style = font16.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primaryTextColor,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = state.potentialDataUse,
                    style = font16,
                    color = MaterialTheme.colorScheme.preferencesSubtitleColor,
                )
            }
        }
    }
}

@Composable
private fun IPVersionMode(
    state: ConnectionUiState,
    actions: ConnectionActions,
) {
    Column {
        // Header with title and description only (no mode selector on right)
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
                    painterResource(com.windscribe.mobile.R.drawable.ip),
                    contentDescription = "",
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primaryTextColor),
                )
                Spacer(modifier = Modifier.padding(8.dp))
                Text(
                    stringResource(R.string.ip_stack),
                    style = font16,
                    color = MaterialTheme.colorScheme.primaryTextColor,
                )
            }
            Spacer(modifier = Modifier.padding(8.dp))
            DescriptionWithLearnMore(stringResource(R.string.ip_stack_description), FeatureExplainer.IPV6)
        }
        // Egress dropdown
        Spacer(modifier = Modifier.height(1.dp))
        CustomDropDown(
            R.string.egress,
            state.ipStackEgressModes,
            state.ipStackEgressMode,
            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
        ) {
            actions.onIpStackEgressModeSelected(it)
        }
        // Future: Ingress dropdown (hidden for now)
    }
}

@Composable
private fun ConnectionItem(
    title: Int,
    screen: Screen,
) {
    var showPermissionRequest by remember { mutableStateOf(false) }
    val navController = LocalNavController.current
    if (showPermissionRequest) {
        RequestLocationPermissions {
            // Refresh network detail now that we have location permission
            appContext.deviceStateManager.refreshNetworkDetail()
            navController.navigate(Screen.NetworkOptions.route)
        }
    }
    Row(
        modifier =
            Modifier
                .testTag("connection_item_${screen.route}")
                .background(
                    color =
                        MaterialTheme.colorScheme.primaryTextColor.copy(
                            alpha = 0.05f,
                        ),
                    shape = RoundedCornerShape(size = 12.dp),
                ).hapticClickable {
                    if (screen == Screen.NetworkOptions) {
                        showPermissionRequest = true
                    } else {
                        navController.navigate(screen.route)
                    }
                }.padding(vertical = 14.dp, horizontal = 14.dp),
    ) {
        Text(
            stringResource(title),
            style =
                font16.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primaryTextColor,
                ),
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(com.windscribe.mobile.R.drawable.arrow_right),
            contentDescription = "",
            tint = MaterialTheme.colorScheme.primaryTextColor,
        )
    }
}

@Composable
private fun ConnectionMode(
    state: ConnectionUiState,
    toastMessage: Flow<ToastMessage>,
    actions: ConnectionActions,
) {
    val mode = state.mode
    val shape =
        if (mode == CONNECTION_MODE_MANUAL) {
            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        } else {
            RoundedCornerShape(size = 16.dp)
        }
    val items =
        listOf<Pair<String, Int>>(
            Pair(CONNECTION_MODE_AUTO, R.string.auto),
            Pair(CONNECTION_MODE_MANUAL, R.string.manual),
        )
    Column {
        Header(
            R.string.connection_mode,
            R.string.connection_mode_description,
            com.windscribe.mobile.R.drawable.ic_connection_mode_icon,
            shape,
            items,
            mode,
            explainer = FeatureExplainer.CONNECTION_MODE,
            onModeSelected = {
                actions.onModeSelected(it)
            },
        )
        if (mode == CONNECTION_MODE_MANUAL) {
            Spacer(modifier = Modifier.height(1.dp))
            CustomDropDown(
                R.string.protocol,
                state.protocols,
                state.selectedProtocol,
                onSelect = {
                    actions.onProtocolSelected(it)
                },
                shape = RoundedCornerShape(0.dp),
            )
            Spacer(modifier = Modifier.height(1.dp))
            CustomDropDown(
                R.string.port,
                state.ports,
                state.selectedPort,
                onSelect = {
                    actions.onPortSelected(it)
                },
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
            )
        }
    }
    HandleToast(toastMessage)
}

@Composable
private fun CustomDNS(
    state: ConnectionUiState,
    actions: ConnectionActions,
) {
    val mode = state.dnsMode
    val shape =
        if (mode == DNS_MODE_CUSTOM) {
            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        } else {
            RoundedCornerShape(size = 16.dp)
        }
    val items =
        listOf<Pair<String, Int>>(
            Pair(DNS_MODE_ROBERT, R.string.auto),
            Pair(DNS_MODE_CUSTOM, R.string.manual),
        )
    Column {
        Header(
            R.string.connected_dns,
            R.string.custom_dns_explainer,
            com.windscribe.mobile.R.drawable.custom_dns_icon,
            shape,
            items,
            mode,
            explainer = FeatureExplainer.CUSTOM_DNS_MODE,
            onModeSelected = {
                actions.onDNSModeSelected(it)
            },
        )
        if (mode == DNS_MODE_CUSTOM) {
            Spacer(modifier = Modifier.height(1.dp))
            CustomDNSAddress(state, actions)
        }
    }
}

@Composable
private fun CustomDNSAddress(
    state: ConnectionUiState,
    actions: ConnectionActions,
) {
    val address = state.customDnsAddress
    var enabled by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                ).padding(vertical = 0.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            modifier =
                Modifier
                    .weight(1.0f)
                    .focusRequester(focusRequester),
            enabled = enabled,
            value = address,
            placeholder = {
                Text(
                    stringResource(R.string.custom_dns_address_hint),
                    style = font16,
                    color = MaterialTheme.colorScheme.primaryTextColor,
                )
            },
            textStyle = font16.copy(textAlign = TextAlign.Start),
            singleLine = true,
            colors =
                TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.primaryTextColor,
                    unfocusedTextColor = MaterialTheme.colorScheme.primaryTextColor,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.primaryTextColor,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primaryTextColor,
                    selectionColors =
                        androidx.compose.foundation.text.selection.TextSelectionColors(
                            handleColor = MaterialTheme.colorScheme.primaryTextColor,
                            backgroundColor = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.3f),
                        ),
                ),
            onValueChange = {
                actions.onCustomDNSAddressChanged(it)
            },
        )
        if (enabled) {
            Icon(
                painterResource(com.windscribe.mobile.R.drawable.ic_close_white),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.5f),
                modifier =
                    Modifier
                        .size(24.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (enabled) {
                                enabled = false
                                focusManager.clearFocus()
                            }
                        }.padding(4.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Icon(
            painterResource(if (enabled) com.windscribe.mobile.R.drawable.ic_check else com.windscribe.mobile.R.drawable.ic_edit_icon),
            contentDescription = "",
            tint =
                if (enabled) {
                    AppColors.neonGreen
                } else {
                    MaterialTheme.colorScheme.primaryTextColor.copy(
                        alpha = 0.5f,
                    )
                },
            modifier =
                Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (enabled) {
                            enabled = false
                            actions.onSaveCustomDNSAddress()
                            focusManager.clearFocus()
                        } else {
                            enabled = true
                            focusRequester.requestFocus()
                        }
                    }.padding(4.dp),
        )
    }
}

@Composable
private fun HandleToast(toastMessage: Flow<ToastMessage>) {
    val context = LocalContext.current
    val message by toastMessage.collectAsState(null)
    LaunchedEffect(message) {
        when (val current = message) {
            is ToastMessage.Raw -> {
                Toast
                    .makeText(
                        context,
                        current.message,
                        Toast.LENGTH_SHORT,
                    ).show()
            }

            is ToastMessage.Localized -> {
                Toast
                    .makeText(
                        context,
                        current.message,
                        Toast.LENGTH_SHORT,
                    ).show()
            }

            else -> {}
        }
    }
}

@Composable
private fun AutoPacketSize(
    state: ConnectionUiState,
    actions: ConnectionActions,
) {
    val packetSize = state.packetSize
    var enabled by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var text by remember { mutableStateOf(packetSize.toString()) }
    val activity = LocalActivity.current as? AppStartActivity
    val showToast = remember { mutableStateOf(false) }
    val autoDetecting = state.autoDetecting
    LaunchedEffect(showToast.value) {
        if (showToast.value) {
            Toast.makeText(activity, "Invalid packet size", Toast.LENGTH_SHORT).show()
            showToast.value = false
        }
    }
    LaunchedEffect(packetSize) {
        if (!enabled) {
            text = packetSize.toString()
        }
    }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                ).padding(vertical = 0.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            modifier =
                Modifier
                    .weight(1.0f)
                    .focusRequester(focusRequester),
            enabled = enabled,
            value = text,
            placeholder = {
                Text(
                    stringResource(R.string.packet_size),
                    style = font16,
                    color = MaterialTheme.colorScheme.primaryTextColor,
                )
            },
            textStyle = font16.copy(textAlign = TextAlign.Start),
            singleLine = true,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
            colors =
                TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.primaryTextColor,
                    unfocusedTextColor = MaterialTheme.colorScheme.primaryTextColor,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.primaryTextColor,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primaryTextColor,
                ),
            onValueChange = {
                if (it.all { char -> char.isDigit() }) {
                    text = it
                    val number = it.toIntOrNull()
                    if (number != null && number in 800..2000) {
                        actions.onPacketSizedChanged(number)
                    }
                }
            },
        )
        if (autoDetecting) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = MaterialTheme.colorScheme.primaryTextColor,
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                painterResource(com.windscribe.mobile.R.drawable.ic_auto_detect_icon),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primaryTextColor,
                modifier =
                    Modifier
                        .size(24.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            actions.onAutoDetectClicked()
                        }.padding(4.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            painterResource(if (enabled) com.windscribe.mobile.R.drawable.ic_check else com.windscribe.mobile.R.drawable.ic_edit_icon),
            contentDescription = "",
            tint =
                if (enabled) {
                    AppColors.neonGreen
                } else {
                    MaterialTheme.colorScheme.primaryTextColor.copy(
                        alpha = 0.5f,
                    )
                },
            modifier =
                Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (enabled) {
                            enabled = false
                            focusManager.clearFocus()
                            val number = text.toIntOrNull()
                            if (number != null && number in 800..2000) {
                                actions.onPacketSizeSaved()
                            } else {
                                showToast.value = true
                            }
                        } else {
                            enabled = true
                            focusRequester.requestFocus()
                        }
                    }.padding(4.dp),
        )
    }
}

@Composable
private fun PacketSize(
    state: ConnectionUiState,
    actions: ConnectionActions,
) {
    val mode = state.packetSizeAuto
    val shape =
        if (mode) {
            RoundedCornerShape(size = 16.dp)
        } else {
            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        }
    val items =
        listOf<Pair<Boolean, Int>>(
            Pair(true, R.string.auto),
            Pair(false, R.string.manual),
        )
    Column {
        Header<Boolean>(
            R.string.packet_size,
            R.string.packet_size_description,
            com.windscribe.mobile.R.drawable.ic_packet_icon,
            shape,
            items,
            mode,
            explainer = FeatureExplainer.PACKET_SIZE,
            onModeSelected = {
                actions.onPacketSizeModeSelected(it)
            },
        )
        if (!mode) {
            Spacer(modifier = Modifier.height(1.dp))
            AutoPacketSize(state, actions)
        }
    }
}

@Composable
private fun <T> Header(
    @StringRes title: Int,
    @StringRes description: Int,
    @DrawableRes icon: Int,
    shape: RoundedCornerShape,
    items: List<Pair<T, Int>>,
    selected: T,
    explainer: String,
    onModeSelected: (T) -> Unit = {},
) {
    val expanded = remember { mutableStateOf(false) }
    val selectedItem = items.firstOrNull { it.first == selected } ?: items.first()
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                    shape = shape,
                ).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(icon),
                contentDescription = "",
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primaryTextColor),
            )
            Spacer(modifier = Modifier.padding(8.dp))
            Text(
                stringResource(title),
                style = font16,
                color = MaterialTheme.colorScheme.primaryTextColor,
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier =
                    Modifier
                        .clickable { expanded.value = !expanded.value },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(selectedItem.second),
                        style = font16,
                        color = MaterialTheme.colorScheme.preferencesSubtitleColor,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(id = com.windscribe.mobile.R.drawable.ic_cm_icon),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primaryTextColor,
                    )
                }

                DropdownMenu(
                    expanded = expanded.value,
                    onDismissRequest = { expanded.value = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primaryTextColor),
                ) {
                    items.forEach {
                        DropdownMenuItem(
                            onClick = {
                                expanded.value = false
                                onModeSelected(it.first)
                            },
                            text = {
                                Text(
                                    text = stringResource(it.second),
                                    color = MaterialTheme.colorScheme.backgroundColor,
                                    style = font16,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            },
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.padding(8.dp))
        DescriptionWithLearnMore(stringResource(description), explainer)
    }
}

@Composable
private fun AlwaysOnVPN() {
    val context = LocalContext.current
    Column(
        modifier =
            Modifier
                .background(
                    MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                ).padding(14.dp)
                .fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth(),
        ) {
            Image(
                painter = painterResource(com.windscribe.mobile.R.drawable.ic_always_on_icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primaryTextColor),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.always_on),
                style = font16.copy(fontWeight = FontWeight.Medium, textAlign = TextAlign.Start),
                color = MaterialTheme.colorScheme.primaryTextColor,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.open_settings),
                style = font14.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.preferencesSubtitleColor,
                textAlign = TextAlign.Start,
                maxLines = 1,
                modifier =
                    Modifier.clickable {
                        val intent = Intent("android.net.vpn.SETTINGS")
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        }
                    },
            )
        }
        Spacer(modifier = Modifier.height(13.5.dp))
        Text(
            text = stringResource(R.string.always_on_warning),
            style = font14.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.preferencesSubtitleColor,
            textAlign = TextAlign.Start,
        )
    }
}

/**
 * Feeds representative state into the preview so the renderer draws [ConnectionContent] without a VM.
 */
private class ConnectionStateProvider : PreviewParameterProvider<ConnectionUiState> {
    override val values =
        sequenceOf(
            ConnectionUiState(),
            ConnectionUiState(
                mode = CONNECTION_MODE_MANUAL,
                decoyTraffic = true,
                packetSizeAuto = false,
            ),
        )
}

@Composable
@MultiDevicePreview
private fun ConnectionContentPreview(
    @PreviewParameter(ConnectionStateProvider::class) state: ConnectionUiState,
) {
    PreviewWithNav {
        ConnectionContent(
            state = state,
            toastMessage = emptyFlow(),
            actions = ConnectionActions(),
        )
    }
}
