package com.windscribe.mobile.ui.preferences.email

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.windscribe.mobile.ui.common.NextButton
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.common.PreferenceProgressBar
import com.windscribe.mobile.ui.connection.ToastMessage
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.font24
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor
import com.windscribe.mobile.ui.theme.primaryTextColor

@Composable
fun ConfirmEmailScreen(viewModel: EmailViewModel = hiltViewModel<EmailViewModelImpl>()) {
    val error by viewModel.error.collectAsState()
    val showProgress by viewModel.showProgress.collectAsState()
    val exit by viewModel.exit.collectAsState()
    val pro by viewModel.pro.collectAsState()
    ConfirmEmailContent(
        error = error,
        showProgress = showProgress,
        exit = exit,
        pro = pro,
        onResendConfirmation = viewModel::resendConfirmation,
    )
}

@Composable
fun ConfirmEmailContent(
    error: ToastMessage?,
    showProgress: Boolean,
    exit: Boolean,
    pro: Boolean,
    onResendConfirmation: () -> Unit = {},
) {
    val navController = LocalNavController.current
    LaunchedEffect(exit) {
        if (exit) {
            navController.popBackStack()
        }
    }
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
                painter = painterResource(id = com.windscribe.mobile.R.drawable.ic_confirmemail),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.primaryTextColor),
            )
            Text(
                text = stringResource(id = com.windscribe.vpn.R.string.confirm_email),
                style = font24,
                color = MaterialTheme.colorScheme.primaryTextColor,
                modifier =
                    Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth(),
            )
            Text(
                text =
                    stringResource(
                        id =
                            if (pro) {
                                com.windscribe.vpn.R.string.pro_reason_to_confirm
                            } else {
                                com.windscribe.vpn.R.string.free_reason_to_confirm
                            },
                    ),
                style = font16,
                color = MaterialTheme.colorScheme.preferencesSubtitleColor,
                modifier =
                    Modifier
                        .fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            NextButton(
                text = stringResource(com.windscribe.vpn.R.string.resend_verification_email),
                enabled = true,
                onClick = {
                    onResendConfirmation()
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = {
                navController.navigate(Screen.AddEmail.route) {
                    popUpTo(Screen.ConfirmEmail.route) { inclusive = true }
                }
            }) {
                Text(
                    stringResource(id = com.windscribe.vpn.R.string.change_email),
                    style = font16,
                    color = MaterialTheme.colorScheme.preferencesSubtitleColor,
                )
            }
            TextButton(onClick = {
                navController.popBackStack()
            }) {
                Text(
                    stringResource(id = com.windscribe.vpn.R.string.cancel),
                    style = font16,
                    color = MaterialTheme.colorScheme.preferencesSubtitleColor,
                )
            }
        }
        PreferenceProgressBar(showProgress)
    }
}

private class ConfirmEmailStateProvider : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(false, true)
}

@Composable
@MultiDevicePreview
fun ConfirmEmailContentPreview(
    @PreviewParameter(ConfirmEmailStateProvider::class) pro: Boolean,
) {
    PreviewWithNav {
        ConfirmEmailContent(
            error = null,
            showProgress = false,
            exit = false,
            pro = pro,
        )
    }
}
