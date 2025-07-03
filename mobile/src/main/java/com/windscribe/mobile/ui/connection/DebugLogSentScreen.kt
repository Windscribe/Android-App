package com.windscribe.mobile.ui.connection

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.common.NextButton
import com.windscribe.mobile.ui.common.openUrl
import com.windscribe.mobile.ui.AppStartActivityViewModel
import com.windscribe.vpn.constants.NetworkKeyConstants

@Composable
fun DebugLogSentScreen(appStartActivityViewModel: AppStartActivityViewModel? = null) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = AppColors.deepBlue)
            .clickable(enabled = false) {}) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
                .width(560.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_green_check_with_background),
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .padding(top = 8.dp),
            )
            Text(
                text = stringResource(com.windscribe.vpn.R.string.your_debug_log_has_been_received_please_contact_support_if_you_want_assistance_with_this_issue),
                style = font16,
                color = AppColors.white,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp)
            )
            Spacer(modifier = Modifier.padding(top = 24.dp))
            NextButton(Modifier, text = stringResource(com.windscribe.vpn.R.string.contact_support), true) {
                context.openUrl(NetworkKeyConstants.URL_HELP_ME)
                appStartActivityViewModel?.autoConnectionModeCallback?.onContactSupportClick()
                navController.popBackStack()
            }
            TextButton(onClick = {
                appStartActivityViewModel?.autoConnectionModeCallback?.onCancel()
                navController.popBackStack()
            }) {
                Text(
                    stringResource(com.windscribe.vpn.R.string.cancel),
                    style = font16,
                    color = AppColors.white
                )
            }
        }
    }
}

@MultiDevicePreview
@Composable
fun DebugLogSentScreenPreview() {
    PreviewWithNav {
        DebugLogSentScreen()
    }
}