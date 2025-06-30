package com.windscribe.mobile.ui.preferences.account

import PreferencesNavBar
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.windscribe.mobile.ui.AppStartActivity
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.common.PreferenceProgressBar
import com.windscribe.mobile.ui.common.openUrl
import com.windscribe.mobile.ui.connection.ToastMessage
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.helper.hapticClickable
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font14
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.preferencesBackgroundColor
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.mobile.upgradeactivity.UpgradeActivity
import com.windscribe.vpn.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


@Composable
fun AccountScreen(viewModel: AccountViewModel? = null) {
    val navController = LocalNavController.current
    val showProgress by viewModel?.showProgress?.collectAsState()
        ?: remember { mutableStateOf(false) }
    PreferenceBackground {
        Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)) {
            PreferencesNavBar(stringResource(R.string.my_account)) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.height(20.dp))
            AccountInfo(viewModel)
            Spacer(modifier = Modifier.height(14.dp))
            PlanInfo(viewModel)
            Spacer(modifier = Modifier.height(14.dp))
            ActionButton(stringResource(R.string.edit_account)) {
                viewModel?.onManageAccountClicked()
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                stringResource(R.string.other),
                style = font12.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.preferencesSubtitleColor
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            VoucherCode(viewModel)
            Spacer(modifier = Modifier.height(14.dp))
            LazyLogin(viewModel)
        }
        PreferenceProgressBar(showProgressBar = showProgress)
        HandleGoto(viewModel)
        HandleAlertState(viewModel)
    }
}

