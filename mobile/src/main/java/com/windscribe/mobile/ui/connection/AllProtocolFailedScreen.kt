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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.AppStartActivityViewModel
import com.windscribe.mobile.ui.common.NextButton
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.font24

/**
 * Stateful entry point. The [AppStartActivityViewModel] is activity-scoped (it carries the
 * connection callbacks set by the hosting activity), so it is passed in rather than resolved
 * via `hiltViewModel()`. Side effects are wired here and rendering is delegated to
 * [AllProtocolFailedContent].
 */
@Composable
fun AllProtocolFailedScreen(appStartActivityViewModel: AppStartActivityViewModel) {
    val navController = LocalNavController.current
    AllProtocolFailedContent(
        onSendLogClick = {
            appStartActivityViewModel.autoConnectionModeCallback?.onSendLogClicked()
            navController.popBackStack()
        },
        onCancelClick = {
            appStartActivityViewModel.autoConnectionModeCallback?.onCancel()
            navController.popBackStack()
        },
    )
}

/**
 * Stateless UI. Everything it needs is passed in, so it renders identically in the app and in
 * `@Preview`. This is the composable previews target.
 */
@Composable
fun AllProtocolFailedContent(
    onSendLogClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    @Suppress("ktlint:standard:max-line-length")
    val networkHatesUsDescription =
        stringResource(
            com.windscribe.vpn.R.string.well_we_gave_it_our_best_shot_we_just_couldn_t_connect_you_on_this_network_send_us_your_debug_log_so_we_can_figure_out_what_happened,
        )
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(color = AppColors.deepBlue)
                .clickable(enabled = false) {},
    ) {
        Column(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp)
                    .width(560.dp)
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_attention_icon),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(100.dp)
                        .padding(top = 8.dp),
                colorFilter = ColorFilter.tint(AppColors.white),
            )
            Text(
                text = stringResource(com.windscribe.vpn.R.string.this_network_hates_us),
                style = font24,
                color = AppColors.white,
                textAlign = TextAlign.Center,
            )
            Text(
                text = networkHatesUsDescription,
                style = font16,
                color = AppColors.white,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Spacer(modifier = Modifier.padding(top = 24.dp))
            NextButton(Modifier, text = stringResource(com.windscribe.vpn.R.string.send_debug_log), true) {
                onSendLogClick()
            }
            TextButton(onClick = onCancelClick) {
                Text(
                    stringResource(com.windscribe.vpn.R.string.cancel),
                    style = font16,
                    color = AppColors.white,
                )
            }
        }
    }
}

@MultiDevicePreview
@Composable
fun AllProtocolFailedScreenPreview() {
    PreviewWithNav {
        AllProtocolFailedContent(
            onSendLogClick = {},
            onCancelClick = {},
        )
    }
}
