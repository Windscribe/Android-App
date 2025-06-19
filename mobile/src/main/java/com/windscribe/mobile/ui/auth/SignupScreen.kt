package com.windscribe.mobile.ui.auth

import NavBar
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
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
import com.windscribe.mobile.ui.nav.NavigationStack
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.common.AppBackground
import com.windscribe.mobile.ui.common.AppProgressBar
import com.windscribe.mobile.ui.common.AuthTextField
import com.windscribe.mobile.ui.common.CaptchaDebugDialog
import com.windscribe.mobile.ui.common.NextButton
import com.windscribe.mobile.ui.common.TextButton
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
                Toast.makeText(context, R.string.failed_network_alert, Toast.LENGTH_SHORT).show()
            }
        }
    }
    AppBackground {
        SignupCompactLayout(navController, signupState, viewModel)
        val showProgressBar = signupState is SignupState.Registering
        val message = (signupState as? SignupState.Registering)?.message ?: ""
        AppProgressBar(showProgressBar, message = message)
        if (signupState is SignupState.Captcha) {
            CaptchaDebugDialog(
                (signupState as SignupState.Captcha).request, onCancel = {
                    viewModel?.dismissCaptcha()
                },
                onSolutionSubmit = { t1, t2 ->
                    Log.i("LoginScreen", "onSolutionSubmit: $t1, $t2")
                    viewModel?.onCaptchaSolutionReceived(
                        CaptchaSolution(
                            t1,
                            t2,
                            (signupState as SignupState.Captcha).request.secureToken
                        )
                    )
                })
        }
    }
}

@Composable
private fun SignupCompactLayout(
    navController: NavController,
    signupState: SignupState,
    viewModel: SignupViewModel? = null
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp)
    ) {
        NavBar(stringResource(if (viewModel?.isAccountClaim == false) R.string.text_sign_up else R.string.sign_up)) {
            navController.popBackStack()
        }
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.Top
        ) {
            SignupUsernameTextField(signupState, viewModel)
            Spacer(modifier = Modifier.height(24.dp))
            SignupPasswordTextField(signupState, viewModel)
            Spacer(modifier = Modifier.height(8.dp))
            Description(stringResource(R.string.password_requirement))
            Spacer(modifier = Modifier.height(16.dp))
            SignupEmailTextField(signupState, viewModel)
            Spacer(modifier = Modifier.height(8.dp))
            Description(stringResource(R.string.email_description))
            if (viewModel?.isAccountClaim == false) {
                Spacer(modifier = Modifier.height(16.dp))
                VoucherTextField(viewModel)
                ExpandMenu(stringResource(R.string.referred_by_someone)) {
                    ReferralFeatures()
                    Spacer(modifier = Modifier.height(8.dp))
                    ReferralUsernameTextField(viewModel)
                }
            }
            if (signupState is SignupState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                SignupErrorText(signupState.error)
            }
            Spacer(modifier = Modifier.height(16.dp))
            SignupHeroButton(viewModel)
            Spacer(modifier = Modifier.height(8.dp))
            if (viewModel?.isAccountClaim == true) {
                TextButton(
                    stringResource(R.string.set_up_later),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .navigationBarsPadding()
                ) {
                    navController.popBackStack()
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SignupPasswordTextField(signupState: SignupState, viewModel: SignupViewModel? = null) {
    AuthTextField(
        hint = stringResource(R.string.password),
        placeHolder = stringResource(R.string.enter_password),
        isError = isError(signupState, AuthInputFields.Password),
        modifier = Modifier.fillMaxWidth(),
        isPassword = true,
        onValueChange = {
            viewModel?.onPasswordChanged(it)
        })
}

@Composable
private fun Description(text: String) {
    Text(
        text,
        style = font12,
        color = AppColors.white.copy(alpha = 0.50f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp),
        textAlign = TextAlign.Start
    )
}

@Composable
private fun ExpandMenu(text: String, content: @Composable () -> Unit = {}) {
    val expanded = remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val rotation by animateFloatAsState(
        if (expanded.value) 180f else 0f,
        label = "expandIconRotation"
    )
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Text(
                text = text,
                style = font16.copy(fontWeight = FontWeight.Bold),
                color = AppColors.white.copy(alpha = 0.50f),
            )
            Image(
                painter = painterResource(id = R.drawable.ic_expand),
                contentDescription = stringResource(id = R.string.image_description),
                modifier = Modifier
                    .size(32.dp)
                    .rotate(rotation)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = rememberRipple(bounded = false, color = Color.White),
                        onClick = { expanded.value = !expanded.value }
                    ),
                colorFilter = ColorFilter.tint(AppColors.white.copy(alpha = 0.50f))
            )
        }
        if (expanded.value) {
            content()
        }
    }
}

@Composable
private fun VoucherTextField(viewModel: SignupViewModel?) {
    AuthTextField(
        hint = stringResource(R.string.voucher_code) + " " + stringResource(R.string.optional),
        isError = false,
        modifier = Modifier.fillMaxWidth(),
        onValueChange = {
            viewModel?.onVoucherChanged(it)
        })
}

@Composable
private fun SignupUsernameTextField(signupState: SignupState, viewModel: SignupViewModel? = null) {
    AuthTextField(
        hint = stringResource(R.string.username),
        placeHolder = stringResource(R.string.enter_password),
        isError = isError(signupState, AuthInputFields.Username),
        modifier = Modifier.fillMaxWidth(),
        onValueChange = {
            viewModel?.onUsernameChanged(it)
        })
}

private fun isError(signupState: SignupState, field: AuthInputFields): Boolean {
    return (signupState as? SignupState.Error)?.error?.highlightedFields?.contains(field) ?: false
}

@Composable
private fun SignupEmailTextField(signupState: SignupState, viewModel: SignupViewModel? = null) {
    AuthTextField(
        hint = stringResource(R.string.email) + " " + stringResource(R.string.optional),
        placeHolder = stringResource(R.string.enter_email),
        isError = isError(signupState, AuthInputFields.Email),
        modifier = Modifier.fillMaxWidth(),
        onValueChange = {
            viewModel?.onEmailChanged(it)
        })
}

@Composable
private fun ReferralUsernameTextField(viewModel: SignupViewModel?) {
    AuthTextField(
        hint = stringResource(R.string.referral_username),
        isError = false,
        modifier = Modifier.fillMaxWidth(),
        onValueChange = {
            viewModel?.onReferralUsernameChanged(it)
        })
}

@Composable
private fun ReferralFeatures() {
    Column {
        ReferralFeature(stringResource(R.string.first_reason_to_use_referral))
        Spacer(modifier = Modifier.height(16.dp))
        ReferralFeature(stringResource(R.string.if_you_go_pro_they_ll_go_pro_too))
    }
}

@Composable
fun ReferralFeature(text: String) {
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_check),
            contentDescription = text,
            modifier = Modifier
                .size(16.dp),
        )
        Text(
            text,
            style = font12.copy(textAlign = TextAlign.Start),
            color = AppColors.white.copy(alpha = 0.50f),
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
private fun SignupHeroButton(viewModel: SignupViewModel? = null) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val isButtonEnabled by viewModel?.signupButtonEnabled?.collectAsState() ?: remember {
        mutableStateOf(false)
    }
    NextButton(
        text = stringResource(R.string.next), enabled = isButtonEnabled, onClick = {
            keyboardController?.hide()
            viewModel?.signupButtonClick()
        }, modifier = Modifier
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