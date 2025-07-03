package com.windscribe.mobile.ui.preferences.connection

import PreferencesNavBar
import android.R.attr.description
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import com.windscribe.vpn.constants.FeatureExplainer
import com.windscribe.vpn.constants.PreferencesKeyConstants.CONNECTION_MODE_AUTO
import com.windscribe.vpn.constants.PreferencesKeyConstants.CONNECTION_MODE_MANUAL
import com.windscribe.vpn.constants.PreferencesKeyConstants.DNS_MODE_CUSTOM
import com.windscribe.vpn.constants.PreferencesKeyConstants.DNS_MODE_ROBERT
import com.windscribe.vpn.mocklocation.MockLocationManager.Companion.isAppSelectedInMockLocationList
import com.windscribe.vpn.mocklocation.MockLocationManager.Companion.isDevModeOn


@Composable
fun ConnectionScreen(viewModel: ConnectionViewModel? = null) {
    val navController = LocalNavController.current
    val autoConnect by viewModel?.autoConnect?.collectAsState()
        ?: remember { mutableStateOf(false) }
    val allowLan by viewModel?.allowLan?.collectAsState() ?: remember { mutableStateOf(false) }
    val startOnBoot by viewModel?.startOnBoot?.collectAsState()
        ?: remember { mutableStateOf(false) }
    val gpsSpoofing by viewModel?.gpsSpoofing?.collectAsState()
        ?: remember { mutableStateOf(false) }
    val antiCensorship by viewModel?.antiCensorship?.collectAsState() ?: remember {
        mutableStateOf(
            false
        )
    }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel?.refreshPreferences()
    }
    PreferenceBackground {
        Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)) {
            PreferencesNavBar(stringResource(R.string.connection)) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.height(20.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                ConnectionItem(
                    R.string.network_options,
                    Screen.NetworkOptions
                )
                Spacer(modifier = Modifier.height(16.dp))
                ConnectionItem(
                    R.string.split_tunneling,
                    Screen.SplitTunnel
                )
                Spacer(modifier = Modifier.height(16.dp))
                AlwaysOnVPN()
                Spacer(modifier = Modifier.height(16.dp))
                SwitchItemView(
                    title = R.string.auto_connect,
                    icon = com.windscribe.mobile.R.drawable.auto_connect,
                    description = R.string.auto_connect_explainer,
                    autoConnect,
                    onSelect = { viewModel?.onAutoConnectToggleClicked() }
                )
                Spacer(modifier = Modifier.height(16.dp))
                ConnectionMode(viewModel)
                Spacer(modifier = Modifier.height(16.dp))
                PacketSize(viewModel)
                Spacer(modifier = Modifier.height(16.dp))
                CustomDNS(viewModel)
                Spacer(modifier = Modifier.height(16.dp))
                SwitchItemView(
                    title = R.string.lan_by_pass,
                    icon = com.windscribe.mobile.R.drawable.ic_lan_icon,
                    description = R.string.allow_lan_description,
                    allowLan,
                    explainer = FeatureExplainer.ALLOW_LAN,
                    onSelect = { viewModel?.onAllowLanToggleClicked() }
                )
                Spacer(modifier = Modifier.height(16.dp))
                SwitchItemView(
                    title = R.string.start_on_boot,
                    icon = com.windscribe.mobile.R.drawable.ic_auto_connect_boot,
                    description = R.string.auto_connect_on_boot_description,
                    startOnBoot,
                    onSelect = { viewModel?.onStartOnBootToggleClicked() }
                )
                Spacer(modifier = Modifier.height(16.dp))
                SwitchItemView(
                    title = R.string.gps_spoofing,
                    icon = com.windscribe.mobile.R.drawable.ic_gps_spoof_icon,
                    description = R.string.gps_spoofing_description,
                    gpsSpoofing,
                    explainer = FeatureExplainer.GPS_SPOOFING,
                    onSelect = {
                        if (gpsSpoofing.not() && (!isDevModeOn(context) || !isAppSelectedInMockLocationList(
                                context
                            ))
                        ) {
                            navController.navigate(Screen.GpsSpoofing.route)
                        } else {
                            viewModel?.onGPSSpoofingToggleClicked()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                DecoyTrafficMode(viewModel)
                Spacer(modifier = Modifier.height(16.dp))
                SwitchItemView(
                    title = R.string.anti_censorship,
                    icon = com.windscribe.mobile.R.drawable.ic_anti_censorship_icon,
                    description = R.string.anti_censorship_explainer,
                    antiCensorship,
                    explainer = FeatureExplainer.CIRCUMVENT_CENSORSHIP,
                    onSelect = { viewModel?.onAntiCensorshipToggleClicked() }
                )
            }
        }
    }
}

@Composable
private fun DecoyTrafficMode(viewModel: ConnectionViewModel?) {
    val navController = LocalNavController.current
    val decoyTraffic by viewModel?.decoyTraffic?.collectAsState()
        ?: remember { mutableStateOf(false) }
    val multiplier by viewModel?.trafficMultiplier?.collectAsState() ?: remember {
        mutableStateOf(
            DropDownStringItem("")
        )
    }
    val multipliers by viewModel?.trafficMultipliers?.collectAsState() ?: remember {
        mutableStateOf(
            emptyList<DropDownStringItem>()
        )
    }
    val potentialDataUse by viewModel?.potentialDataUse?.collectAsState()
        ?: remember { mutableStateOf("") }
    val shape = if (decoyTraffic) {
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
                    viewModel?.onDecoyTrafficToggleClicked()
                }
            }
        )
        if (decoyTraffic) {
            Spacer(modifier = Modifier.height(1.dp))
            CustomDropDown(
                R.string.fake_traffic_volume,
                multipliers,
                multiplier.toString(),
                shape = RoundedCornerShape(0.dp),
            ) {
                viewModel?.onFakeTrafficVolumeSelected(it)
            }
            Spacer(modifier = Modifier.height(1.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                    )
                    .padding(14.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.potential_data_use),
                    style = font16.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primaryTextColor
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = potentialDataUse,
                    style = font16,
                    color = MaterialTheme.colorScheme.preferencesSubtitleColor
                )
            }
        }
    }
}

