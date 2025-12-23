package com.windscribe.mobile.ui.auth

import NavBar
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.common.AppBackground
import com.windscribe.mobile.ui.common.AppProgressBar
import androidx.compose.ui.autofill.ContentType
import com.windscribe.mobile.ui.common.AuthTextField
import com.windscribe.mobile.ui.common.CaptchaDebugDialog
import com.windscribe.mobile.ui.common.NextButton
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.nav.NavigationStack
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.vpn.constants.NetworkKeyConstants

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun LoginScreen(
    windowSizeClass: WindowSizeClass? = currentWindowAdaptiveInfo().windowSizeClass,
    viewModel: LoginViewModel? = null
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val loginState by viewModel?.loginState?.collectAsState() ?: remember {
        mutableStateOf(LoginState.Error(AuthError.InputError("")))
    }
    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            navController.navigate(Screen.Home.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
    LaunchedEffect(Unit) {
        viewModel?.showAllBackupFailedDialog?.collect { show ->
            if (show) {
                navController.navigate(Screen.AllProtocolFailedDialog.route)
                viewModel.clearDialog()
            }
        }
    }
    AppBackground {
        LoginCompactLayout(navController, loginState, viewModel)
        val showProgressBar = loginState is LoginState.LoggingIn
        val message = (loginState as? LoginState.LoggingIn)?.message ?: ""
        AppProgressBar(showProgressBar, message = message)
        if (loginState is LoginState.Captcha) {
            val captchaRequest = (loginState as LoginState.Captcha).request
            CaptchaDebugDialog(
                captchaRequest, onCancel = {
                    viewModel?.dismissCaptcha()
                },
                onSolutionSubmit = { t1, t2 ->
                    Log.i("LoginScreen", "onSolutionSubmit: $t1, $t2")
                    viewModel?.onCaptchaSolutionReceived(
                        CaptchaSolution(
                            t1,
                            t2,
                            captchaRequest.secureToken
                        )
                    )
                })
        }
    }
}

@Composable
fun LoginCompactLayout(
    navController: NavController, loginState: LoginState, viewModel: LoginViewModel? = null
) {
    val showTwoFactorTextField = viewModel?.twoFactorEnabled?.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .imePadding(),
        verticalArrangement = Arrangement.Top
    ) {
        NavBar(stringResource(com.windscribe.vpn.R.string.login)) {
            navController.popBackStack()
        }
        Spacer(modifier = Modifier.height(16.dp))
        LoginUsernameTextField(loginState, viewModel)
        Spacer(modifier = Modifier.height(16.dp))
        LoginPasswordTextField(loginState, viewModel)
        Spacer(modifier = Modifier.height(8.dp))
        AnimatedContent(
            targetState = showTwoFactorTextField?.value ?: false,
            transitionSpec = {
                fadeIn() + slideInVertically(initialOffsetY = { it }) togetherWith
                        fadeOut() + slideOutVertically(targetOffsetY = { it })
            }
        ) { isTwoFactor ->
            if (isTwoFactor) {
                LoginTwoFactorTextField(loginState, viewModel)
            } else {
                ActionSheet(viewModel)
            }
        }
        if (loginState is LoginState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            ErrorText(loginState.errorType)
        }
        Spacer(modifier = Modifier.height(16.dp))
        LoginHeroButton(viewModel)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun isError(loginState: LoginState, field: AuthInputFields): Boolean {
    return (loginState as? LoginState.Error)?.errorType?.highlightedFields?.contains(field) ?: false
}

@Composable
fun LoginPasswordTextField(loginState: LoginState, viewModel: LoginViewModel? = null) {
    AuthTextField(
        hint = stringResource(com.windscribe.vpn.R.string.password),
        placeHolder = stringResource(com.windscribe.vpn.R.string.enter_password),
        isError = isError(loginState, AuthInputFields.Password),
        modifier = Modifier.fillMaxWidth(),
        isPassword = true,
        autofillType = ContentType.Password,
        onValueChange = {
            viewModel?.onPasswordChanged(it)
        })
}

@Composable
fun LoginTwoFactorTextField(loginState: LoginState, viewModel: LoginViewModel? = null) {
    AuthTextField(
        hint = stringResource(com.windscribe.vpn.R.string.two_fa),
        isError = isError(loginState, AuthInputFields.TwoFactor),
        modifier = Modifier.fillMaxWidth(),
        onValueChange = {
            viewModel?.onTwoFactorChanged(it)
        }, onHintClick = {
            viewModel?.onTwoFactorHintClicked()
        })
}


@Composable
fun LoginUsernameTextField(loginState: LoginState, viewModel: LoginViewModel? = null) {
    AuthTextField(
        hint = stringResource(com.windscribe.vpn.R.string.username),
        placeHolder = stringResource(com.windscribe.vpn.R.string.enter_username),
        isError = isError(loginState, AuthInputFields.Username),
        modifier = Modifier.fillMaxWidth(),
        autofillType = ContentType.Username,
        onValueChange = {
            viewModel?.onUsernameChanged(it)
        })
}

@Composable
fun LoginHeroButton(viewModel: LoginViewModel? = null) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val isButtonEnabled by viewModel?.loginButtonEnabled?.collectAsState() ?: remember {
        mutableStateOf(false)
    }
    NextButton(
        text = stringResource(com.windscribe.vpn.R.string.next), enabled = isButtonEnabled, onClick = {
            keyboardController?.hide()
            viewModel?.loginButtonClick()
        }, modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    )
}

@Composable
private fun AppTextButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .clickable {
                onClick()
            }
    ) {
        Text(
            text = text,
            style = font16.copy(textAlign = TextAlign.Center),
            modifier = Modifier.align(Alignment.CenterStart),
            color = AppColors.white.copy(alpha = 0.50f),
        )
    }
}

@Composable
fun ActionSheet(viewModel: LoginViewModel? = null) {
    val context = LocalContext.current
    Row {
        AppTextButton(text = stringResource(com.windscribe.vpn.R.string.two_fa)) {
            viewModel?.onTwoFactorHintClicked()
        }
        Spacer(modifier = Modifier.weight(1f))
        AppTextButton(text = stringResource(com.windscribe.vpn.R.string.forgot_password)) {
            val url =
                NetworkKeyConstants.getWebsiteLink(NetworkKeyConstants.URL_FORGOT_PASSWORD)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun ErrorText(errorType: AuthError) {
    val message = when (errorType) {
        is AuthError.LocalizedInputError -> stringResource(errorType.error)
        is AuthError.InputError -> errorType.error
    }
    if (message.isNotBlank()) {
        Text(
            text = message,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp),
            style = font12.copy(textAlign = TextAlign.Start),
            color = AppColors.red,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
@MultiDevicePreview
fun LoginScreenPreview() {
    PreviewWithNav {
        LoginScreen()
    }
}