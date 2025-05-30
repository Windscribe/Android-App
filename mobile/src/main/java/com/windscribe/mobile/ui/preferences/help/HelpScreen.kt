package com.windscribe.mobile.ui.preferences.help

import PreferencesNavBar
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.vpn.R


@Composable
fun HelpScreen(viewModel: HelpViewModel? = null) {
    val navController = LocalNavController.current
    PreferenceBackground {
        Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)) {
            PreferencesNavBar(stringResource(R.string.help_me)) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
@MultiDevicePreview
private fun HelpScreenPreview() {
    PreviewWithNav {
        HelpScreen()
    }
}