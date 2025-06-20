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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.common.PreferenceProgressBar
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.R


@Composable
fun MainMenuScreen(viewModel: MainMenuViewModel? = null) {
    val navController = LocalNavController.current
    val showProgress by viewModel?.showProgress?.collectAsState() ?: remember { mutableStateOf(false) }
    PreferenceBackground {
        Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)) {
            PreferencesNavBar(stringResource(R.string.preferences)) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.height(20.dp))
            MainMenuItem(
                R.string.general,
                com.windscribe.mobile.R.drawable.ic_preferences_icon,
                Screen.General.route
            )
            Spacer(modifier = Modifier.height(16.dp))
            MainMenuItem(
                R.string.my_account,
                com.windscribe.mobile.R.drawable.ic_myaccount_icon,
                Screen.Account.route
            )
            Spacer(modifier = Modifier.height(16.dp))
            MainMenuItem(
                R.string.connection,
                com.windscribe.mobile.R.drawable.ic_connection_icon,
                Screen.Connection.route
            )
            Spacer(modifier = Modifier.height(16.dp))
            MainMenuItem(
                R.string.robert,
                com.windscribe.mobile.R.drawable.ic_robert,
                Screen.Robert.route
            )
            if (viewModel?.showReferData == true) {
                Spacer(modifier = Modifier.height(16.dp))
                MainMenuItem(
                    R.string.refer_for_data,
                    com.windscribe.mobile.R.drawable.ic_favourite,
                    Screen.ShareLink.route
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            MainMenuItem(
                R.string.look_and_feel,
                com.windscribe.mobile.R.drawable.ic_feel,
                Screen.LookAndFeel.route
            )
            Spacer(modifier = Modifier.height(16.dp))
            MainMenuItem(
                R.string.help_me,
                com.windscribe.mobile.R.drawable.ic_helpme_icon,
                Screen.HelpMe.route
            )
            Spacer(modifier = Modifier.height(16.dp))
            MainMenuItem(
                R.string.about_us,
                com.windscribe.mobile.R.drawable.ic_about,
                Screen.About.route
            )
            Spacer(modifier = Modifier.height(16.dp))
            LogoutItem(viewModel)
        }
        PreferenceProgressBar(showProgress)
    }
}

@Composable
private fun MainMenuItem(
    @StringRes title: Int,
    @DrawableRes icon: Int,
    nextRoute: String,
) {
    val navController = LocalNavController.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                shape = RoundedCornerShape(size = 12.dp)
            )
            .clickable {
                navController.navigate(nextRoute)
            }
            .padding(start = 14.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier
                .size(24.dp)
                .padding(4.dp),
            painter = painterResource(icon),
            contentDescription = "",
            tint = MaterialTheme.colorScheme.primaryTextColor
        )
        Text(
            stringResource(title),
            modifier = Modifier.padding(start = 16.dp),
            style = font16,
            color = MaterialTheme.colorScheme.primaryTextColor
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
private fun LogoutItem(viewModel: MainMenuViewModel?) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(
                color = AppColors.yellow.copy(0.05f),
                shape = RoundedCornerShape(size = 12.dp)
            )
            .clickable {
                viewModel?.logout()
            }.padding(start = 14.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier
                .size(24.dp),
            painter = painterResource(com.windscribe.mobile.R.drawable.ic_sign_out),
            contentDescription = "",
            tint = AppColors.yellow
        )
        Text(
            stringResource(R.string.logout),
            modifier = Modifier.padding(start = 16.dp),
            style = font16.copy(fontWeight = FontWeight.Medium),
            color = AppColors.yellow
        )
    }
}

@Composable
@MultiDevicePreview
private fun MainMenuScreenPreview() {
    PreviewWithNav {
        MainMenuScreen()
    }
}