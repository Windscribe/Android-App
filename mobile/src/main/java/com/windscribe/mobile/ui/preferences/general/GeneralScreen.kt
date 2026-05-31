package com.windscribe.mobile.ui.preferences.general

import PreferencesNavBar
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.AppStartActivity
import com.windscribe.mobile.ui.common.DropDownItemView
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.common.SwitchItemView
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.model.DropDownStringItem
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor
import com.windscribe.mobile.ui.theme.primaryTextColor

/**
 * Callbacks the general-preferences UI can raise. Hoisted out of the composables so the
 * stateless [GeneralContent] never needs to know about [GeneralViewModel] — previews supply
 * no-op lambdas.
 */
class GeneralActions(
    val onOrderBySelected: (DropDownStringItem) -> Unit = {},
    val onLanguageSelected: (DropDownStringItem) -> Unit = {},
    val onLocationLoadClick: () -> Unit = {},
    val onNotificationStatClick: () -> Unit = {},
    val onHapticToggleClick: () -> Unit = {},
)

/**
 * Stateful entry point. Owns the [GeneralViewModel], collects its flows and wires the
 * reload-app side effect, then delegates rendering to [GeneralContent].
 */
@Composable
fun GeneralScreen(viewModel: GeneralViewModel = hiltViewModel<GeneralViewModelImpl>()) {
    val activity = LocalActivity.current as? AppStartActivity
    val reloadAppState = viewModel.reloadApp.collectAsState(initial = false)
    val isHapticEnabled by viewModel.isHapticEnabled.collectAsState()
    val isNotificationStatEnabled by viewModel.isNotificationStatEnabled.collectAsState()
    val isLocationLoadEnabled by viewModel.isLocationLoadEnabled.collectAsState()
    LaunchedEffect(reloadAppState.value) {
        if (reloadAppState.value) {
            activity?.recreate()
        }
    }
    GeneralContent(
        orderByItems = viewModel.orderByItems,
        selectedOrderBy = viewModel.selectedOrderBy,
        languageItems = viewModel.languageItems,
        selectedLanguage = viewModel.selectedLanguage,
        isLocationLoadEnabled = isLocationLoadEnabled,
        isNotificationStatEnabled = isNotificationStatEnabled,
        isHapticEnabled = isHapticEnabled,
        versionName = viewModel.versionName,
        actions =
            GeneralActions(
                onOrderBySelected = viewModel::onOrderByItemSelected,
                onLanguageSelected = viewModel::onLanguageItemSelected,
                onLocationLoadClick = viewModel::onLocationLoadItemClicked,
                onNotificationStatClick = viewModel::onNotificationStatEnabledClick,
                onHapticToggleClick = viewModel::onHapticToggleButtonClicked,
            ),
    )
}

/**
 * Stateless general-preferences UI. Everything it needs is passed in, so it renders identically
 * in the app and in `@Preview`. This is the composable previews target.
 */
@Composable
fun GeneralContent(
    orderByItems: List<DropDownStringItem>,
    selectedOrderBy: String,
    languageItems: List<DropDownStringItem>,
    selectedLanguage: String,
    isLocationLoadEnabled: Boolean,
    isNotificationStatEnabled: Boolean,
    isHapticEnabled: Boolean,
    versionName: String,
    actions: GeneralActions,
) {
    val navController = LocalNavController.current
    PreferenceBackground {
        Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)) {
            PreferencesNavBar(title = stringResource(com.windscribe.vpn.R.string.general)) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.height(20.dp))

            DropDownItemView(
                title = com.windscribe.vpn.R.string.sort_by,
                icon = R.drawable.ic_sort_location,
                description = com.windscribe.vpn.R.string.location_order_description,
                items = orderByItems,
                selectedItemKey = selectedOrderBy,
                onSelect = actions.onOrderBySelected,
            )

            Spacer(modifier = Modifier.height(14.dp))

            DropDownItemView(
                title = com.windscribe.vpn.R.string.preferred_language,
                icon = R.drawable.ic_language,
                description = com.windscribe.vpn.R.string.language_description,
                items = languageItems,
                selectedItemKey = selectedLanguage,
                onSelect = actions.onLanguageSelected,
            )
            Spacer(modifier = Modifier.height(14.dp))
            SwitchItemView(
                com.windscribe.vpn.R.string.show_location_health,
                R.drawable.ic_location_load,
                com.windscribe.vpn.R.string.location_load_description,
                isLocationLoadEnabled,
            ) {
                actions.onLocationLoadClick()
            }
            Spacer(modifier = Modifier.height(14.dp))
            SwitchItemView(
                title = com.windscribe.vpn.R.string.notifications,
                icon = R.drawable.ic_notification_stats,
                description = com.windscribe.vpn.R.string.notification_stats_description,
                isNotificationStatEnabled,
                onSelect = { actions.onNotificationStatClick() },
            )
            Spacer(modifier = Modifier.height(14.dp))
            SwitchItemView(
                title = com.windscribe.vpn.R.string.haptic_setting_label,
                icon = R.drawable.ic_haptic,
                description = com.windscribe.vpn.R.string.haptic_feedback_description,
                isHapticEnabled,
                onSelect = { actions.onHapticToggleClick() },
            )
            Spacer(modifier = Modifier.height(14.dp))
            VersionName(versionName)
        }
    }
}

@Composable
private fun VersionName(versionName: String) {
    Row(
        Modifier
            .background(
                color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                shape = RoundedCornerShape(size = 12.dp),
            ).padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 14.dp),
    ) {
        Text(
            text = stringResource(com.windscribe.vpn.R.string.version),
            style =
                font16.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primaryTextColor,
                ),
        )
        Spacer(modifier = Modifier.weight(1.0f))
        Text(
            text = versionName,
            style = font16.copy(color = MaterialTheme.colorScheme.preferencesSubtitleColor),
        )
    }
}

/**
 * Feeds representative state into the preview so the renderer draws [GeneralContent] without a VM.
 */
private class GeneralStateProvider : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(false, true)
}

@Composable
@MultiDevicePreview
private fun GeneralContentPreview(
    @PreviewParameter(GeneralStateProvider::class) toggled: Boolean,
) {
    PreviewWithNav {
        GeneralContent(
            orderByItems = emptyList(),
            selectedOrderBy = "",
            languageItems = emptyList(),
            selectedLanguage = "",
            isLocationLoadEnabled = toggled,
            isNotificationStatEnabled = toggled,
            isHapticEnabled = toggled,
            versionName = "v3.0.0",
            actions = GeneralActions(),
        )
    }
}
