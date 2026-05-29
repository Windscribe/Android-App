package com.windscribe.mobile.ui.auth

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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

/**
 * Callbacks the login UI can raise. Hoisted out of the composables so the stateless
 * [LoginContent] never needs to know about [LoginViewModel] — previews supply no-op lambdas.
 */
class LoginActions(
    val onUsernameChange: (String) -> Unit = {},
    val onPasswordChange: (String) -> Unit = {},
    val onTwoFactorChange: (String) -> Unit = {},
    val onTwoFactorInfoClick: () -> Unit = {},
    val onAuthTypeChange: (AuthType) -> Unit = {},
    val onAccountHashChange: (String) -> Unit = {},
    val onUploadHashClick: () -> Unit = {},
    val onLoginClick: () -> Unit = {},
    val onCaptchaCancel: () -> Unit = {},
    val onCaptchaSolution: (CaptchaSolution) -> Unit = {},
    val onTwoFactorInfoDismiss: () -> Unit = {},
)

/**
 * Stateful entry point. Owns the [LoginViewModel], collects its flows and wires up
 * navigation / file-picker side effects, then delegates rendering to [LoginContent].
 */
@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel()) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val loginState by viewModel.loginState.collectAsState()
    val selectedAuthType by viewModel.selectedAuthType.collectAsState()
    val accountHashDisplay by viewModel.accountHashDisplay.collectAsState()
    val loginButtonEnabled by viewModel.loginButtonEnabled.collectAsState()
    val showTwoFactorInfoDialog by viewModel.showTwoFactorInfoDialog.collectAsState()

    val filePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri ->
            uri?.let { viewModel.onFileSelected(context, it) }
        }

    LaunchedEffect(Unit) {
        viewModel.triggerFilePicker.collect { trigger ->
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
        viewModel.showAllBackupFailedDialog.collect { show ->
            if (show) {
                navController.navigate(Screen.AllProtocolFailedDialog.route)
                viewModel.clearDialog()
            }
        }
    }

    LoginContent(
        navController = navController,
        loginState = loginState,
        selectedAuthType = selectedAuthType,
        accountHashDisplay = accountHashDisplay,
        loginButtonEnabled = loginButtonEnabled,
        showTwoFactorInfoDialog = showTwoFactorInfoDialog,
        actions =
            LoginActions(
                onUsernameChange = viewModel::onUsernameChanged,
                onPasswordChange = viewModel::onPasswordChanged,
                onTwoFactorChange = viewModel::onTwoFactorChanged,
                onTwoFactorInfoClick = viewModel::onTwoFactorHintClicked,
                onAuthTypeChange = viewModel::onAuthTypeChanged,
                onAccountHashChange = viewModel::onAccountHashChanged,
                onUploadHashClick = viewModel::onUploadHashClick,
                onLoginClick = viewModel::loginButtonClick,
                onCaptchaCancel = viewModel::dismissCaptcha,
                onCaptchaSolution = viewModel::onCaptchaSolutionReceived,
                onTwoFactorInfoDismiss = viewModel::dismissTwoFactorInfoDialog,
            ),
    )
}

/**
 * Stateless login UI. Everything it needs is passed in, so it renders identically in the
 * app and in `@Preview`. This is the composable previews target.
 */
@Composable
fun LoginContent(
    navController: NavController,
    loginState: LoginState,
    selectedAuthType: AuthType,
    accountHashDisplay: String,
    loginButtonEnabled: Boolean,
    showTwoFactorInfoDialog: Boolean,
    actions: LoginActions,
) {
    AppBackground {
        LoginCompactLayout(
            navController = navController,
            loginState = loginState,
            selectedAuthType = selectedAuthType,
            accountHashDisplay = accountHashDisplay,
            loginButtonEnabled = loginButtonEnabled,
            actions = actions,
        )
        val showProgressBar = loginState is LoginState.LoggingIn
        val message = (loginState as? LoginState.LoggingIn)?.message ?: ""
        AppProgressBar(showProgressBar, message = message)
        if (loginState is LoginState.Captcha) {
            val captchaRequest = loginState.request
            CaptchaDebugDialog(
                captchaRequest,
                onCancel = actions.onCaptchaCancel,
                onSolutionSubmit = { t1, t2 ->
                    Log.i("LoginScreen", "onSolutionSubmit: $t1, $t2")
                    actions.onCaptchaSolution(
                        CaptchaSolution(
                            t1,
                            t2,
                            captchaRequest.secureToken,
                        ),
                    )
                },
            )
        }

        if (showTwoFactorInfoDialog) {
            TwoFactorInfoDialog(onDismiss = actions.onTwoFactorInfoDismiss)
        }
    }
}

