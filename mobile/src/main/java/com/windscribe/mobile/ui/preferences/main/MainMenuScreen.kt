package com.windscribe.mobile.ui.preferences.main

import PreferencesNavBar
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.common.PreferenceProgressBar
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.home.HomeViewmodel
import com.windscribe.mobile.ui.home.HomeViewmodelImpl
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.R

/**
 * Stateful entry point. Owns the [MainMenuViewModel] and [HomeViewmodel] (each its own per-route
 * instance), collects their flows, then delegates rendering to [MainMenuContent].
 */
@Composable
fun MainMenuScreen(
    viewModel: MainMenuViewModel = hiltViewModel<MainMenuViewModelImpl>(),
    homeViewModel: HomeViewmodel = hiltViewModel<HomeViewmodelImpl>(),
) {
    val showProgress by viewModel.showProgress.collectAsState()
    val hapticFeedbackEnabled by homeViewModel.hapticFeedbackEnabled.collectAsState()
    MainMenuContent(
        showProgress = showProgress,
        showReferData = viewModel.showReferData,
        hapticFeedbackEnabled = hapticFeedbackEnabled,
        onLogout = viewModel::logout,
    )
}

/**
 * Stateless main-menu UI. Everything it needs is passed in, so it renders identically in the app
 * and in `@Preview`. This is the composable previews target.
 */
@Composable
fun MainMenuContent(
    showProgress: Boolean,
    showReferData: Boolean,
    hapticFeedbackEnabled: Boolean,
    onLogout: () -> Unit,
) {
    val navController = LocalNavController.current
    PreferenceBackground {
        Column(modifier = Modifier.testTag("main_menu_screen").padding(vertical = 16.dp, horizontal = 16.dp)) {
            PreferencesNavBar(stringResource(R.string.preferences)) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.height(20.dp))
            MainMenuItem(
                R.string.general,
                com.windscribe.mobile.R.drawable.ic_preferences_icon,
                Screen.General.route,
                hapticFeedbackEnabled,
            )
            Spacer(modifier = Modifier.height(16.dp))
            MainMenuItem(
                R.string.my_account,
                com.windscribe.mobile.R.drawable.ic_myaccount_icon,
                Screen.Account.route,
                hapticFeedbackEnabled,
            )
            Spacer(modifier = Modifier.height(16.dp))
            MainMenuItem(
                R.string.connection,
                com.windscribe.mobile.R.drawable.ic_connection_icon,
                Screen.Connection.route,
                hapticFeedbackEnabled,
            )
            Spacer(modifier = Modifier.height(16.dp))
            MainMenuItem(
                R.string.robert,
                com.windscribe.mobile.R.drawable.ic_robert,
                Screen.Robert.route,
                hapticFeedbackEnabled,
            )
            if (showReferData) {
                Spacer(modifier = Modifier.height(16.dp))
                MainMenuItem(
                    R.string.refer_for_data,
                    com.windscribe.mobile.R.drawable.ic_favourite,
                    Screen.ShareLink.route,
                    hapticFeedbackEnabled,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            MainMenuItem(
                R.string.look_and_feel,
                com.windscribe.mobile.R.drawable.ic_feel,
                Screen.LookAndFeel.route,
                hapticFeedbackEnabled,
            )
            Spacer(modifier = Modifier.height(16.dp))
            MainMenuItem(
                R.string.help_me,
                com.windscribe.mobile.R.drawable.ic_helpme_icon,
                Screen.HelpMe.route,
                hapticFeedbackEnabled,
            )
            Spacer(modifier = Modifier.height(16.dp))
            MainMenuItem(
                R.string.about_us,
                com.windscribe.mobile.R.drawable.ic_about,
                Screen.About.route,
                hapticFeedbackEnabled,
            )
            Spacer(modifier = Modifier.height(16.dp))
            LogoutItem(hapticFeedbackEnabled, showProgress, onLogout)
        }
        PreferenceProgressBar(showProgress)
    }
}

@Composable
private fun MainMenuItem(
    @StringRes title: Int,
    @DrawableRes icon: Int,
    nextRoute: String,
    hapticFeedbackEnabled: Boolean,
) {
    val navController = LocalNavController.current
    val hapticFeedback = LocalHapticFeedback.current
    Row(
        Modifier
            .testTag("menu_item_$nextRoute")
            .fillMaxWidth()
            .height(44.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                shape = RoundedCornerShape(size = 12.dp),
            ).clickable {
                if (hapticFeedbackEnabled) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                }
                navController.navigate(nextRoute)
            }.padding(start = 14.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier =
                Modifier
                    .size(24.dp)
                    .padding(4.dp),
            painter = painterResource(icon),
            contentDescription = "",
            tint = MaterialTheme.colorScheme.primaryTextColor,
        )
        Text(
            stringResource(title),
            modifier = Modifier.padding(start = 16.dp),
            style = font16,
            color = MaterialTheme.colorScheme.primaryTextColor,
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
private fun LogoutItem(
    hapticFeedbackEnabled: Boolean,
    showProgress: Boolean,
    onLogout: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("logout_button")
            .background(
                color = AppColors.yellow.copy(0.05f),
                shape = RoundedCornerShape(size = 12.dp),
            ).clickable(enabled = !showProgress) {
                if (hapticFeedbackEnabled) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                }
                onLogout()
            }.padding(start = 14.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val alpha = if (showProgress) 0.5f else 1f
        Icon(
            modifier =
                Modifier
                    .size(24.dp),
            painter = painterResource(com.windscribe.mobile.R.drawable.ic_sign_out),
            contentDescription = "",
            tint = AppColors.yellow.copy(alpha = alpha),
        )
        Text(
            stringResource(R.string.logout),
            modifier = Modifier.padding(start = 16.dp),
            style = font16.copy(fontWeight = FontWeight.Medium),
            color = AppColors.yellow.copy(alpha = alpha),
        )
    }
}

/**
 * Feeds representative state into the preview so the renderer draws [MainMenuContent] without a VM.
 */
private class MainMenuStateProvider : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(false, true)
}

@Composable
@MultiDevicePreview
private fun MainMenuContentPreview(
    @PreviewParameter(MainMenuStateProvider::class) showProgress: Boolean,
) {
    PreviewWithNav {
        MainMenuContent(
            showProgress = showProgress,
            showReferData = true,
            hapticFeedbackEnabled = false,
            onLogout = {},
        )
    }
}
