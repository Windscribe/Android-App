package com.windscribe.mobile.ui.preferences.split_tunnel

import PreferencesNavBar
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.windscribe.mobile.ui.common.CustomDropDown
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.common.PreferenceProgressBar
import com.windscribe.mobile.ui.common.SwitchItemView
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.model.DropDownStringItem
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.preferencesBackgroundColor
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.R
import com.windscribe.vpn.api.response.InstalledAppsData
import com.windscribe.vpn.cache.AppIconCache
import com.windscribe.vpn.constants.FeatureExplainer

/**
 * Snapshot of the split-tunnel UI state. Hoisted so the stateless [SplitTunnelContent] never
 * touches [SplitTunnelViewModel] — previews feed it directly.
 */
data class SplitTunnelState(
    val showProgress: Boolean = false,
    val modes: List<DropDownStringItem> = emptyList(),
    val selectedModeKey: String = "",
    val isSplitTunnelEnabled: Boolean = false,
    val showSystemApps: Boolean = false,
    val searchKeyword: String = "",
    val filteredApps: List<InstalledAppsData> = emptyList(),
    val appIconCache: AppIconCache? = null,
    val isAndroid13Plus: Boolean = false,
)

/**
 * Callbacks the split-tunnel UI can raise.
 */
class SplitTunnelActions(
    val onModeSelected: (DropDownStringItem) -> Unit = {},
    val onAppSelected: (InstalledAppsData) -> Unit = {},
    val onSplitTunnelSettingChanged: () -> Unit = {},
    val onQueryTextChange: (String) -> Unit = {},
    val onShowSystemAppsToggle: () -> Unit = {},
    val onManageDomainsIps: () -> Unit = {},
)

/**
 * Stateful entry point. Owns the [SplitTunnelViewModel], collects its flows, then delegates
 * rendering to [SplitTunnelContent].
 */
@Composable
fun SplitTunnelScreen(viewModel: SplitTunnelViewModel = hiltViewModel<SplitTunnelViewModelImpl>()) {
    val navController = LocalNavController.current
    val showProgress by viewModel.showProgress.collectAsState()
    val selectedModeKey by viewModel.selectedModeKey.collectAsState()
    val isSplitTunnelEnabled by viewModel.isSplitTunnelEnabled.collectAsState()
    val showSystemApps by viewModel.showSystemApps.collectAsState()
    val searchKeyword by viewModel.searchKeyword.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()

    SplitTunnelContent(
        state =
            SplitTunnelState(
                showProgress = showProgress,
                modes = viewModel.modes,
                selectedModeKey = selectedModeKey,
                isSplitTunnelEnabled = isSplitTunnelEnabled,
                showSystemApps = showSystemApps,
                searchKeyword = searchKeyword,
                filteredApps = filteredApps,
                appIconCache = viewModel.appIconCache,
                isAndroid13Plus = viewModel.isAndroid13Plus,
            ),
        actions =
            SplitTunnelActions(
                onModeSelected = viewModel::onModeSelected,
                onAppSelected = viewModel::onAppSelected,
                onSplitTunnelSettingChanged = viewModel::onSplitTunnelSettingChanged,
                onQueryTextChange = viewModel::onQueryTextChange,
                onShowSystemAppsToggle = viewModel::onShowSystemAppsToggle,
                onManageDomainsIps = {
                    navController.navigate("excluded_ips_domains")
                },
            ),
    )
}

/**
 * Stateless split-tunnel UI. Everything it needs is passed in, so it renders identically in the
 * app and in `@Preview`. This is the composable previews target.
 */
@Composable
fun SplitTunnelContent(
    state: SplitTunnelState,
    actions: SplitTunnelActions,
) {
    val navController = LocalNavController.current
    PreferenceBackground {
        Column(
            modifier =
                Modifier
                    .testTag("split_tunnel_screen")
                    .padding(vertical = 16.dp, horizontal = 16.dp)
                    .navigationBarsPadding(),
        ) {
            PreferencesNavBar(stringResource(R.string.split_tunneling)) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.height(20.dp))
            Mode(
                state.modes,
                state.selectedModeKey,
                state.isSplitTunnelEnabled,
                actions.onSplitTunnelSettingChanged,
                actions.onModeSelected,
            )
            Spacer(modifier = Modifier.height(14.dp))
            if (state.isAndroid13Plus) {
                DomainsIpsSection(actions.onManageDomainsIps)
                Spacer(modifier = Modifier.height(14.dp))
            }
            AppsTitle()
            Spacer(modifier = Modifier.height(8.dp))
            ShowSystemAppsToggle(state.showSystemApps, actions.onShowSystemAppsToggle)
            Spacer(modifier = Modifier.height(8.dp))
            Search(state.searchKeyword, actions.onQueryTextChange)
            Apps(state.filteredApps, state.appIconCache, actions.onAppSelected)
        }
        PreferenceProgressBar(state.showProgress)
    }
}

@Composable
private fun ShowSystemAppsToggle(
    showSystemApps: Boolean,
    onShowSystemAppsToggle: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp),
                ).clickable { onShowSystemAppsToggle() }
                .padding(start = 16.dp, end = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.show_system_apps),
            color = MaterialTheme.colorScheme.primaryTextColor,
            style = font16.copy(fontWeight = FontWeight.Medium),
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f),
        )
        Checkbox(
            showSystemApps,
            onCheckedChange = {
                onShowSystemAppsToggle()
            },
            colors =
                CheckboxDefaults.colors(
                    checkmarkColor = MaterialTheme.colorScheme.preferencesBackgroundColor,
                    checkedColor = MaterialTheme.colorScheme.primaryTextColor,
                    uncheckedColor = MaterialTheme.colorScheme.preferencesSubtitleColor,
                ),
        )
    }
}

