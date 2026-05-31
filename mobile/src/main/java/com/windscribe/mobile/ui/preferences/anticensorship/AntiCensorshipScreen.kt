package com.windscribe.mobile.ui.preferences.anticensorship

import PreferencesNavBar
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.common.CustomDropDown
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.common.StyledTextField
import com.windscribe.mobile.ui.common.SwitchItemView
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.model.DropDownStringItem
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.font14
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor
import com.windscribe.mobile.ui.theme.primaryTextColor

/**
 * Snapshot of the anti-censorship UI state. Hoisted so the stateless [AntiCensorshipContent]
 * never touches [AntiCensorshipViewModel] — previews feed it directly.
 */
data class AntiCensorshipState(
    val protocolTweaksModes: List<DropDownStringItem> = emptyList(),
    val selectedProtocolTweaksMode: String = "auto",
    val serverRoutingModes: List<DropDownStringItem> = emptyList(),
    val selectedServerRouting: String = "Auto",
    val amneziaPresets: List<DropDownStringItem> = emptyList(),
    val selectedPreset: String = "",
    val extraTlsPaddingEnabled: Boolean = true,
    val tlsServerName: String = "",
)

/**
 * Callbacks the anti-censorship UI can raise.
 */
class AntiCensorshipActions(
    val onProtocolTweaksModeSelected: (String) -> Unit = {},
    val onAmneziaPresetSelected: (String) -> Unit = {},
    val onServerRoutingSelected: (String) -> Unit = {},
    val onExtraTlsPaddingToggled: () -> Unit = {},
    val onTlsServerNameChanged: (String) -> Unit = {},
)

/**
 * Stateful entry point. Owns the [AntiCensorshipViewModel], collects its flows and wires up
 * side effects, then delegates rendering to [AntiCensorshipContent].
 */
@Composable
fun AntiCensorshipScreen(viewModel: AntiCensorshipViewModel = hiltViewModel<AntiCensorshipViewModelImpl>()) {
    val protocolTweaksModes by viewModel.protocolTweaksModes.collectAsState()
    val selectedProtocolTweaksMode by viewModel.selectedProtocolTweaksMode.collectAsState()
    val serverRoutingModes by viewModel.serverRoutingModes.collectAsState()
    val selectedServerRouting by viewModel.selectedServerRouting.collectAsState()
    val amneziaPresets by viewModel.amneziaPresets.collectAsState()
    val selectedPreset by viewModel.selectedPreset.collectAsState()
    val extraTlsPaddingEnabled by viewModel.extraTlsPaddingEnabled.collectAsState()
    val tlsServerName by viewModel.tlsServerName.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshPreferences()
    }

    AntiCensorshipContent(
        state =
            AntiCensorshipState(
                protocolTweaksModes = protocolTweaksModes,
                selectedProtocolTweaksMode = selectedProtocolTweaksMode,
                serverRoutingModes = serverRoutingModes,
                selectedServerRouting = selectedServerRouting,
                amneziaPresets = amneziaPresets,
                selectedPreset = selectedPreset,
                extraTlsPaddingEnabled = extraTlsPaddingEnabled,
                tlsServerName = tlsServerName,
            ),
        actions =
            AntiCensorshipActions(
                onProtocolTweaksModeSelected = viewModel::onProtocolTweaksModeSelected,
                onAmneziaPresetSelected = viewModel::onAmneziaPresetSelected,
                onServerRoutingSelected = viewModel::onServerRoutingSelected,
                onExtraTlsPaddingToggled = viewModel::onExtraTlsPaddingToggled,
                onTlsServerNameChanged = viewModel::onTlsServerNameChanged,
            ),
    )
}

/**
 * Stateless anti-censorship UI. Everything it needs is passed in, so it renders identically in
 * the app and in `@Preview`. This is the composable previews target.
 */