@Composable
private fun HandleAlertState(viewModel: AccountViewModel?) {
    val activity = LocalContext.current as? AppStartActivity
    val alertState by viewModel?.alertState?.collectAsState() ?: remember {
        mutableStateOf(AlertState.None)
    }
    var showVoucherDialog by remember { mutableStateOf(false) }
    var showLazyLoginDialog by remember { mutableStateOf(false) }
    LaunchedEffect(alertState) {
        when (alertState) {
            is AlertState.Error -> {
                val result = (alertState as AlertState.Error).message
                if (result is ToastMessage.Raw) {
                    Toast.makeText(activity, result.message, Toast.LENGTH_SHORT).show()
                }
                if (result is ToastMessage.Localized) {
                    Toast.makeText(activity, result.message, Toast.LENGTH_SHORT).show()
                }
            }

            is AlertState.Success -> {
                val result = (alertState as AlertState.Success).message
                if (result is ToastMessage.Raw) {
                    Toast.makeText(activity, result.message, Toast.LENGTH_SHORT).show()
                }
                if (result is ToastMessage.Localized) {
                    Toast.makeText(activity, result.message, Toast.LENGTH_SHORT).show()
                }
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
    if (showVoucherDialog) {
        TextFieldDialog(onDismiss = {
            showVoucherDialog = false
            viewModel?.onDialogDismiss()
        }, onSubmit = {
            viewModel?.onEnterVoucherCode(it)
        })
    }

    if (showLazyLoginDialog) {
        TextFieldDialog(onDismiss = {
            showLazyLoginDialog = false
            viewModel?.onDialogDismiss()
        }, onSubmit = {
            viewModel?.onEnterLazyLoginCode(it)
        })
    }
}

@Composable
private fun TextFieldDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    val text = remember { mutableStateOf("") }
    val activity = LocalContext.current as? AppStartActivity
    val hapticFeedbackEnabled by activity?.viewmodel?.hapticFeedback?.collectAsState() ?: remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryTextColor,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextField(
                    value = text.value,
                    onValueChange = {
                        text.value = it
                    },
                    colors = TextFieldDefaults.colors().copy(
                        focusedContainerColor = MaterialTheme.colorScheme.primaryTextColor,
                        unfocusedContainerColor = MaterialTheme.colorScheme.primaryTextColor,
                        disabledContainerColor = MaterialTheme.colorScheme.primaryTextColor,
                        focusedTextColor = MaterialTheme.colorScheme.preferencesBackgroundColor,
                        unfocusedTextColor = MaterialTheme.colorScheme.preferencesBackgroundColor,
                        disabledTextColor = MaterialTheme.colorScheme.preferencesBackgroundColor
                    ),
                    placeholder = {
                        Text(
                            stringResource(R.string.enter_code),
                            style = font16.copy(
                                color = MaterialTheme.colorScheme.preferencesBackgroundColor,
                                textAlign = TextAlign.Start
                            )
                        )
                    },
                    textStyle = font16.copy(
                        color = MaterialTheme.colorScheme.preferencesBackgroundColor,
                        textAlign = TextAlign.Start
                    ),
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
                        colors = ButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.preferencesBackgroundColor.copy(
                                alpha = 0.70f
                            ),
                            disabledContainerColor = Color.Transparent,
                            disabledContentColor = Color.Transparent
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            stringResource(R.string.cancel),
                            style = font16,
                            color = MaterialTheme.colorScheme.preferencesBackgroundColor,
                            textAlign = TextAlign.Start
                        )
                    }
                    Button(
                        onClick = {
                            if (hapticFeedbackEnabled) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                            }
                            onDismiss()
                            onSubmit(text.value)
                        },
                        colors = ButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.preferencesBackgroundColor,
                            disabledContainerColor = Color.Transparent,
                            disabledContentColor = Color.Transparent
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            stringResource(R.string.text_ok),
                            style = font16,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountInfo(viewModel: AccountViewModel? = null) {
    val accountState by viewModel?.accountState?.collectAsState()
        ?: remember { mutableStateOf(AccountState.Loading) }

    val account = accountState as? AccountState.Account ?: return

    val username = account.username
    val emailState = account.emailState
    val navController = LocalNavController.current

    Column {
        Text(
            stringResource(R.string.info),
            style = font12.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.preferencesSubtitleColor
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Username Row
        Row(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )
                .padding(14.dp)
        ) {
            Text(
                stringResource(R.string.username),
                style = font16.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primaryTextColor
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                username,
                style = font16.copy(color = MaterialTheme.colorScheme.preferencesSubtitleColor)
            )
        }

        Spacer(modifier = Modifier.height(1.dp))

        // Email Section
        Column(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                )
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (emailState) {
                    is EmailState.NoEmail -> {
                        Icon(
                            painterResource(com.windscribe.mobile.R.drawable.ic_email_attention),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primaryTextColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    is EmailState.UnconfirmedEmail -> {
                        Icon(
                            painterResource(com.windscribe.mobile.R.drawable.ic_email_attention),
                            contentDescription = null,
                            tint = AppColors.yellow
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    else -> Unit
                }

                Text(
                    stringResource(R.string.email),
                    style = font16.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primaryTextColor
                    )
                )

                Spacer(modifier = Modifier.width(16.dp))

                ValueItem(
                    when (emailState) {
                        is EmailState.Email -> emailState.email
                        is EmailState.UnconfirmedEmail -> emailState.email
                        is EmailState.NoEmail -> stringResource(R.string.none)
                    }
                )
            }

            when (emailState) {
                is EmailState.NoEmail -> {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        stringResource(R.string.get_10gb_data),
                        style = font14.copy(
                            color = MaterialTheme.colorScheme.preferencesSubtitleColor,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Start
                        ),
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .fillMaxWidth()
                    )
                }

                is EmailState.UnconfirmedEmail -> {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .background(AppColors.yellow, shape = RoundedCornerShape(6.dp))
                            .clickable {
                                navController.navigate(Screen.ConfirmEmail.route)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.confirm_your_email),
                            style = font14.copy(
                                color = Color.Black,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Start
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            stringResource(R.string.resend),
                            style = font16.copy(
                                color = Color.Black,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                        )
                    }
                }

                else -> Unit
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

@Composable
private fun RowScope.ValueItem(value: String) {
    Text(
        value,
        style = font16.copy(
            color = MaterialTheme.colorScheme.preferencesSubtitleColor,
            textAlign = TextAlign.End
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
    )
}

@Composable
private fun PlanInfo(viewModel: AccountViewModel? = null) {
    val accountState by viewModel?.accountState?.collectAsState() ?: remember {
        mutableStateOf(AccountState.Loading)
    }
    val activity = LocalContext.current as? AppStartActivity
    if (accountState !is AccountState.Account) {
        return
    }
    val type = (accountState as AccountState.Account).type
    val plan = when (type) {
        is AccountType.Pro, is AccountType.Unlimited -> stringResource(R.string.unlimited_data)
        is AccountType.Free -> type.data
    }
    val planType = when (type) {
        is AccountType.Pro -> stringResource(R.string.pro)
        is AccountType.Unlimited -> stringResource(R.string.a_la_carte_unlimited_plan)
        is AccountType.Free -> stringResource(R.string.free)
    }
    val resetDateBackground = when (type) {
        is AccountType.Pro, is AccountType.Unlimited -> RoundedCornerShape(
            bottomStart = 12.dp,
            bottomEnd = 12.dp
        )

        is AccountType.Free -> RoundedCornerShape(0.dp)
    }
    val dateType = (accountState as AccountState.Account).dateType
    Column {
        Text(
            stringResource(R.string.plan),
            style = font12.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.preferencesSubtitleColor
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primaryTextColor.copy(
                        alpha = 0.05f
                    ), shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )
                .padding(vertical = 14.dp, horizontal = 14.dp)
        ) {
            Text(
                plan,
                style = font16.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primaryTextColor
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                planType,
                style = font16.copy(color = if (type is AccountType.Free) MaterialTheme.colorScheme.primaryTextColor else AppColors.cyberBlue)
            )
        }
        Spacer(modifier = Modifier.height(1.dp))
        Row(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primaryTextColor.copy(
                        alpha = 0.05f
                    ), shape = resetDateBackground
                )
                .padding(vertical = 14.dp, horizontal = 14.dp)
        ) {
            Text(
                if (dateType is DateType.Expiry) stringResource(R.string.expiry_date) else stringResource(
                    R.string.reset_date
                ),
                style = font16.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primaryTextColor
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                dateType.date,
                style = font16.copy(color = MaterialTheme.colorScheme.preferencesSubtitleColor)
            )
        }
        if (type is AccountType.Free) {
            Spacer(modifier = Modifier.height(1.dp))
            Row(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primaryTextColor.copy(
                            alpha = 0.05f
                        ), shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                    )
                    .padding(vertical = 14.dp, horizontal = 14.dp)
            ) {
                Text(
                    stringResource(R.string.data_left_in_your_plan),
                    style = font16.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primaryTextColor
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    (accountState as AccountState.Account).dataLeft,
                    style = font16.copy(color = MaterialTheme.colorScheme.preferencesSubtitleColor)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            ActionButton(
                stringResource(R.string.upgrade_to_pro),
                textColor = AppColors.cyberBlue,
                backgroundColor = AppColors.cyberBlue.copy(alpha = 0.05f)
            ) {
                activity?.startActivity(UpgradeActivity.getStartIntent(activity))
            }
        }
    }
}


@Composable
private fun ActionButton(
    title: String,
    textColor: Color = MaterialTheme.colorScheme.primaryTextColor,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryTextColor.copy(
        alpha = 0.05f
    ),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(size = 12.dp)
            )
            .hapticClickable {
                onClick()
            }
            .padding(vertical = 14.dp, horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            title,
            style = font16.copy(
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun VoucherCode(viewModel: AccountViewModel?) {
    Column(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primaryTextColor.copy(
                    alpha = 0.05f
                ), shape = RoundedCornerShape(size = 12.dp)
            )
            .hapticClickable {
                viewModel?.onVoucherCodeClicked()
            }
            .padding(vertical = 14.dp, horizontal = 14.dp)
    ) {
        Row() {
            Text(
                stringResource(R.string.voucher_code),
                style = font16.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primaryTextColor
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(com.windscribe.mobile.R.drawable.arrow_right),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primaryTextColor
            )
        }
        Spacer(modifier = Modifier.height(13.5.dp))
        Text(
            text = stringResource(R.string.apply_voucher_code),
            style = font14.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.preferencesSubtitleColor,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun LazyLogin(viewModel: AccountViewModel?) {
    Column(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primaryTextColor.copy(
                    alpha = 0.05f
                ), shape = RoundedCornerShape(size = 12.dp)
            )
            .hapticClickable {
                viewModel?.onLazyLoginClicked()
            }
            .padding(vertical = 14.dp, horizontal = 14.dp)
    ) {
        Row {
            Text(
                stringResource(R.string.xpress_login),
                style = font16.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primaryTextColor
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(com.windscribe.mobile.R.drawable.arrow_right),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primaryTextColor
            )
        }
        Spacer(modifier = Modifier.height(13.5.dp))
        Text(
            text = stringResource(R.string.lazy_login_description),
            style = font14.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.preferencesSubtitleColor,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun HandleGoto(viewModel: AccountViewModel?) {
    val activity = LocalContext.current as? AppStartActivity
    val goto by viewModel?.goTo?.collectAsState(initial = AccountGoTo.None) ?: remember {
        mutableStateOf(
            AccountGoTo.None
        )
    }
    LaunchedEffect(goto) {
        when (goto) {
            is AccountGoTo.ManageAccount -> activity?.openUrl((goto as AccountGoTo.ManageAccount).url)
            is AccountGoTo.Error -> Toast.makeText(
                activity,
                (goto as AccountGoTo.Error).message,
                Toast.LENGTH_SHORT
            ).show()

            else -> {}
        }
    }
}

@Composable
private fun AccountScreenPreview(accountState: AccountState) {
    PreviewWithNav {
        val viewModel = object : AccountViewModel() {
            override val showProgress: StateFlow<Boolean> = MutableStateFlow(false)
            override val accountState: StateFlow<AccountState> = MutableStateFlow(accountState)
            override val alertState: StateFlow<AlertState> = MutableStateFlow(AlertState.None)
        }
        AccountScreen(viewModel)
    }
}

@Composable
@Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun AccountScreenFreePreview() {
    AccountScreenPreview(
        AccountState.Account(
            AccountType.Free("10 GB"),
            "CryptoBuddy",
            EmailState.NoEmail,
            DateType.Reset("2323-01-20")
        )
    )
}

@Composable
@Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun AccountScreenProPreview() {
    AccountScreenPreview(
        AccountState.Account(
            AccountType.Pro,
            "CryptoBuddy",
            EmailState.UnconfirmedEmail("james.monroe@examplepetstore.comjames.monroe@examplepetstore.com"),
            DateType.Expiry("2323-01-20")
        )
    )
}

@Composable
@Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun AccountScreenUnconfirmedPreview() {
    AccountScreenPreview(
        AccountState.Account(
            AccountType.Unlimited,
            "CryptoBuddy",
            EmailState.Email("james.monroe@examplepetstore.com"),
            DateType.Expiry("2323-01-20")
        )
    )
}