@Composable
private fun Search(
    query: String,
    onQueryTextChange: (String) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                ).padding(horizontal = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize(),
        ) {
            Image(
                painter = painterResource(com.windscribe.mobile.R.drawable.ic_location_search),
                contentDescription = "Search",
                modifier =
                    Modifier.clickable {
                    },
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.70f)),
            )
            TextField(
                value = query,
                onValueChange = {
                    onQueryTextChange(it)
                },
                modifier =
                    Modifier
                        .testTag("split_tunnel_search")
                        .weight(1f)
                        .fillMaxHeight(),
                textStyle = font16.copy(textAlign = TextAlign.Start, color = MaterialTheme.colorScheme.primaryTextColor),
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primaryTextColor,
                        selectionColors =
                            androidx.compose.foundation.text.selection.TextSelectionColors(
                                handleColor = MaterialTheme.colorScheme.primaryTextColor,
                                backgroundColor = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.3f),
                            ),
                    ),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            )
        }
    }
}

@Composable
private fun Mode(
    modes: List<DropDownStringItem>,
    selectedKey: String,
    splitTunnelEnabled: Boolean,
    onSplitTunnelSettingChanged: () -> Unit,
    onModeSelected: (DropDownStringItem) -> Unit,
) {
    val modeDescription =
        if (selectedKey == "Exclusive") R.string.feature_tunnel_mode_exclusive else R.string.feature_tunnel_mode_inclusive
    Column {
        SwitchItemView(
            title = R.string.split_tunneling,
            icon = com.windscribe.mobile.R.drawable.ic_split_routing,
            description = R.string.split_tunneling_feature,
            splitTunnelEnabled,
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            onSelect = {
                onSplitTunnelSettingChanged()
            },
            explainer = FeatureExplainer.SPLIT_TUNNELING,
        )
        Spacer(modifier = Modifier.height(1.dp))
        CustomDropDown(R.string.mode, modes, selectedKey, description = modeDescription) {
            onModeSelected(it)
        }
    }
}

@Composable
private fun DomainsIpsSection(onManageClick: () -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.domains_ips),
            color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.50f),
            style = font16.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp),
                    ).clickable { onManageClick() }
                    .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.add_ip_domain),
                color = MaterialTheme.colorScheme.primaryTextColor,
                style = font16.copy(fontWeight = FontWeight.Medium),
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f),
            )
            Image(
                painter = painterResource(android.R.drawable.ic_menu_add),
                contentDescription = "Add",
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primaryTextColor),
            )
        }
    }
}

@Composable
private fun AppsTitle() {
    Text(
        text = stringResource(R.string.apps),
        color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.50f),
        style = font16.copy(fontWeight = FontWeight.SemiBold),
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Apps(
    apps: List<InstalledAppsData>,
    appIconCache: AppIconCache?,
    onAppSelected: (InstalledAppsData) -> Unit,
) {
    LazyColumn {
        itemsIndexed(apps) { index, app ->
            val shape =
                if (index == apps.lastIndex) {
                    RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                } else {
                    RoundedCornerShape(0.dp)
                }
            Spacer(modifier = Modifier.height(1.dp))
            Row(
                modifier =
                    Modifier
                        .testTag("app_row_${app.appName}")
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                            shape = shape,
                        ).padding(start = 14.dp)
                        .clickable { onAppSelected(app) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val iconBitmap = appIconCache?.getIcon(app.packageName)
                if (iconBitmap != null) {
                    Image(
                        painter = BitmapPainter(iconBitmap.asImageBitmap()),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                } else {
                    Image(
                        painter = painterResource(android.R.drawable.sym_def_app_icon),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = app.appName,
                    color = MaterialTheme.colorScheme.primaryTextColor,
                    style = font16.copy(fontWeight = FontWeight.Medium),
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Checkbox(
                    app.isChecked,
                    onCheckedChange = {
                        onAppSelected(app)
                    },
                    colors =
                        CheckboxDefaults.colors(
                            checkmarkColor = MaterialTheme.colorScheme.preferencesBackgroundColor,
                            checkedColor = MaterialTheme.colorScheme.primaryTextColor,
                            uncheckedColor = MaterialTheme.colorScheme.preferencesSubtitleColor,
                        ),
                )
            }
        }
    }
}

/**
 * Feeds representative [SplitTunnelState] values into the preview.
 */
private class SplitTunnelStateProvider : PreviewParameterProvider<SplitTunnelState> {
    override val values: Sequence<SplitTunnelState>
        get() {
            val chrome = InstalledAppsData("Chrome", "com.google.chrome").apply { isChecked = true }
            val discord = InstalledAppsData("Discord", "com.discord")
            val telegram = InstalledAppsData("Telegram", "org.telegram.messenger")
            return sequenceOf(
                SplitTunnelState(
                    isSplitTunnelEnabled = true,
                    filteredApps = listOf(chrome, discord, telegram),
                    appIconCache = AppIconCache(),
                ),
            )
        }
}

@Composable
@MultiDevicePreview
private fun SplitTunnelContentPreview(
    @PreviewParameter(SplitTunnelStateProvider::class) state: SplitTunnelState,
) {
    PreviewWithNav {
        SplitTunnelContent(
            state = state,
            actions = SplitTunnelActions(),
        )
    }
}