@Composable
fun AntiCensorshipContent(
    state: AntiCensorshipState,
    actions: AntiCensorshipActions,
) {
    val navController = LocalNavController.current
    val scrollState = rememberScrollState()

    PreferenceBackground {
        Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)) {
            PreferencesNavBar(stringResource(com.windscribe.vpn.R.string.anti_censorship_screen_title)) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.height(20.dp))
            ScreenDescription()
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .navigationBarsPadding()
                        .verticalScroll(scrollState),
            ) {
                // Protocol Tweaks Section
                ProtocolTweaksSection(
                    state.protocolTweaksModes,
                    state.selectedProtocolTweaksMode,
                    state.amneziaPresets,
                    state.selectedPreset,
                    actions.onProtocolTweaksModeSelected,
                    actions.onAmneziaPresetSelected,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Server Routing Section
                ServerRoutingSection(
                    state.serverRoutingModes,
                    state.selectedServerRouting,
                    actions.onServerRoutingSelected,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Extra TLS Padding Section
                ExtraTlsPaddingSection(
                    state.extraTlsPaddingEnabled,
                    actions.onExtraTlsPaddingToggled,
                )

                // TLS Server Name Section - hidden for now
                // Spacer(modifier = Modifier.height(16.dp))
                // TlsServerNameSection(
                //     state.tlsServerName,
                //     actions.onTlsServerNameChanged,
                // )
            }
        }
    }
}

@Composable
private fun ScreenDescription() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(12.dp),
                ).padding(14.dp),
    ) {
        Text(
            text = stringResource(com.windscribe.vpn.R.string.anti_censorship_screen_description),
            style = font14.copy(textAlign = TextAlign.Start),
            color = MaterialTheme.colorScheme.preferencesSubtitleColor,
        )
    }
}

@Composable
private fun ProtocolTweaksSection(
    modes: List<DropDownStringItem>,
    selectedMode: String,
    presets: List<DropDownStringItem>,
    selectedPreset: String,
    onProtocolTweaksModeSelected: (String) -> Unit,
    onAmneziaPresetSelected: (String) -> Unit,
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
            shape =
                if (isEnabledMode) {
                    RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                } else {
                    RoundedCornerShape(12.dp)
                },
        ) {
            onProtocolTweaksModeSelected(it.key)
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
                onAmneziaPresetSelected(it.key)
            }
        }
    }
}

@Composable
private fun ExtraTlsPaddingSection(
    enabled: Boolean,
    onExtraTlsPaddingToggled: () -> Unit,
) {
    SwitchItemView(
        title = com.windscribe.vpn.R.string.extra_tls_padding,
        icon = R.drawable.ic_anti_censorship_icon,
        description = com.windscribe.vpn.R.string.extra_tls_padding_description,
        enabled,
        shape = RoundedCornerShape(12.dp),
        onSelect = { onExtraTlsPaddingToggled() },
    )
}

@Composable
private fun ServerRoutingSection(
    modes: List<DropDownStringItem>,
    selectedMode: String,
    onServerRoutingSelected: (String) -> Unit,
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
            onServerRoutingSelected(it.key)
        }
    }
}

// Hidden for now - kept for easy re-enabling. See AntiCensorshipContent.
@Suppress("unused")
@Composable
private fun TlsServerNameSection(
    value: String,
    onTlsServerNameChanged: (String) -> Unit,
) {
    Column {
        Text(
            text = "TLS Server Name",
            style = font14,
            color = MaterialTheme.colorScheme.primaryTextColor,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        StyledTextField(
            value = value,
            onValueChange = onTlsServerNameChanged,
            placeholder = "Enter TLS server name (optional)",
        )
    }
}

/**
 * Feeds a representative [AntiCensorshipState] into the preview.
 */
private class AntiCensorshipStateProvider : PreviewParameterProvider<AntiCensorshipState> {
    override val values =
        sequenceOf(
            AntiCensorshipState(
                protocolTweaksModes =
                    listOf(
                        DropDownStringItem("auto", "Auto"),
                        DropDownStringItem("manual", "Enabled"),
                        DropDownStringItem("disabled", "Disabled"),
                    ),
                selectedProtocolTweaksMode = "auto",
                serverRoutingModes =
                    listOf(
                        DropDownStringItem("auto", "Auto"),
                        DropDownStringItem("regular", "Regular"),
                        DropDownStringItem("alternate", "Alternate"),
                    ),
                selectedServerRouting = "auto",
                extraTlsPaddingEnabled = true,
            ),
        )
}

@Composable
@MultiDevicePreview
private fun AntiCensorshipContentPreview(
    @PreviewParameter(AntiCensorshipStateProvider::class) state: AntiCensorshipState,
) {
    PreviewWithNav {
        AntiCensorshipContent(
            state = state,
            actions = AntiCensorshipActions(),
        )
    }
}
