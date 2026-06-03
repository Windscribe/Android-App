package com.windscribe.mobile.ui.preferences.account

import PreferencesNavBar
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.windscribe.mobile.ui.AppStartActivity
import com.windscribe.mobile.ui.common.NextButton
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.common.PreferenceProgressBar
import com.windscribe.mobile.ui.common.openUrl
import com.windscribe.mobile.ui.connection.ToastMessage
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.helper.hapticClickable
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.mobile.ui.popup.FullScreenDialogState
import com.windscribe.mobile.ui.popup.HandleFullScreenDialog
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.backgroundColor
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font14
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.preferencesBackgroundColor
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.R
import com.windscribe.vpn.Windscribe.Companion.appContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Callbacks the account UI can raise. Hoisted out of the composables so the stateless
 * [AccountContent] never needs to know about [AccountViewModel] — previews supply no-op lambdas.
 */
class AccountActions(
    val onManageAccountClicked: () -> Unit = {},
    val onResetPasswordClicked: () -> Unit = {},
    val onVoucherCodeClicked: () -> Unit = {},
    val onLazyLoginClicked: () -> Unit = {},
    val onEnterVoucherCode: (String) -> Unit = {},
    val onEnterLazyLoginCode: (String) -> Unit = {},
    val onDialogDismiss: () -> Unit = {},
)

/**
 * Stateful entry point. Owns the [AccountViewModel], collects its flows, then delegates rendering
 * to [AccountContent].
 */
@Composable
fun AccountScreen(viewModel: AccountViewModel = hiltViewModel<AccountViewModelImpl>()) {
    val showProgress by viewModel.showProgress.collectAsState()
    val isGhostAccount by viewModel.isGhostAccount.collectAsState()
    val accountState by viewModel.accountState.collectAsState()
    val isSsoLogin by viewModel.isSsoLogin.collectAsState()
    AccountContent(
        showProgress = showProgress,
        isGhostAccount = isGhostAccount,
        accountState = accountState,
        isSsoLogin = isSsoLogin,
        alertState = viewModel.alertState,
        goTo = viewModel.goTo,
        actions =
            AccountActions(
                onManageAccountClicked = viewModel::onManageAccountClicked,
                onResetPasswordClicked = viewModel::onResetPasswordClicked,
                onVoucherCodeClicked = viewModel::onVoucherCodeClicked,
                onLazyLoginClicked = viewModel::onLazyLoginClicked,
                onEnterVoucherCode = viewModel::onEnterVoucherCode,
                onEnterLazyLoginCode = viewModel::onEnterLazyLoginCode,
                onDialogDismiss = viewModel::onDialogDismiss,
            ),
    )
}

/**
 * Stateless account UI. Everything it needs is passed in, so it renders identically in the app and
 * in `@Preview`. This is the composable previews target.
 */
@Composable
fun AccountContent(
    showProgress: Boolean,
    isGhostAccount: Boolean,
    accountState: AccountState,
    isSsoLogin: Boolean,
    alertState: Flow<AlertState>,
    goTo: Flow<AccountGoTo>,
    actions: AccountActions,
) {
    val navController = LocalNavController.current
    val scrollState = rememberScrollState()
    PreferenceBackground {
        Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)) {
            PreferencesNavBar(stringResource(R.string.my_account)) {
                navController.popBackStack()
            }
            if (isGhostAccount) {
                GhostAccountState()
                return@Column
            }
            Column(
                Modifier
                    .navigationBarsPadding()
                    .verticalScroll(scrollState),
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                AccountInfo(accountState)
                Spacer(modifier = Modifier.height(14.dp))
                PlanInfo(accountState)
                if (isSsoLogin && accountState.emailState is EmailState.Email) {
                    Spacer(modifier = Modifier.height(14.dp))
                    ActionButton(stringResource(R.string.reset_password)) {
                        actions.onResetPasswordClicked()
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    stringResource(R.string.other),
                    style =
                        font12.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.preferencesSubtitleColor,
                        ),
                )
                Spacer(modifier = Modifier.height(8.dp))
                VoucherCode(actions.onVoucherCodeClicked)
                Spacer(modifier = Modifier.height(14.dp))
                LazyLogin(actions.onLazyLoginClicked)
            }
        }
        PreferenceProgressBar(showProgressBar = showProgress)
        HandleGoto(goTo)
        HandleAlertState(alertState, actions)
    }
}

