package com.windscribe.mobile.ui.auth

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.common.AppBackground
import com.windscribe.mobile.ui.common.AppProgressBar
import com.windscribe.mobile.ui.common.CaptchaDebugDialog
import com.windscribe.mobile.ui.common.PrimaryButton
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font14
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.preferencesBackgroundColor
import com.windscribe.mobile.ui.theme.primaryTextColor
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

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel?.onFileSelected(context, it) }
    }

    LaunchedEffect(Unit) {
        viewModel?.triggerFilePicker?.collect { trigger ->
            if (trigger) {
                filePickerLauncher.launch("*/*")
            }
        }
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

    val showTwoFactorInfoDialog by viewModel?.showTwoFactorInfoDialog?.collectAsState() ?: remember {
        mutableStateOf(false)
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

        if (showTwoFactorInfoDialog) {
            TwoFactorInfoDialog(onDismiss = { viewModel?.dismissTwoFactorInfoDialog() })
        }
    }
}

@Composable
fun LoginCompactLayout(
    navController: NavController, loginState: LoginState, viewModel: LoginViewModel? = null
) {
    val selectedAuthType by viewModel?.selectedAuthType?.collectAsState() ?: remember {
        mutableStateOf(AuthType.STANDARD)
    }
    val accountHashDisplay by viewModel?.accountHashDisplay?.collectAsState() ?: remember {
        mutableStateOf("")
    }
    val showTwoFactorTextField by viewModel?.twoFactorEnabled?.collectAsState() ?: remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .imePadding(),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header with title and tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(24.dp).clickable {
                        navController.popBackStack()
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back_arrow),
                        contentDescription = stringResource(com.windscribe.vpn.R.string.back),
                        tint = AppColors.white,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = stringResource(com.windscribe.vpn.R.string.login),
                    style = font16.copy(fontWeight = FontWeight.SemiBold),
                    color = AppColors.white
                )
            }

            AuthTabSelector(
                selectedTab = selectedAuthType,
                onTabSelected = { viewModel?.onAuthTypeChanged(it) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Conditional form rendering based on selected tab
        when (selectedAuthType) {
            AuthType.STANDARD -> {
                StandardLoginForm(
                    isUsernameError = isError(loginState, AuthInputFields.Username),
                    isPasswordError = isError(loginState, AuthInputFields.Password),
                    is2FAError = isError(loginState, AuthInputFields.TwoFactor),
                    onUsernameChange = { viewModel?.onUsernameChanged(it) },
                    onPasswordChange = { viewModel?.onPasswordChanged(it) },
                    on2FAChange = { viewModel?.onTwoFactorChanged(it) },
                    on2FAInfoClick = { viewModel?.onTwoFactorHintClicked() }
                )
            }
            AuthType.HASHED -> {
                HashedLoginForm(
                    accountHash = accountHashDisplay,
                    isError = isError(loginState, AuthInputFields.Username),
                    is2FAError = isError(loginState, AuthInputFields.TwoFactor),
                    onHashValueChange = { viewModel?.onAccountHashChanged(it) },
                    onUploadClick = { viewModel?.onUploadHashClick() },
                    on2FAChange = { viewModel?.onTwoFactorChanged(it) },
                    on2FAInfoClick = { viewModel?.onTwoFactorHintClicked() }
                )
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
fun LoginHeroButton(viewModel: LoginViewModel? = null) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val isButtonEnabled by viewModel?.loginButtonEnabled?.collectAsState() ?: remember {
        mutableStateOf(false)
    }
    PrimaryButton(
        text = stringResource(com.windscribe.vpn.R.string.login),
        enabled = isButtonEnabled,
        onClick = {
            keyboardController?.hide()
            viewModel?.loginButtonClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .testTag("login_submit_button")
    )
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
private fun TwoFactorInfoDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.preferencesBackgroundColor,
            tonalElevation = 0.dp,
            modifier = Modifier.border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 0.dp, start = 16.dp, end = 16.dp)
                    .width(180.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(com.windscribe.vpn.R.string.two_fa),
                    style = font16.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primaryTextColor
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(com.windscribe.vpn.R.string.two_fa_description),
                    style = font12.copy(lineHeight = font12.fontSize * 1.4f),
                    color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Start
                )

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        stringResource(com.windscribe.vpn.R.string.ok),
                        style = font14.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primaryTextColor
                    )
                }
            }
        }
    }
}

@Composable
@MultiDevicePreview
fun LoginScreenPreview() {
    PreviewWithNav {
        LoginScreen()
    }
}