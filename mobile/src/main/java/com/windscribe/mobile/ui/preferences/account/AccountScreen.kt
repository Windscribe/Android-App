package com.windscribe.mobile.ui.preferences.account

import PreferencesNavBar
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.windscribe.mobile.ui.AppStartActivity
import com.windscribe.mobile.ui.common.AppProgressBar
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.common.openUrl
import com.windscribe.mobile.ui.connection.ToastMessage
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font14
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.preferencesBackgroundColor
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.mobile.upgradeactivity.UpgradeActivity
import com.windscribe.vpn.R


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
            ManageAccount(viewModel)
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
        AppProgressBar(showProgressBar = showProgress, message = "")
        HandleGoto(viewModel)
        HandleAlertState(viewModel)
    }
}

@Composable
private fun HandleAlertState(viewModel: AccountViewModel?) {
    val activity = LocalContext.current as AppStartActivity
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
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
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
                        onClick = onDismiss,
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
    val accountState by viewModel?.accountState?.collectAsState() ?: remember {
        mutableStateOf(AccountState.Loading)
    }
    if (accountState !is AccountState.Account) {
        return
    }
    val username = (accountState as AccountState.Account).username
    Column {
        Text(
            stringResource(R.string.info),
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
                stringResource(R.string.email),
                style = font16.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primaryTextColor
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            if (accountState.emailState is EmailState.UnconfirmedEmail) {
                Text(
                    (accountState.emailState as EmailState.UnconfirmedEmail).email,
                    style = font16.copy(color = MaterialTheme.colorScheme.preferencesSubtitleColor)
                )
            }
            if (accountState.emailState is EmailState.Email) {
                Text(
                    (accountState.emailState as EmailState.Email).email,
                    style = font16.copy(color = MaterialTheme.colorScheme.preferencesSubtitleColor)
                )
            }
            if (accountState.emailState is EmailState.NoEmail) {
                Text(
                    stringResource(R.string.add_email),
                    style = font16.copy(color = AppColors.cyberBlue)
                )
            }
        }
    }
}

@Composable
private fun PlanInfo(viewModel: AccountViewModel? = null) {
    val activity = LocalContext.current as AppStartActivity
    val accountState by viewModel?.accountState?.collectAsState() ?: remember {
        mutableStateOf(AccountState.Loading)
    }
    if (accountState !is AccountState.Account) {
        return
    }
    val type = (accountState as AccountState.Account).type
    val plan = when (type) {
        is AccountType.Pro, is AccountType.Unlimited -> stringResource(R.string.unlimited_data)
        is AccountType.Free -> type.data
    }
    val planAction = when (type) {
        is AccountType.Pro -> stringResource(R.string.pro)
        is AccountType.Unlimited -> stringResource(R.string.a_la_carte_unlimited_plan)
        is AccountType.Free -> stringResource(R.string.upgrade)
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
            Text(modifier = Modifier.clickable {
                if (type is AccountType.Free) {
                    activity.startActivity(UpgradeActivity.getStartIntent(activity))
                    return@clickable
                }
            }, text = planAction, style = font16.copy(color = AppColors.cyberBlue))
        }
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
    }
}

@Composable
private fun ManageAccount(viewModel: AccountViewModel?) {
    Row(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                shape = RoundedCornerShape(size = 12.dp)
            )
            .padding(vertical = 14.dp, horizontal = 14.dp)
            .clickable {
                viewModel?.onManageAccountClicked()
            }
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            stringResource(R.string.edit_account),
            style = font16.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primaryTextColor
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
            .padding(vertical = 14.dp, horizontal = 14.dp)
            .clickable {
                viewModel?.onVoucherCodeClicked()
            }
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
            .padding(vertical = 14.dp, horizontal = 14.dp)
            .clickable {
                viewModel?.onLazyLoginClicked()
            }
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
    val activity = LocalContext.current as AppStartActivity
    val goto by viewModel?.goTo?.collectAsState(initial = AccountGoTo.None) ?: remember {
        mutableStateOf(
            AccountGoTo.None
        )
    }
    LaunchedEffect(goto) {
        when (goto) {
            is AccountGoTo.ManageAccount -> activity.openUrl((goto as AccountGoTo.ManageAccount).url)
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
private fun EnterLazyLoginCode() {

}