@Composable
private fun GhostAccountState() {
    val navController = LocalNavController.current
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
    ) {
        Spacer(modifier = Modifier.weight(1.0f))
        NextButton(
            modifier = Modifier,
            text = stringResource(R.string.login),
            enabled = true,
        ) {
            navController.navigate(Screen.Login.route)
        }
        Spacer(modifier = Modifier.height(16.dp))
        NextButton(
            modifier = Modifier,
            text = stringResource(R.string.account_set_up),
            enabled = true,
        ) {
            navController.currentBackStackEntry?.savedStateHandle?.set("isAccountClaim", true)
            navController.navigate(Screen.Signup.route)
        }
        Spacer(modifier = Modifier.weight(1.0f))
    }
}

@Composable
private fun HandleAlertState(
    alertStateFlow: Flow<AlertState>,
    actions: AccountActions,
) {
    val activity = LocalActivity.current as? AppStartActivity
    val alertState by alertStateFlow.collectAsState(initial = AlertState.None)
    var showVoucherDialog by remember { mutableStateOf(false) }
    var showLazyLoginDialog by remember { mutableStateOf(false) }
    var fullScreenDialogState by remember { mutableStateOf<FullScreenDialogState>(FullScreenDialogState.None) }

    LaunchedEffect(alertState) {
        when (alertState) {
            is AlertState.Error -> {
                val result = (alertState as AlertState.Error).message
                val message =
                    when (result) {
                        is ToastMessage.Raw -> result.message
                        is ToastMessage.Localized -> activity?.getString(result.message) ?: ""
                        else -> ""
                    }
                fullScreenDialogState = FullScreenDialogState.Error(message)
            }

            is AlertState.Success -> {
                val result = (alertState as AlertState.Success).message
                val message =
                    when (result) {
                        is ToastMessage.Raw -> result.message
                        is ToastMessage.Localized -> activity?.getString(result.message) ?: ""
                        else -> ""
                    }
                fullScreenDialogState = FullScreenDialogState.Success(message)
            }

            is AlertState.LazyLogin -> {
                showLazyLoginDialog = true
            }

            is AlertState.VoucherCode -> {
                showVoucherDialog = true
            }

            else -> {}
        }
    }

    HandleFullScreenDialog(
        state = fullScreenDialogState,
        onDismiss = {
            fullScreenDialogState = FullScreenDialogState.None
            actions.onDialogDismiss()
        },
    )

    if (showVoucherDialog) {
        TextFieldDialog(onDismiss = {
            showVoucherDialog = false
            actions.onDialogDismiss()
        }, onSubmit = {
            actions.onEnterVoucherCode(it)
        })
    }

    if (showLazyLoginDialog) {
        TextFieldDialog(onDismiss = {
            showLazyLoginDialog = false
            actions.onDialogDismiss()
        }, onSubmit = {
            actions.onEnterLazyLoginCode(it)
        }, true)
    }
}

