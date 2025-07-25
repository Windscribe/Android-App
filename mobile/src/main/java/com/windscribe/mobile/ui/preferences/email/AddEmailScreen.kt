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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.autofill.ContentType
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
fun AddEmailScreen(viewModel: EmailViewModel? = null) {
    val navController = LocalNavController.current
    val error by viewModel?.error?.collectAsState() ?: remember { mutableStateOf(null) }
    val showProgress by viewModel?.showProgress?.collectAsState()
        ?: remember { mutableStateOf(false) }
    val exit by viewModel?.exit?.collectAsState() ?: remember { mutableStateOf(false) }
    LaunchedEffect(exit) {
        if (exit) {
            navController.popBackStack()
        }
    }
    val errorMessage = if (error is ToastMessage.Raw) {
        (error as ToastMessage.Raw).message
    } else if (error is ToastMessage.Localized) {
        val resourceID = (error as ToastMessage.Localized).message
        stringResource(resourceID)
    } else {
        ""
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
                    viewModel?.onEmailChanged(it)
                })
            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp),
                    style = font12.copy(textAlign = TextAlign.Start),
                    color = AppColors.red,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            NextButton(
                modifier = Modifier,
                stringResource(com.windscribe.vpn.R.string.add_email),
                true,
                onClick = {
                    viewModel?.addEmail()
                })
        }
        PreferenceProgressBar(showProgress)
    }
}

@Composable
@MultiDevicePreview
fun AddEmailScreenPreview() {
    PreviewWithNav {
        AddEmailScreen()
    }
}