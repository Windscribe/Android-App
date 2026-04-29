package com.windscribe.mobile.ui.preferences.anticensorship

import PreferencesNavBar
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.common.CustomDropDown
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.common.SwitchItemView
import com.windscribe.mobile.ui.connection.ToastMessage
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.model.DropDownStringItem
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.font14
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.constants.FeatureExplainer
import kotlinx.coroutines.delay

@Composable
fun AntiCensorshipScreen(viewModel: AntiCensorshipViewModel? = null) {
    val navController = LocalNavController.current
    val protocolTweaksModes by viewModel?.protocolTweaksModes?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) }
    val selectedProtocolTweaksMode by viewModel?.selectedProtocolTweaksMode?.collectAsState()
        ?: remember { mutableStateOf("auto") }
    val serverRoutingModes by viewModel?.serverRoutingModes?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) }
    val selectedServerRouting by viewModel?.selectedServerRouting?.collectAsState()
        ?: remember { mutableStateOf("Auto") }
    val amneziaPresets by viewModel?.amneziaPresets?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) }
    val selectedPreset by viewModel?.selectedPreset?.collectAsState()
        ?: remember { mutableStateOf("") }
    val extraTlsPaddingEnabled by viewModel?.extraTlsPaddingEnabled?.collectAsState()
        ?: remember { mutableStateOf(true) }

    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel?.refreshPreferences()
    }

    PreferenceBackground {
        Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)) {
            PreferencesNavBar(stringResource(com.windscribe.vpn.R.string.anti_censorship_screen_title)) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.height(20.dp))
            ScreenDescription()
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .navigationBarsPadding()
                    .verticalScroll(scrollState)
            ) {
                // Protocol Tweaks Section
                ProtocolTweaksSection(
                    protocolTweaksModes,
                    selectedProtocolTweaksMode,
                    amneziaPresets,
                    selectedPreset,
                    viewModel
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Server Routing Section
                ServerRoutingSection(
                    serverRoutingModes,
                    selectedServerRouting,
                    viewModel
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Extra TLS Padding Section
                ExtraTlsPaddingSection(
                    extraTlsPaddingEnabled,
                    viewModel
                )
            }
        }
    }
}

@Composable
private fun ScreenDescription() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.10f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {
        Text(
            text = stringResource(com.windscribe.vpn.R.string.anti_censorship_screen_description),
            style = font14.copy(textAlign = TextAlign.Start),
            color = MaterialTheme.colorScheme.preferencesSubtitleColor
        )
    }
}

@Composable
private fun ProtocolTweaksSection(
    modes: List<DropDownStringItem>,
    selectedMode: String,
    presets: List<DropDownStringItem>,
    selectedPreset: String,
    viewModel: AntiCensorshipViewModel?
) {
    val isEnabledMode = selectedMode == "manual"

    Column {
        // Protocol Tweaks Mode Dropdown
        CustomDropDown(
            com.windscribe.vpn.R.string.protocol_tweaks,
            modes,
            selectedMode,
            description = com.windscribe.vpn.R.string.protocol_tweaks_description,
            textAlign = TextAlign.Start,
            shape = if (isEnabledMode) {
                RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            } else {
                RoundedCornerShape(12.dp)
            },
        ) {
            viewModel?.onProtocolTweaksModeSelected(it.key)
        }

        // Amnezia Preset Dropdown (only shown when Enabled mode is selected)
        if (isEnabledMode && presets.isNotEmpty()) {
            Spacer(modifier = Modifier.height(1.dp))
            CustomDropDown(
                com.windscribe.vpn.R.string.amnezia_preset,
                presets,
                selectedPreset,
                textAlign = TextAlign.Start,
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
            ) {
                viewModel?.onAmneziaPresetSelected(it.key)
            }
        }
    }
}

@Composable
private fun ExtraTlsPaddingSection(
    enabled: Boolean,
    viewModel: AntiCensorshipViewModel?
) {
    SwitchItemView(
        title = com.windscribe.vpn.R.string.extra_tls_padding,
        icon = R.drawable.ic_anti_censorship_icon,
        description = com.windscribe.vpn.R.string.extra_tls_padding_description,
        enabled,
        shape = RoundedCornerShape(12.dp),
        onSelect = { viewModel?.onExtraTlsPaddingToggled() }
    )
}

@Composable
private fun ServerRoutingSection(
    modes: List<DropDownStringItem>,
    selectedMode: String,
    viewModel: AntiCensorshipViewModel?
) {
    if (modes.isNotEmpty()) {
        CustomDropDown(
            com.windscribe.vpn.R.string.server_routing,
            modes,
            selectedMode,
            description = com.windscribe.vpn.R.string.server_routing_description,
            textAlign = TextAlign.Start,
            shape = RoundedCornerShape(12.dp),
        ) {
            viewModel?.onServerRoutingSelected(it.key)
        }
    }
}

@Composable
@MultiDevicePreview
private fun AntiCensorshipScreenPreview() {
    PreviewWithNav {
        AntiCensorshipScreen()
    }
}
