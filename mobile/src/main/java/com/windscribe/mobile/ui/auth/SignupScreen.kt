package com.windscribe.mobile.ui.auth

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
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
 * Callbacks the signup UI can raise. Hoisted out so the stateless [SignupContent] never needs to
 * know about [SignupViewModel] — previews supply no-op lambdas.
 */
class SignupActions(
    val onAuthTypeChange: (AuthType) -> Unit = {},
    val onUsernameChange: (String) -> Unit = {},
    val onPasswordChange: (String) -> Unit = {},
    val onConfirmPasswordChange: (String) -> Unit = {},
    val onEmailChange: (String) -> Unit = {},
    val onVoucherChange: (String) -> Unit = {},
    val onReferralUsernameChange: (String) -> Unit = {},
    val onGenerateUsername: () -> Unit = {},
    val onGeneratePassword: () -> Unit = {},
    val onEmailInfoClick: () -> Unit = {},
    val onBackupConfirmedChanged: (Boolean) -> Unit = {},
    val onRegenerateHash: () -> Unit = {},
    val onUploadHash: () -> Unit = {},
    val onDownloadHash: () -> Unit = {},
    val onLearnMoreClick: () -> Unit = {},
    val onSignupClick: () -> Unit = {},
    val onCaptchaCancel: () -> Unit = {},
    val onCaptchaSolution: (CaptchaSolution) -> Unit = {},
    val onEmailInfoDismiss: () -> Unit = {},
)

/**
 * Stateful entry point. Owns the [SignupViewModel], collects its flows and wires up navigation /
 * file-picker / toast side effects, then delegates rendering to [SignupContent].
 */
@Composable
fun SignupScreen(viewModel: SignupViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val signupState by viewModel.signupState.collectAsState()
    val selectedAuthType by viewModel.selectedAuthType.collectAsState()
    val accountHash by viewModel.accountHash.collectAsState()
    val isBackupConfirmed by viewModel.isBackupConfirmed.collectAsState()
    val generatedUsername by viewModel.generatedUsername.collectAsState()
    val generatedPassword by viewModel.generatedPassword.collectAsState()
    val signupButtonEnabled by viewModel.signupButtonEnabled.collectAsState()
    val showEmailInfoDialog by viewModel.showEmailInfoDialog.collectAsState()

    viewModel.isAccountClaim = navController.previousBackStackEntry
        ?.savedStateHandle
        ?.get<Boolean>("isAccountClaim") ?: false

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

    LaunchedEffect(signupState) {
        if (signupState is SignupState.Success) {
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
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    SignupContent(
        navController = navController,
        signupState = signupState,
        selectedAuthType = selectedAuthType,
        accountHash = accountHash,
        isBackupConfirmed = isBackupConfirmed,
        generatedUsername = generatedUsername,
        generatedPassword = generatedPassword,
        signupButtonEnabled = signupButtonEnabled,
        showEmailInfoDialog = showEmailInfoDialog,
        actions =
            SignupActions(
                onAuthTypeChange = viewModel::onAuthTypeChanged,
                onUsernameChange = viewModel::onUsernameChanged,
                onPasswordChange = viewModel::onPasswordChanged,
                onConfirmPasswordChange = viewModel::onConfirmPasswordChanged,
                onEmailChange = viewModel::onEmailChanged,
                onVoucherChange = viewModel::onVoucherChanged,
                onReferralUsernameChange = viewModel::onReferralUsernameChanged,
                onGenerateUsername = viewModel::generateUsername,
                onGeneratePassword = viewModel::generatePassword,
                onEmailInfoClick = viewModel::onEmailInfoClick,
                onBackupConfirmedChanged = viewModel::onBackupConfirmedChanged,
                onRegenerateHash = viewModel::generateAccountHash,
                onUploadHash = viewModel::onUploadHashClick,
                onDownloadHash = { viewModel.onDownloadHashClick(context) },
                onLearnMoreClick = {
                    val intent =
                        Intent(
                            Intent.ACTION_VIEW,
                            "https://windscribe.net/knowledge-base/articles/hashed-login".toUri(),
                        )
                    context.startActivity(intent)
                },
                onSignupClick = viewModel::signupButtonClick,
                onCaptchaCancel = viewModel::dismissCaptcha,
                onCaptchaSolution = viewModel::onCaptchaSolutionReceived,
                onEmailInfoDismiss = viewModel::dismissEmailInfoDialog,
            ),
    )
}

/**
 * Stateless signup UI. Everything it needs is passed in, so it renders identically in the app and
 * in `@Preview`. This is the composable previews target.
 */
@Composable
fun SignupContent(
    navController: NavController,
    signupState: SignupState,
    selectedAuthType: AuthType,
    accountHash: String,
    isBackupConfirmed: Boolean,
    generatedUsername: String,
    generatedPassword: String,
    signupButtonEnabled: Boolean,
    showEmailInfoDialog: Boolean,
    actions: SignupActions,
) {
    AppBackground {
        SignupCompactLayout(
            navController = navController,
            signupState = signupState,
            selectedAuthType = selectedAuthType,
            accountHash = accountHash,
            isBackupConfirmed = isBackupConfirmed,
            generatedUsername = generatedUsername,
            generatedPassword = generatedPassword,
            signupButtonEnabled = signupButtonEnabled,
            actions = actions,
        )
        val showProgressBar = signupState is SignupState.Registering
        val message = (signupState as? SignupState.Registering)?.message ?: ""
        AppProgressBar(showProgressBar, message = message)
        if (signupState is SignupState.Captcha) {
            val captchaRequest = signupState.request
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

        if (showEmailInfoDialog) {
            EmailInfoDialog(onDismiss = actions.onEmailInfoDismiss)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SignupCompactLayout(
    navController: NavController,
    signupState: SignupState,
    selectedAuthType: AuthType,
    accountHash: String,
    isBackupConfirmed: Boolean,
    generatedUsername: String,
    generatedPassword: String,
    signupButtonEnabled: Boolean,
    actions: SignupActions,
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    val imeHeight = ime.getBottom(density)
    val isKeyboardVisible = imeHeight > 0

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
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
                        text = stringResource(com.windscribe.vpn.R.string.text_sign_up),
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
                    StandardSignupForm(
                        generatedUsername = generatedUsername,
                        generatedPassword = generatedPassword,
                        isUsernameError = isError(signupState, AuthInputFields.Username),
                        isPasswordError = isError(signupState, AuthInputFields.Password),
                        isConfirmPasswordError = isError(signupState, AuthInputFields.ConfirmPassword),
                        isEmailError = isError(signupState, AuthInputFields.Email),
                        isReferralError = isError(signupState, AuthInputFields.Referral),
                        onUsernameChange = actions.onUsernameChange,
                        onPasswordChange = actions.onPasswordChange,
                        onConfirmPasswordChange = actions.onConfirmPasswordChange,
                        onEmailChange = actions.onEmailChange,
                        onVoucherChange = actions.onVoucherChange,
                        onReferralUsernameChange = actions.onReferralUsernameChange,
                        onGenerateUsername = actions.onGenerateUsername,
                        onGeneratePassword = actions.onGeneratePassword,
                        onEmailInfoClick = actions.onEmailInfoClick,
                    )
                }

                AuthType.HASHED -> {
                    HashedSignupForm(
                        accountHash = accountHash,
                        isBackupConfirmed = isBackupConfirmed,
                        onBackupConfirmedChanged = actions.onBackupConfirmedChanged,
                        onVoucherChange = actions.onVoucherChange,
                        onRegenerateHash = actions.onRegenerateHash,
                        onUploadHash = actions.onUploadHash,
                        onDownloadHash = actions.onDownloadHash,
                        onCopyHash = {},
                        onLearnMoreClick = actions.onLearnMoreClick,
                    )
                }
            }

            if (signupState is SignupState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                SignupErrorText(signupState.error)
            }

            Spacer(modifier = Modifier.height(16.dp))

            SignupHeroButton(signupButtonEnabled, actions.onSignupClick)
        }

        // Bottom "Already have account" link - Pinned to bottom, hide when keyboard is visible
        if (!isKeyboardVisible) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(vertical = 16.dp)
                        .clickable {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Start.route)
                            }
                        },
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(com.windscribe.vpn.R.string.already_have_account_log_in),
                    style =
                        font16.copy(
                            fontWeight = FontWeight.Medium,
                            lineHeight = font16.fontSize * 1.5f,
                        ),
                    color = AppColors.grayText,
                )
                Spacer(modifier = Modifier.size(2.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_right_small),
                    contentDescription = null,
                    tint = AppColors.grayText,
                )
            }
        }
    }
}

