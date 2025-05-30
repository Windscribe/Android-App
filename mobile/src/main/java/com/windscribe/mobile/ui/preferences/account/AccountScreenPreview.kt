package com.windscribe.mobile.ui.preferences.account

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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
    AccountScreenPreview(AccountState.Account(AccountType.Free("10 GB"), "CryptoBuddy", EmailState.NoEmail, DateType.Reset("2323-01-20")))
}

@Composable
@Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun AccountScreenProPreview() {
    AccountScreenPreview(AccountState.Account(AccountType.Pro, "CryptoBuddy", EmailState.NoEmail, DateType.Expiry("2323-01-20")))
}

@Composable
@Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun AccountScreenUnconfirmedPreview() {
    AccountScreenPreview(AccountState.Account(AccountType.Unlimited, "CryptoBuddy", EmailState.NoEmail, DateType.Expiry("2323-01-20")))
}