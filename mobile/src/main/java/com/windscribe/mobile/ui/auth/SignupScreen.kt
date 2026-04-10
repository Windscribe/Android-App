package com.windscribe.mobile.ui.auth

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.common.AppBackground
import com.windscribe.mobile.ui.common.AppProgressBar
import com.windscribe.mobile.ui.common.CaptchaDebugDialog
import com.windscribe.mobile.ui.common.PrimaryButton
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SignupScreen(
    windowSizeClass: WindowSizeClass? = currentWindowAdaptiveInfo().windowSizeClass,
    viewModel: SignupViewModel? = null
) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val signupState by viewModel?.signupState?.collectAsState() ?: remember {
        mutableStateOf(SignupState.Idle)
    }
    viewModel?.isAccountClaim = navController.previousBackStackEntry
        ?.savedStateHandle
        ?.get<Boolean>("isAccountClaim") ?: false

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

    LaunchedEffect(signupState) {
        if (signupState is SignupState.Success) {
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
    LaunchedEffect(Unit) {
        viewModel?.toastMessage?.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    AppBackground {
        SignupCompactLayout(navController, signupState, viewModel)
        val showProgressBar = signupState is SignupState.Registering
        val message = (signupState as? SignupState.Registering)?.message ?: ""
        AppProgressBar(showProgressBar, message = message)
        if (signupState is SignupState.Captcha) {
            val captchaRequest = (signupState as SignupState.Captcha).request
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SignupCompactLayout(
    navController: NavController,
    signupState: SignupState,
    viewModel: SignupViewModel? = null
) {
    val context = LocalContext.current
    val selectedAuthType by viewModel?.selectedAuthType?.collectAsState() ?: remember {
        mutableStateOf(AuthType.STANDARD)
    }
    val accountHash by viewModel?.accountHash?.collectAsState() ?: remember {
        mutableStateOf("")
    }
    val isBackupConfirmed by viewModel?.isBackupConfirmed?.collectAsState() ?: remember {
        mutableStateOf(false)
    }
    val generatedUsername by viewModel?.generatedUsername?.collectAsState() ?: remember {
        mutableStateOf("")
    }
    val generatedPassword by viewModel?.generatedPassword?.collectAsState() ?: remember {
        mutableStateOf("")
    }
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    val imeHeight = ime.getBottom(density)
    val isKeyboardVisible = imeHeight > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
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
                        text = stringResource(com.windscribe.vpn.R.string.text_sign_up),
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
                    StandardSignupForm(
                        generatedUsername = generatedUsername,
                        generatedPassword = generatedPassword,
                        isUsernameError = isError(signupState, AuthInputFields.Username),
                        isPasswordError = isError(signupState, AuthInputFields.Password),
                        isEmailError = isError(signupState, AuthInputFields.Email),
                        onUsernameChange = { viewModel?.onUsernameChanged(it) },
                        onPasswordChange = { viewModel?.onPasswordChanged(it) },
                        onConfirmPasswordChange = { /* Handle confirm password */ },
                        onEmailChange = { viewModel?.onEmailChanged(it) },
                        onVoucherChange = { viewModel?.onVoucherChanged(it) },
                        onReferralUsernameChange = { viewModel?.onReferralUsernameChanged(it) },
                        onGenerateUsername = { viewModel?.generateUsername() },
                        onGeneratePassword = { viewModel?.generatePassword() },
                        onEmailInfoClick = { /* Handle email info click */ }
                    )
                }
                AuthType.HASHED -> {
                    HashedSignupForm(
                        accountHash = accountHash,
                        isBackupConfirmed = isBackupConfirmed,
                        onBackupConfirmedChanged = { viewModel?.onBackupConfirmedChanged(it) },
                        onVoucherChange = { viewModel?.onVoucherChanged(it) },
                        onRegenerateHash = { viewModel?.generateAccountHash() },
                        onUploadHash = { viewModel?.onUploadHashClick() },
                        onDownloadHash = { viewModel?.onDownloadHashClick(context) },
                        onCopyHash = {},
                        onLearnMoreClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://windscribe.net/knowledge-base/articles/hashed-login"))
                            context.startActivity(intent)
                        }
                    )
                }
            }

            if (signupState is SignupState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                SignupErrorText(signupState.error)
            }

            Spacer(modifier = Modifier.height(16.dp))

            SignupHeroButton(viewModel)
        }

        // Bottom "Already have account" link - Pinned to bottom, hide when keyboard is visible
        if (!isKeyboardVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 16.dp)
                    .clickable {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Start.route)
                        }
                    },
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(com.windscribe.vpn.R.string.already_have_account_log_in),
                    style = font16.copy(
                        fontWeight = FontWeight.Medium,
                        lineHeight = font16.fontSize * 1.5f
                    ),
                    color = AppColors.grayText
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

private fun isError(signupState: SignupState, field: AuthInputFields): Boolean {
    return (signupState as? SignupState.Error)?.error?.highlightedFields?.contains(field) ?: false
}

@Composable
private fun SignupHeroButton(viewModel: SignupViewModel? = null) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val isButtonEnabled by viewModel?.signupButtonEnabled?.collectAsState() ?: remember {
        mutableStateOf(false)
    }
    PrimaryButton(
        text = stringResource(com.windscribe.vpn.R.string.text_sign_up),
        enabled = isButtonEnabled,
        onClick = {
            keyboardController?.hide()
            viewModel?.signupButtonClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    )
}

@Composable
private fun SignupErrorText(errorType: AuthError) {
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
fun SignupScreenPreview() {
    PreviewWithNav {
        SignupScreen()
    }
}