private fun isError(
    signupState: SignupState,
    field: AuthInputFields,
): Boolean = (signupState as? SignupState.Error)?.error?.highlightedFields?.contains(field) ?: false

@Composable
private fun SignupHeroButton(
    isButtonEnabled: Boolean,
    onSignupClick: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    PrimaryButton(
        text = stringResource(com.windscribe.vpn.R.string.text_sign_up),
        enabled = isButtonEnabled,
        onClick = {
            keyboardController?.hide()
            onSignupClick()
        },
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
    )
}

@Composable
private fun SignupErrorText(errorType: AuthError) {
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
private fun EmailInfoDialog(onDismiss: () -> Unit) {
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
                    text = stringResource(com.windscribe.vpn.R.string.add_email),
                    style = font16.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primaryTextColor,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(com.windscribe.vpn.R.string.email_description),
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
 * Feeds representative [SignupState] values into the preview. The renderer draws [SignupContent]
 * once per value.
 */
private class SignupStateProvider : PreviewParameterProvider<SignupState> {
    override val values =
        sequenceOf(
            SignupState.Idle,
            SignupState.Registering("Creating account..."),
            SignupState.Error(
                AuthError.InputError(
                    "Invalid username",
                    listOf(AuthInputFields.Username),
                ),
            ),
        )
}

@Composable
@MultiDevicePreview
private fun SignupContentPreview(
    @PreviewParameter(SignupStateProvider::class) signupState: SignupState,
) {
    PreviewWithNav {
        SignupContent(
            navController = LocalNavController.current,
            signupState = signupState,
            selectedAuthType = AuthType.STANDARD,
            accountHash = "",
            isBackupConfirmed = false,
            generatedUsername = "",
            generatedPassword = "",
            signupButtonEnabled = signupState is SignupState.Idle,
            showEmailInfoDialog = false,
            actions = SignupActions(),
        )
    }
}
