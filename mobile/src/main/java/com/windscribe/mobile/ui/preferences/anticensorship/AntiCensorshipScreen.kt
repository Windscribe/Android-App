package com.windscribe.mobile.ui.preferences.anticensorship

import PreferencesNavBar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.common.CustomDropDown
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.common.SwitchItemView
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.model.DropDownStringItem
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font14
import com.windscribe.mobile.ui.theme.font16
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

                Spacer(modifier = Modifier.height(16.dp))
                TlsServerNameSection(
                    state.tlsServerName,
                    actions.onTlsServerNameChanged,
                )
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
                ).padding(12.dp),
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

@Composable
private fun TlsServerNameSection(
    value: String,
    onTlsServerNameChanged: (String) -> Unit,
) {
    var enabled by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Column {
        // Header section with title and description
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    ).padding(12.dp),
        ) {
            Text(
                stringResource(com.windscribe.vpn.R.string.tls_server_name),
                style = font16,
                color = MaterialTheme.colorScheme.primaryTextColor,
            )
            Spacer(modifier = Modifier.padding(8.dp))
            Text(
                stringResource(com.windscribe.vpn.R.string.tls_server_name_description),
                style = font14.copy(textAlign = TextAlign.Start, fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.preferencesSubtitleColor,
            )
        }

        Spacer(modifier = Modifier.height(1.dp))

        // Text field section with edit controls
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                    ).padding(horizontal = 16.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                modifier =
                    Modifier
                        .weight(1.0f)
                        .focusRequester(focusRequester),
                enabled = enabled,
                value = value,
                placeholder = {
                    Text(
                        stringResource(com.windscribe.vpn.R.string.tls_server_name_hint),
                        style = font14,
                        color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.6f),
                    )
                },
                textStyle = font14.copy(textAlign = TextAlign.Start),
                singleLine = true,
                colors =
                    TextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.primaryTextColor,
                        unfocusedTextColor = MaterialTheme.colorScheme.primaryTextColor,
                        disabledTextColor = MaterialTheme.colorScheme.primaryTextColor,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.6f),
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
                    onTlsServerNameChanged(it)
                },
            )
            if (enabled) {
                Icon(
                    painterResource(R.drawable.ic_close_white),
                    contentDescription = "",
                    tint = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.5f),
                    modifier =
                        Modifier
                            .size(24.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                enabled = false
                                focusManager.clearFocus()
                            }.padding(4.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Icon(
                painterResource(if (enabled) R.drawable.ic_check else R.drawable.ic_edit_icon),
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
                            } else {
                                enabled = true
                                focusRequester.requestFocus()
                            }
                        }.padding(4.dp),
            )
        }
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