@Composable
private fun ConnectionItem(title: Int, screen: Screen) {
    var showPermissionRequest by remember { mutableStateOf(false) }
    val navController = LocalNavController.current
    if (showPermissionRequest) {
        RequestLocationPermissions {
            showPermissionRequest = false
            navController.navigate(Screen.NetworkOptions.route)
        }
    }
    Row(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primaryTextColor.copy(
                    alpha = 0.05f
                ), shape = RoundedCornerShape(size = 12.dp)
            )
            .hapticClickable {
                if (screen == Screen.NetworkOptions) {
                    showPermissionRequest = true
                } else {
                    navController.navigate(screen.route)
                }
            }
            .padding(vertical = 14.dp, horizontal = 14.dp)
    ) {
        Text(
            stringResource(title),
            style = font16.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primaryTextColor
            )
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(com.windscribe.mobile.R.drawable.arrow_right),
            contentDescription = "",
            tint = MaterialTheme.colorScheme.primaryTextColor
        )
    }
}

@Composable
private fun ConnectionMode(viewModel: ConnectionViewModel?) {
    val protocols by viewModel?.protocols?.collectAsState() ?: remember {
        mutableStateOf(
            listOf(
                DropDownStringItem("IKEv2")
            )
        )
    }
    val ports by viewModel?.ports?.collectAsState() ?: remember {
        mutableStateOf(
            listOf(
                DropDownStringItem("500")
            )
        )
    }
    val selectedProtocol by viewModel?.selectedProtocol?.collectAsState()
        ?: remember { mutableStateOf("IKEv2") }
    val selectedPort by viewModel?.selectedPort?.collectAsState()
        ?: remember { mutableStateOf("500") }
    val mode by viewModel?.mode?.collectAsState()
        ?: remember { mutableStateOf(CONNECTION_MODE_AUTO) }
    val shape = if (mode == CONNECTION_MODE_MANUAL) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    } else {
        RoundedCornerShape(size = 16.dp)
    }
    val items = listOf<Pair<String, Int>>(
        Pair(CONNECTION_MODE_AUTO, R.string.auto),
        Pair(CONNECTION_MODE_MANUAL, R.string.manual)
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
                viewModel?.onModeSelected(it)
            }
        )
        if (mode == CONNECTION_MODE_MANUAL) {
            Spacer(modifier = Modifier.height(1.dp))
            CustomDropDown(
                R.string.protocol,
                protocols,
                selectedProtocol,
                onSelect = {
                    viewModel?.onProtocolSelected(it)
                }, shape = RoundedCornerShape(0.dp)
            )
            Spacer(modifier = Modifier.height(1.dp))
            CustomDropDown(
                R.string.port,
                ports,
                selectedPort,
                onSelect = {
                    viewModel?.onPortSelected(it)
                }, shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
            )
        }
    }
    HandleToast(viewModel)
}

