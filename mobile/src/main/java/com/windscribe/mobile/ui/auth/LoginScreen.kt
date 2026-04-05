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
import com.windscribe.mobile.ui.common.NextButton
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
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
                    onUsernameChange = { viewModel?.onUsernameChanged(it) },
                    onPasswordChange = { viewModel?.onPasswordChanged(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                AnimatedContent(
                    targetState = showTwoFactorTextField,
                    transitionSpec = {
                        fadeIn() + slideInVertically(initialOffsetY = { it }) togetherWith
                                fadeOut() + slideOutVertically(targetOffsetY = { it })
                    },
                    label = "2FAAnimation"
                ) { isTwoFactor ->
                    if (isTwoFactor) {
                        LoginTwoFactorTextField(loginState, viewModel)
                    } else {
                        ActionSheet(viewModel)
                    }
                }
            }
            AuthType.HASHED -> {
                HashedLoginForm(
                    accountHash = accountHashDisplay,
                    isError = isError(loginState, AuthInputFields.Username),
                    onHashValueChange = { viewModel?.onAccountHashChanged(it) },
                    onUploadClick = { viewModel?.onUploadHashClick() }
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

@Composable
fun LoginTwoFactorTextField(loginState: LoginState, viewModel: LoginViewModel? = null) {
    var text by remember { mutableStateOf("") }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp, start = 8.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(com.windscribe.vpn.R.string.two_fa),
                style = font16.copy(fontWeight = FontWeight.Medium),
                color = if (isError(loginState, AuthInputFields.TwoFactor)) AppColors.red else AppColors.white,
                modifier = Modifier.clickable {
                    viewModel?.onTwoFactorHintClicked()
                }
            )
            if (isError(loginState, AuthInputFields.TwoFactor)) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_input_error_icon),
                    contentDescription = stringResource(com.windscribe.vpn.R.string.two_fa),
                    tint = Color.Red,
                )
            }
        }

        Box {
            TextField(
                value = text,
                onValueChange = {
                    text = it
                    viewModel?.onTwoFactorChanged(it)
                },
                isError = isError(loginState, AuthInputFields.TwoFactor),
                singleLine = true,
                shape = RoundedCornerShape(9.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Number
                ),
                placeholder = {
                    Text(
                        text = stringResource(com.windscribe.vpn.R.string.enter_two_fa_code),
                        style = font16.copy(fontWeight = FontWeight.Normal),
                        color = AppColors.grayText,
                        textAlign = TextAlign.Start
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = if (isError(loginState, AuthInputFields.TwoFactor)) AppColors.red else AppColors.white,
                    unfocusedTextColor = if (isError(loginState, AuthInputFields.TwoFactor)) AppColors.red else AppColors.white,
                    disabledTextColor = if (isError(loginState, AuthInputFields.TwoFactor)) AppColors.red else AppColors.white,
                    unfocusedContainerColor = AppColors.white.copy(0.05f),
                    focusedContainerColor = AppColors.white.copy(0.05f),
                    disabledContainerColor = AppColors.white.copy(0.05f),
                    errorContainerColor = AppColors.white.copy(0.05f),
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    cursorColor = AppColors.white,
                    disabledIndicatorColor = Color.Transparent,
                    selectionColors = androidx.compose.foundation.text.selection.TextSelectionColors(
                        handleColor = AppColors.white,
                        backgroundColor = AppColors.white.copy(alpha = 0.3f)
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .then(
                        if (isError(loginState, AuthInputFields.TwoFactor)) Modifier.border(
                            width = 1.dp,
                            color = AppColors.red,
                            shape = RoundedCornerShape(9.dp)
                        ) else Modifier
                    ),
                textStyle = font16.copy(
                    color = if (isError(loginState, AuthInputFields.TwoFactor)) AppColors.red else AppColors.white,
                    textAlign = TextAlign.Start
                ),
            )
        }
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
            val url = NetworkKeyConstants.getWebsiteLink(NetworkKeyConstants.URL_FORGOT_PASSWORD)
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
private fun AppTextButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .clickable { onClick() }
    ) {
        Text(
            text = text,
            style = font16.copy(textAlign = TextAlign.Center),
            modifier = Modifier.align(Alignment.CenterStart),
            color = AppColors.white.copy(alpha = 0.50f),
        )
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