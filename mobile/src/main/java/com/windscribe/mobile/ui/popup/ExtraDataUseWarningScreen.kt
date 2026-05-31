package com.windscribe.mobile.ui.popup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.AppStartActivityViewModel
import com.windscribe.mobile.ui.common.NextButton
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.font24
import com.windscribe.mobile.ui.theme.preferencesBackgroundColor
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor
import com.windscribe.mobile.ui.theme.primaryTextColor

/**
 * Stateful entry point. The [AppStartActivityViewModel] is activity-scoped, so it is passed in
 * rather than resolved via `hiltViewModel()`. Side effects are wired here and rendering is
 * delegated to [ExtraDataUseWarningContent].
 */
@Composable
fun ExtraDataUseWarningScreen(viewmodel: AppStartActivityViewModel) {
    val navController = LocalNavController.current
    ExtraDataUseWarningContent(
        onUnderstandClick = {
            viewmodel.protocolInformation
            viewmodel.enableDecoyTraffic()
            navController.popBackStack()
        },
        onCancelClick = {
            navController.popBackStack()
        },
    )
}

/**
 * Stateless UI. Everything it needs is passed in, so it renders identically in the app and in
 * `@Preview`. This is the composable previews target.
 */
@Composable
fun ExtraDataUseWarningContent(
    onUnderstandClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    PreferenceBackground {
        Column(
            modifier =
                Modifier
                    .width(400.dp)
                    .padding(horizontal = 32.dp)
                    .align(Alignment.Center),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_garrydecoy),
                contentDescription = null,
            )
            Text(
                text = stringResource(id = com.windscribe.vpn.R.string.decoy_traffic_mode),
                style = font24,
                color = MaterialTheme.colorScheme.primaryTextColor,
                modifier =
                    Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth(),
            )
            Text(
                text = stringResource(id = com.windscribe.vpn.R.string.decoy_traffic_warning),
                style = font16,
                color = MaterialTheme.colorScheme.preferencesSubtitleColor,
                modifier =
                    Modifier
                        .fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.preferencesSubtitleColor,
                                shape = RoundedCornerShape(12.dp),
                            ).padding(16.dp)
                            .align(Alignment.TopCenter),
                ) {
                    Text(
                        text = stringResource(com.windscribe.vpn.R.string.decoy_caution_description),
                        style = font12,
                        color = MaterialTheme.colorScheme.preferencesSubtitleColor,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(
                    text = stringResource(com.windscribe.vpn.R.string.caution),
                    style = font12,
                    color = MaterialTheme.colorScheme.preferencesSubtitleColor,
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .background(MaterialTheme.colorScheme.preferencesBackgroundColor) // match parent background to "cut" border
                            .padding(horizontal = 8.dp)
                            .offset(y = (-8).dp),
                )
            }
            NextButton(
                text = stringResource(com.windscribe.vpn.R.string.i_understand),
                enabled = true,
                onClick = onUnderstandClick,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onCancelClick) {
                Text(
                    stringResource(id = com.windscribe.vpn.R.string.cancel),
                    style = font16,
                    color = MaterialTheme.colorScheme.preferencesSubtitleColor,
                )
            }
        }
    }
}

@Composable
@MultiDevicePreview
fun ExtraDataUseWarningScreenPreview() {
    PreviewWithNav {
        ExtraDataUseWarningContent(
            onUnderstandClick = {},
            onCancelClick = {},
        )
    }
}
