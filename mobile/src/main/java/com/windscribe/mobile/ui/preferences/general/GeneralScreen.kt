package com.windscribe.mobile.ui.preferences.general

import PreferencesNavBar
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.AppStartActivity
import com.windscribe.mobile.ui.common.DropDownItemView
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.common.SwitchItemView
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor
import com.windscribe.mobile.ui.theme.primaryTextColor

@Composable
fun GeneralScreen(viewModel: GeneralViewModel? = null) {
    val navController = LocalNavController.current
    val activity = LocalContext.current as? AppStartActivity
    val reloadAppState = viewModel?.reloadApp?.collectAsState(initial = false)
    val isHapticEnabled by viewModel?.isHapticEnabled?.collectAsState()
        ?: remember { mutableStateOf(false) }
    val isNotificationStatEnabled by viewModel?.isNotificationStatEnabled?.collectAsState()
        ?: remember { mutableStateOf(false) }
    val isLocationLoadEnabled by viewModel?.isLocationLoadEnabled?.collectAsState()
        ?: remember { mutableStateOf(false) }
    LaunchedEffect(reloadAppState?.value) {
        if (reloadAppState?.value == true) {
            activity?.reloadApp()
        }
    }
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
                items = viewModel?.orderByItems ?: emptyList(),
                selectedItemKey = viewModel?.selectedOrderBy ?: "",
                onSelect = { viewModel?.onOrderByItemSelected(it) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            DropDownItemView(
                title = com.windscribe.vpn.R.string.preferred_language,
                icon = R.drawable.ic_language,
                description = com.windscribe.vpn.R.string.language_description,
                items = viewModel?.languageItems ?: emptyList(),
                selectedItemKey = viewModel?.selectedLanguage ?: "",
                onSelect = { viewModel?.onLanguageItemSelected(it) }
            )
            Spacer(modifier = Modifier.height(14.dp))
            SwitchItemView(
                com.windscribe.vpn.R.string.show_location_health,
                R.drawable.ic_location_load,
                com.windscribe.vpn.R.string.location_load_description,
                isLocationLoadEnabled
            ) {
                viewModel?.onLocationLoadItemClicked()
            }
            Spacer(modifier = Modifier.height(14.dp))
            SwitchItemView(
                title = com.windscribe.vpn.R.string.notifications,
                icon = R.drawable.ic_notification_stats,
                description = com.windscribe.vpn.R.string.notification_stats_description,
                isNotificationStatEnabled,
                onSelect = { viewModel?.onNotificationStatEnabledClick() }
            )
            Spacer(modifier = Modifier.height(14.dp))
            SwitchItemView(
                title = com.windscribe.vpn.R.string.haptic_setting_label,
                icon = R.drawable.ic_haptic,
                description = com.windscribe.vpn.R.string.haptic_feedback_description,
                isHapticEnabled,
                onSelect = { viewModel?.onHapticToggleButtonClicked() }
            )
            Spacer(modifier = Modifier.height(14.dp))
            VersionName(viewModel)
        }
    }
}

@Composable
private fun VersionName(viewModel: GeneralViewModel?) {
    Row(
        Modifier
            .background(
                color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                shape = RoundedCornerShape(size = 12.dp)
            )
            .padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 14.dp),
    ) {
        Text(
            text = stringResource(com.windscribe.vpn.R.string.version),
            style = font16.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primaryTextColor
            ),
        )
        Spacer(modifier = Modifier.weight(1.0f))
        Text(
            text = viewModel?.versionName ?: "",
            style = font16.copy(color = MaterialTheme.colorScheme.preferencesSubtitleColor)
        )
    }
}

@Composable
@MultiDevicePreview
private fun GeneralScreenPreview() {
    PreviewWithNav {
        GeneralScreen()
    }
}