@Composable
fun LoginCompactLayout(
    navController: NavController,
    loginState: LoginState,
    selectedAuthType: AuthType,
    accountHashDisplay: String,
    loginButtonEnabled: Boolean,
    actions: LoginActions,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .imePadding(),
        verticalArrangement = Arrangement.Top,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header with title and tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier.size(24.dp).clickable {
                            navController.popBackStack()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back_arrow),
                        contentDescription = stringResource(com.windscribe.vpn.R.string.back),
                        tint = AppColors.white,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = stringResource(com.windscribe.vpn.R.string.login),
                    style = font16.copy(fontWeight = FontWeight.SemiBold),
                    color = AppColors.white,
                )
            }

            AuthTabSelector(
                selectedTab = selectedAuthType,
                onTabSelected = actions.onAuthTypeChange,
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
                    onUsernameChange = actions.onUsernameChange,
                    onPasswordChange = actions.onPasswordChange,
                    on2FAChange = actions.onTwoFactorChange,
                    on2FAInfoClick = actions.onTwoFactorInfoClick,
                )
            }

            AuthType.HASHED -> {
                HashedLoginForm(
                    accountHash = accountHashDisplay,
                    isError = isError(loginState, AuthInputFields.Username),
                    is2FAError = isError(loginState, AuthInputFields.TwoFactor),
                    onHashValueChange = actions.onAccountHashChange,
                    onUploadClick = actions.onUploadHashClick,
                    on2FAChange = actions.onTwoFactorChange,
                    on2FAInfoClick = actions.onTwoFactorInfoClick,
                )
            }
        }

        if (loginState is LoginState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            ErrorText(loginState.errorType)
        }
        Spacer(modifier = Modifier.height(16.dp))
        LoginHeroButton(
            isButtonEnabled = loginButtonEnabled,
            onLoginClick = actions.onLoginClick,
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun isError(
    loginState: LoginState,
    field: AuthInputFields,
): Boolean = (loginState as? LoginState.Error)?.errorType?.highlightedFields?.contains(field) ?: false

@Composable
fun LoginHeroButton(
    isButtonEnabled: Boolean,
    onLoginClick: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    PrimaryButton(
        text = stringResource(com.windscribe.vpn.R.string.login),
        enabled = isButtonEnabled,
        onClick = {
            keyboardController?.hide()
            onLoginClick()
        },
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .testTag("login_submit_button"),
    )
}

@Composable
fun ErrorText(errorType: AuthError) {
    val message =
        when (errorType) {
            is AuthError.LocalizedInputError -> stringResource(errorType.error)
            is AuthError.InputError -> errorType.error
        }
    if (message.isNotBlank()) {
        Text(
            text = message,
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
}

@Composable
private fun TwoFactorInfoDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.preferencesBackgroundColor,
            tonalElevation = 0.dp,
            modifier =
                Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                ),
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(top = 16.dp, bottom = 0.dp, start = 16.dp, end = 16.dp)
                        .width(180.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = stringResource(com.windscribe.vpn.R.string.two_fa),
                    style = font16.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primaryTextColor,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(com.windscribe.vpn.R.string.two_fa_description),
                    style = font12.copy(lineHeight = font12.fontSize * 1.4f),
                    color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Start,
                )

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(
                        stringResource(com.windscribe.vpn.R.string.ok),
                        style = font14.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primaryTextColor,
                    )
                }
            }
        }
    }
}

/**
 * Feeds representative [LoginState] values into the preview. The renderer draws [LoginContent]
 * once per value, so the preview pane shows the idle, logging-in and error states side by side.
 */
private class LoginStateProvider : PreviewParameterProvider<LoginState> {
    override val values =
        sequenceOf(
            LoginState.Idle,
            LoginState.LoggingIn("Logging in..."),
            LoginState.Error(
                AuthError.InputError(
                    "Invalid username or password",
                    listOf(AuthInputFields.Username, AuthInputFields.Password),
                ),
            ),
        )
}

@Composable
@MultiDevicePreview
private fun LoginContentPreview(
    @PreviewParameter(LoginStateProvider::class) loginState: LoginState,
) {
    PreviewWithNav {
        LoginContent(
            navController = LocalNavController.current,
            loginState = loginState,
            selectedAuthType = AuthType.STANDARD,
            accountHashDisplay = "",
            loginButtonEnabled = loginState is LoginState.Idle,
            showTwoFactorInfoDialog = false,
            actions = LoginActions(),
        )
    }
}