@Composable
private fun TextFieldDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    isLazyLogin: Boolean = false,
) {
    val activity = LocalActivity.current as? AppStartActivity
    val hapticFeedbackEnabled by activity?.viewmodel?.hapticFeedback?.collectAsState()
        ?: remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    LaunchedEffect(textFieldValue.text) {
        if (isLazyLogin && textFieldValue.text.replace("-", "").length == 8) {
            onDismiss()
            onSubmit(textFieldValue.text)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryTextColor,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TextField(
                    value = textFieldValue,
                    onValueChange = { input ->
                        if (isLazyLogin) {
                            val clean =
                                input.text
                                    .uppercase()
                                    .replace("-", "")
                                    .take(8)

                            val formatted =
                                if (clean.length > 4) {
                                    "${clean.substring(0, 4)}-${clean.substring(4)}"
                                } else {
                                    clean
                                }
                            textFieldValue =
                                if (formatted != textFieldValue.text) {
                                    TextFieldValue(
                                        text = formatted,
                                        selection = TextRange(formatted.length),
                                    )
                                } else {
                                    input
                                }
                        } else {
                            textFieldValue = input
                        }
                    },
                    colors =
                        TextFieldDefaults.colors().copy(
                            focusedContainerColor = MaterialTheme.colorScheme.primaryTextColor,
                            unfocusedContainerColor = MaterialTheme.colorScheme.primaryTextColor,
                            disabledContainerColor = MaterialTheme.colorScheme.primaryTextColor,
                            focusedTextColor = MaterialTheme.colorScheme.preferencesBackgroundColor,
                            unfocusedTextColor = MaterialTheme.colorScheme.preferencesBackgroundColor,
                            disabledTextColor = MaterialTheme.colorScheme.preferencesBackgroundColor,
                            cursorColor = MaterialTheme.colorScheme.backgroundColor,
                        ),
                    placeholder = {
                        Text(
                            stringResource(R.string.enter_code),
                            style =
                                font16.copy(
                                    color = MaterialTheme.colorScheme.preferencesBackgroundColor,
                                    textAlign = TextAlign.Start,
                                ),
                        )
                    },
                    textStyle =
                        font16.copy(
                            color = MaterialTheme.colorScheme.preferencesBackgroundColor,
                            textAlign = TextAlign.Start,
                        ),
                    keyboardOptions =
                        KeyboardOptions.Default.copy(
                            capitalization = KeyboardCapitalization.Characters,
                        ),
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Spacer(modifier = Modifier.weight(1.0f))
                    Button(
                        onClick = {
                            if (hapticFeedbackEnabled) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
                            }
                            onDismiss()
                        },
                        colors =
                            ButtonColors(
                                containerColor = Color.Transparent,
                                contentColor =
                                    MaterialTheme.colorScheme.preferencesBackgroundColor.copy(
                                        alpha = 0.70f,
                                    ),
                                disabledContainerColor = Color.Transparent,
                                disabledContentColor = Color.Transparent,
                            ),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text(
                            stringResource(R.string.cancel),
                            style = font16,
                            color = MaterialTheme.colorScheme.preferencesBackgroundColor,
                            textAlign = TextAlign.Start,
                        )
                    }
                    Button(
                        onClick = {
                            if (hapticFeedbackEnabled) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                            }
                            onDismiss()
                            onSubmit(textFieldValue.text)
                        },
                        colors =
                            ButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.preferencesBackgroundColor,
                                disabledContainerColor = Color.Transparent,
                                disabledContentColor = Color.Transparent,
                            ),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text(
                            stringResource(R.string.text_ok),
                            style = font16,
                            textAlign = TextAlign.Start,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountInfo(accountState: AccountState) {
    val account = accountState as? AccountState.Account ?: return

    val username = account.username
    val emailState = account.emailState
    val navController = LocalNavController.current
    val context = LocalContext.current
    val isHashedAccount = username.startsWith("0x")

    Column {
        Text(
            stringResource(R.string.info),
            style =
                font12.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.preferencesSubtitleColor,
                ),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Username/Hash Row
        Row(
            modifier =
                Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                        shape =
                            if (isHashedAccount) {
                                RoundedCornerShape(12.dp)
                            } else {
                                RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                            },
                    ).hapticClickable {
                        val clipboard =
                            context.getSystemService(
                                android.content.Context.CLIPBOARD_SERVICE,
                            ) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText(if (isHashedAccount) "Hash" else "Username", username)
                        clipboard.setPrimaryClip(clip)
                    }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (isHashedAccount) stringResource(R.string.hash) else stringResource(R.string.username),
                style =
                    font16.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primaryTextColor,
                    ),
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isHashedAccount) {
                // Display hash in two lines (34 chars total: 18 + 16)
                val hashText =
                    if (username.length > 18) {
                        "${username.substring(0, 18)}\n${username.substring(18)}"
                    } else {
                        username
                    }
                Text(
                    hashText,
                    style =
                        font16.copy(
                            color = MaterialTheme.colorScheme.preferencesSubtitleColor,
                            textAlign = TextAlign.End,
                            lineHeight = 22.sp,
                        ),
                    maxLines = 2,
                )
            } else {
                Text(
                    username,
                    style = font16.copy(color = MaterialTheme.colorScheme.preferencesSubtitleColor),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (!isHashedAccount) {
            Spacer(modifier = Modifier.height(1.dp))

            // Email Section
            Column(
                modifier =
                    Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                        ).padding(14.dp)
                        .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (emailState) {
                        is EmailState.NoEmail -> {
                            Icon(
                                painterResource(com.windscribe.mobile.R.drawable.ic_email_attention),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primaryTextColor,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        is EmailState.UnconfirmedEmail -> {
                            Icon(
                                painterResource(com.windscribe.mobile.R.drawable.ic_email_attention),
                                contentDescription = null,
                                tint = AppColors.yellow,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        else -> {}
                    }

                    Text(
                        stringResource(R.string.email),
                        style =
                            font16.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primaryTextColor,
                            ),
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    ValueItem(
                        when (emailState) {
                            is EmailState.Email -> emailState.email
                            is EmailState.UnconfirmedEmail -> emailState.email
                            is EmailState.NoEmail -> stringResource(R.string.none)
                        },
                    )
                }

                when (emailState) {
                    is EmailState.NoEmail -> {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            stringResource(R.string.get_10gb_data),
                            style =
                                font14.copy(
                                    color = MaterialTheme.colorScheme.preferencesSubtitleColor,
                                    fontWeight = FontWeight.Normal,
                                    textAlign = TextAlign.Start,
                                ),
                            modifier =
                                Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(6.dp),
                                    ).padding(horizontal = 12.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                        )
                    }

                    is EmailState.UnconfirmedEmail -> {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier =
                                Modifier
                                    .background(AppColors.yellow, shape = RoundedCornerShape(6.dp))
                                    .clickable {
                                        navController.navigate(Screen.ConfirmEmail.route)
                                    }.padding(horizontal = 12.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.confirm_your_email),
                                style =
                                    font14.copy(
                                        color = Color.Black,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Start,
                                    ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                stringResource(R.string.resend),
                                style =
                                    font16.copy(
                                        color = Color.Black,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                maxLines = 1,
                            )
                        }
                    }

                    else -> {}
                }
            }

            if (emailState is EmailState.NoEmail) {
                Spacer(modifier = Modifier.height(14.dp))
                ActionButton("${stringResource(R.string.add_email)} (${stringResource(R.string.get_10_gb)})") {
                    navController.navigate(Screen.AddEmail.route)
                }
            }
        }
    }
}

@Composable
private fun RowScope.ValueItem(value: String) {
    Text(
        value,
        style =
            font16.copy(
                color = MaterialTheme.colorScheme.preferencesSubtitleColor,
                textAlign = TextAlign.End,
            ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
    )
}

@Composable
private fun PlanInfo(accountState: AccountState) {
    val navController = LocalNavController.current
    if (accountState !is AccountState.Account) {
        return
    }
    val type = accountState.type
    val plan =
        when (type) {
            is AccountType.Pro, is AccountType.Unlimited -> stringResource(R.string.unlimited_data)
            is AccountType.Free -> type.data
            is AccountType.AlcCustom -> type.data
        }
    val planType =
        when (type) {
            is AccountType.Pro -> stringResource(R.string.pro)
            is AccountType.Unlimited -> stringResource(R.string.a_la_carte_unlimited_plan)
            is AccountType.AlcCustom -> stringResource(R.string.custom)
            is AccountType.Free -> stringResource(R.string.free)
        }
    val resetDateBackground =
        when (type) {
            is AccountType.Pro, is AccountType.Unlimited -> {
                RoundedCornerShape(
                    bottomStart = 12.dp,
                    bottomEnd = 12.dp,
                )
            }

            is AccountType.Free, is AccountType.AlcCustom -> {
                RoundedCornerShape(0.dp)
            }
        }
    val dateType = accountState.dateType
    Column {
        Text(
            stringResource(R.string.plan),
            style =
                font12.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.preferencesSubtitleColor,
                ),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier =
                Modifier
                    .background(
                        color =
                            MaterialTheme.colorScheme.primaryTextColor.copy(
                                alpha = 0.05f,
                            ),
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    ).padding(vertical = 14.dp, horizontal = 14.dp),
        ) {
            Text(
                plan,
                style =
                    font16.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primaryTextColor,
                    ),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                planType,
                style =
                    font16.copy(
                        color =
                            if (type is AccountType.Free ||
                                type is AccountType.AlcCustom
                            ) {
                                MaterialTheme.colorScheme.primaryTextColor
                            } else {
                                AppColors.cyberBlue
                            },
                    ),
            )
        }
        Spacer(modifier = Modifier.height(1.dp))
        Row(
            modifier =
                Modifier
                    .background(
                        color =
                            MaterialTheme.colorScheme.primaryTextColor.copy(
                                alpha = 0.05f,
                            ),
                        shape = resetDateBackground,
                    ).padding(vertical = 14.dp, horizontal = 14.dp),
        ) {
            Text(
                if (dateType is DateType.Expiry) {
                    stringResource(R.string.expiry_date)
                } else {
                    stringResource(
                        R.string.reset_date,
                    )
                },
                style =
                    font16.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primaryTextColor,
                    ),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                dateType.date,
                style = font16.copy(color = MaterialTheme.colorScheme.preferencesSubtitleColor),
            )
        }
        if (type is AccountType.Free || type is AccountType.AlcCustom) {
            Spacer(modifier = Modifier.height(1.dp))
            Row(
                modifier =
                    Modifier
                        .background(
                            color =
                                MaterialTheme.colorScheme.primaryTextColor.copy(
                                    alpha = 0.05f,
                                ),
                            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                        ).padding(vertical = 14.dp, horizontal = 14.dp),
            ) {
                Text(
                    stringResource(R.string.data_left_in_your_plan),
                    style =
                        font16.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primaryTextColor,
                        ),
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    accountState.dataLeft,
                    style = font16.copy(color = MaterialTheme.colorScheme.preferencesSubtitleColor),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            ActionButton(
                stringResource(R.string.upgrade_to_pro),
                textColor = AppColors.cyberBlue,
                backgroundColor = AppColors.cyberBlue.copy(alpha = 0.05f),
            ) {
                navController.navigate(Screen.Upgrade.route)
            }
        }
    }
}

@Composable
private fun ActionButton(
    title: String,
    textColor: Color = MaterialTheme.colorScheme.primaryTextColor,
    backgroundColor: Color =
        MaterialTheme.colorScheme.primaryTextColor.copy(
            alpha = 0.05f,
        ),
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(size = 12.dp),
                ).hapticClickable {
                    onClick()
                }.padding(vertical = 14.dp, horizontal = 14.dp),
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            title,
            style =
                font16.copy(
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                ),
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun VoucherCode(onVoucherCodeClicked: () -> Unit) {
    Column(
        modifier =
            Modifier
                .background(
                    color =
                        MaterialTheme.colorScheme.primaryTextColor.copy(
                            alpha = 0.05f,
                        ),
                    shape = RoundedCornerShape(size = 12.dp),
                ).hapticClickable {
                    onVoucherCodeClicked()
                }.padding(vertical = 14.dp, horizontal = 14.dp),
    ) {
        Row {
            Text(
                stringResource(R.string.voucher_code),
                style =
                    font16.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primaryTextColor,
                    ),
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(com.windscribe.mobile.R.drawable.arrow_right),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primaryTextColor,
            )
        }
        Spacer(modifier = Modifier.height(13.5.dp))
        Text(
            text = stringResource(R.string.apply_voucher_code),
            style = font14.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.preferencesSubtitleColor,
            textAlign = TextAlign.Start,
        )
    }
}

@Composable
private fun LazyLogin(onLazyLoginClicked: () -> Unit) {
    Column(
        modifier =
            Modifier
                .background(
                    color =
                        MaterialTheme.colorScheme.primaryTextColor.copy(
                            alpha = 0.05f,
                        ),
                    shape = RoundedCornerShape(size = 12.dp),
                ).hapticClickable {
                    onLazyLoginClicked()
                }.padding(vertical = 14.dp, horizontal = 14.dp),
    ) {
        Row {
            Text(
                stringResource(R.string.xpress_login),
                style =
                    font16.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primaryTextColor,
                    ),
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(com.windscribe.mobile.R.drawable.arrow_right),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primaryTextColor,
            )
        }
        Spacer(modifier = Modifier.height(13.5.dp))
        Text(
            text = stringResource(R.string.lazy_login_description),
            style = font14.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.preferencesSubtitleColor,
            textAlign = TextAlign.Start,
        )
    }
}

@Composable
private fun HandleGoto(gotoFlow: Flow<AccountGoTo>) {
    val activity = LocalActivity.current as? AppStartActivity
    val navController = LocalNavController.current
    val goto by gotoFlow.collectAsState(initial = AccountGoTo.None)
    LaunchedEffect(goto) {
        when (goto) {
            is AccountGoTo.ManageAccount -> {
                activity?.openUrl((goto as AccountGoTo.ManageAccount).url)
            }

            is AccountGoTo.Error -> {
                Toast
                    .makeText(
                        activity,
                        (goto as AccountGoTo.Error).message,
                        Toast.LENGTH_SHORT,
                    ).show()
            }

            is AccountGoTo.Upgrade -> {
                // The upgrade view model reads the promo from the app lifecycle observer, so stash it
                // there before routing (replaces the old PROMO_EXTRA intent extra).
                appContext.appLifeCycleObserver.pushNotificationAction =
                    (goto as AccountGoTo.Upgrade).promoAction
                navController.navigate(Screen.Upgrade.route)
            }

            else -> {}
        }
    }
}

/**
 * Feeds representative [AccountState] values into the preview so the renderer draws [AccountContent]
 * without a VM.
 */
private class AccountStateProvider : PreviewParameterProvider<AccountState> {
    override val values =
        sequenceOf(
            AccountState.Account(
                AccountType.Pro,
                "CryptoBuddy",
                EmailState.UnconfirmedEmail("james.monroe@examplepetstore.comjames.monroe@examplepetstore.com"),
                DateType.Expiry("2323-01-20"),
            ),
            AccountState.Account(
                AccountType.Free("10 GB"),
                "CryptoBuddy",
                EmailState.NoEmail,
                DateType.Reset("2323-01-20"),
                dataLeft = "10GB",
            ),
            AccountState.Account(
                AccountType.Unlimited,
                "CryptoBuddy",
                EmailState.Email("james.monroe@examplepetstore.com"),
                DateType.Expiry("2323-01-20"),
            ),
            AccountState.Account(
                AccountType.AlcCustom("10 GB"),
                "CryptoBuddy",
                EmailState.NoEmail,
                DateType.Reset("2323-01-20"),
                dataLeft = "20GB",
            ),
        )
}

@Composable
@MultiDevicePreview
private fun AccountContentPreview(
    @PreviewParameter(AccountStateProvider::class) accountState: AccountState,
) {
    PreviewWithNav {
        AccountContent(
            showProgress = false,
            isGhostAccount = false,
            accountState = accountState,
            isSsoLogin = false,
            alertState = emptyFlow(),
            goTo = emptyFlow(),
            actions = AccountActions(),
        )
    }
}
