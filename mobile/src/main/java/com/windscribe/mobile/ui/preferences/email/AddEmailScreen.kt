package com.windscribe.mobile.ui.preferences.email

import PreferencesNavBar
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.windscribe.mobile.ui.common.AuthTextField
import com.windscribe.mobile.ui.common.NextButton
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.common.PreferenceProgressBar
import com.windscribe.mobile.ui.connection.ToastMessage
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font12

@Composable
fun AddEmailScreen(viewModel: EmailViewModel = hiltViewModel<EmailViewModelImpl>()) {
    val error by viewModel.error.collectAsState()
    val showProgress by viewModel.showProgress.collectAsState()
    val exit by viewModel.exit.collectAsState()
    AddEmailContent(
        error = error,
        showProgress = showProgress,
        exit = exit,
        onEmailChanged = viewModel::onEmailChanged,
        onAddEmailClicked = viewModel::addEmail,
    )
}

@Composable
fun AddEmailContent(
    error: ToastMessage?,
    showProgress: Boolean,
    exit: Boolean,
    onEmailChanged: (String) -> Unit = {},
    onAddEmailClicked: () -> Unit = {},
) {
    val navController = LocalNavController.current
    LaunchedEffect(exit) {
        if (exit) {
            navController.popBackStack()
        }
    }
    val errorMessage =
        when (error) {
            is ToastMessage.Raw -> {
                error.message
            }

            is ToastMessage.Localized -> {
                val resourceID = error.message
                stringResource(resourceID)
            }

            else -> {
                ""
            }
        }
    PreferenceBackground {
        Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)) {
            PreferencesNavBar(stringResource(com.windscribe.vpn.R.string.add_email)) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.height(20.dp))
            AuthTextField(
                Modifier,
                stringResource(com.windscribe.vpn.R.string.email),
                autofillType = ContentType.EmailAddress,
                onValueChange = {
                    onEmailChanged(it)
                },
            )
            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp),
                    style = font12.copy(textAlign = TextAlign.Start),
                    color = AppColors.red,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            NextButton(
                modifier = Modifier,
                stringResource(com.windscribe.vpn.R.string.add_email),
                true,
                onClick = {
                    onAddEmailClicked()
                },
            )
        }
        PreferenceProgressBar(showProgress)
    }
}

private class AddEmailStateProvider : PreviewParameterProvider<ToastMessage?> {
    override val values =
        sequenceOf(
            null,
            ToastMessage.Raw("Something went wrong"),
        )
}

@Composable
@MultiDevicePreview
fun AddEmailContentPreview(
    @PreviewParameter(AddEmailStateProvider::class) error: ToastMessage?,
) {
    PreviewWithNav {
        AddEmailContent(
            error = error,
            showProgress = false,
            exit = false,
        )
    }
}