@Composable
private fun CustomDNS(viewModel: ConnectionViewModel?) {
    val mode by viewModel?.dnsMode?.collectAsState() ?: remember { mutableStateOf(DNS_MODE_CUSTOM) }
    val shape = if (mode == DNS_MODE_CUSTOM) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    } else {
        RoundedCornerShape(size = 16.dp)
    }
    val items = listOf<Pair<String, Int>>(
        Pair(DNS_MODE_ROBERT, R.string.auto),
        Pair(DNS_MODE_CUSTOM, R.string.manual)
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
                viewModel?.onDNSModeSelected(it)
            }
        )
        if (mode == DNS_MODE_CUSTOM) {
            Spacer(modifier = Modifier.height(1.dp))
            CustomDNSAddress(viewModel)
        }
    }
}

@Composable
private fun CustomDNSAddress(viewModel: ConnectionViewModel?) {
    val address by viewModel?.customDnsAddress?.collectAsState()
        ?: remember { mutableStateOf("1.1.1.1") }
    var enabled by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            )
            .padding(vertical = 0.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            modifier = Modifier
                .weight(1.0f)
                .focusRequester(focusRequester),
            enabled = enabled,
            value = address,
            placeholder = {
                Text(
                    stringResource(R.string.custom_dns_address_hint),
                    style = font16,
                    color = MaterialTheme.colorScheme.primaryTextColor
                )
            },
            textStyle = font16.copy(textAlign = TextAlign.Start),
            singleLine = true,
            colors = TextFieldDefaults.colors(
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
                viewModel?.onCustomDNSAddressChanged(it)
            })
        if (enabled) {
            Icon(
                painterResource(com.windscribe.mobile.R.drawable.ic_close_white),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (enabled) {
                            enabled = false
                            focusManager.clearFocus()
                        }
                    }
                    .padding(4.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Icon(
            painterResource(if (enabled) com.windscribe.mobile.R.drawable.ic_check else com.windscribe.mobile.R.drawable.ic_edit_icon),
            contentDescription = "",
            tint = if (enabled) AppColors.neonGreen else MaterialTheme.colorScheme.primaryTextColor.copy(
                alpha = 0.5f
            ),
            modifier = Modifier
                .size(24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (enabled) {
                        enabled = false
                        viewModel?.saveCustomDNSAddress()
                        focusManager.clearFocus()
                    } else {
                        enabled = true
                        focusRequester.requestFocus()
                    }
                }
                .padding(4.dp)
        )
    }
}

@Composable
private fun HandleToast(viewModel: ConnectionViewModel?) {
    val context = LocalContext.current
    val toastMessage by viewModel?.toastMessage?.collectAsState("")
        ?: remember { mutableStateOf("") }
    LaunchedEffect(toastMessage) {
        when (toastMessage) {
            is ToastMessage.Raw -> {
                Toast.makeText(
                    context,
                    (toastMessage as ToastMessage.Raw).message,
                    Toast.LENGTH_SHORT
                ).show()
            }

            is ToastMessage.Localized -> {
                Toast.makeText(
                    context,
                    (toastMessage as ToastMessage.Localized).message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

@Composable
private fun AutoPacketSize(viewModel: ConnectionViewModel?) {
    val packetSize by viewModel?.packetSize?.collectAsState()
        ?: remember { mutableStateOf(1500) }
    var enabled by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var text by remember { mutableStateOf(packetSize.toString()) }
    val activity = LocalContext.current as? AppStartActivity
    var showToast = remember { mutableStateOf(false) }
    val autoDetecting by viewModel?.autoDetecting?.collectAsState() ?: remember {
        mutableStateOf(
            false
        )
    }
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
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            )
            .padding(vertical = 0.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            modifier = Modifier
                .weight(1.0f)
                .focusRequester(focusRequester),
            enabled = enabled,
            value = text,
            placeholder = {
                Text(
                    stringResource(R.string.packet_size),
                    style = font16,
                    color = MaterialTheme.colorScheme.primaryTextColor
                )
            },
            textStyle = font16.copy(textAlign = TextAlign.Start),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            colors = TextFieldDefaults.colors(
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
                        viewModel?.onPacketSizedChanged(number)
                    }
                }
            },
        )
        if (autoDetecting) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = MaterialTheme.colorScheme.primaryTextColor,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                painterResource(com.windscribe.mobile.R.drawable.ic_auto_detect_icon),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primaryTextColor,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        viewModel?.onAutoDetectClicked()
                    }
                    .padding(4.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            painterResource(if (enabled) com.windscribe.mobile.R.drawable.ic_check else com.windscribe.mobile.R.drawable.ic_edit_icon),
            contentDescription = "",
            tint = if (enabled) AppColors.neonGreen else MaterialTheme.colorScheme.primaryTextColor.copy(
                alpha = 0.5f
            ),
            modifier = Modifier
                .size(24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (enabled) {
                        enabled = false
                        focusManager.clearFocus()
                        val number = text.toIntOrNull()
                        if (number != null && number in 800..2000) {
                            viewModel?.onPacketSizeSaved()
                        } else {
                            showToast.value = true
                        }
                    } else {
                        enabled = true
                        focusRequester.requestFocus()
                    }
                }
                .padding(4.dp)
        )
    }
}

@Composable
private fun PacketSize(viewModel: ConnectionViewModel?) {
    val mode by viewModel?.packetSizeAuto?.collectAsState() ?: remember { mutableStateOf(true) }
    val shape = if (mode) {
        RoundedCornerShape(size = 16.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    }
    val items = listOf<Pair<Boolean, Int>>(
        Pair(true, R.string.auto),
        Pair(false, R.string.manual)
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
                viewModel?.onPacketSizeModeSelected(it)
            }
        )
        if (!mode) {
            Spacer(modifier = Modifier.height(1.dp))
            AutoPacketSize(viewModel)
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
    onModeSelected: (T) -> Unit = {}
) {
    val expanded = remember { mutableStateOf(false) }
    val selectedItem = items.firstOrNull { it.first == selected } ?: items.first()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                shape = shape
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(icon),
                contentDescription = "",
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primaryTextColor)
            )
            Spacer(modifier = Modifier.padding(8.dp))
            Text(
                stringResource(title),
                style = font16,
                color = MaterialTheme.colorScheme.primaryTextColor
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clickable { expanded.value = !expanded.value }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(selectedItem.second),
                        style = font16,
                        color = MaterialTheme.colorScheme.preferencesSubtitleColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(id = com.windscribe.mobile.R.drawable.ic_cm_icon),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primaryTextColor
                    )
                }

                DropdownMenu(
                    expanded = expanded.value,
                    onDismissRequest = { expanded.value = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primaryTextColor)
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
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
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
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Image(
                painter = painterResource(com.windscribe.mobile.R.drawable.ic_always_on_icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primaryTextColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.always_on),
                style = font16.copy(fontWeight = FontWeight.Medium, textAlign = TextAlign.Start),
                color = MaterialTheme.colorScheme.primaryTextColor,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.open_settings),
                style = font14.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.preferencesSubtitleColor,
                textAlign = TextAlign.Start,
                maxLines = 1,
                modifier = Modifier.clickable {
                    val intent = Intent("android.net.vpn.SETTINGS")
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(13.5.dp))
        Text(
            text = stringResource(R.string.always_on_warning),
            style = font14.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.preferencesSubtitleColor,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
@MultiDevicePreview
private fun ConnectionScreenPreview() {
    PreviewWithNav {
        ConnectionScreen()
    }
}