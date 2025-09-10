package com.windscribe.mobile.ui.preferences.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.connection.ToastMessage
import com.windscribe.mobile.ui.preferences.account.EmailState.Email
import com.windscribe.mobile.ui.preferences.account.EmailState.NoEmail
import com.windscribe.mobile.ui.preferences.account.EmailState.UnconfirmedEmail
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.ClaimVoucherCodeResponse
import com.windscribe.vpn.api.response.VerifyExpressLoginResponse
import com.windscribe.vpn.api.response.WebSession
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.constants.NetworkKeyConstants
import com.windscribe.vpn.constants.UserStatusConstants
import com.windscribe.vpn.model.User
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.workers.WindScribeWorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.Locale

sealed class AccountType {
    object Pro : AccountType()
    data class Free(val data: String) : AccountType()
    data class AlcCustom(val data: String) : AccountType()
    object Unlimited : AccountType()
}

sealed class DateType {
    open val date: String = ""

    data class Expiry(override val date: String) : DateType()
    data class Reset(override val date: String) : DateType()
}

sealed class AccountState {
    open val emailState: EmailState = NoEmail
    open val username: String = ""

    object Loading : AccountState()
    object Ghost : AccountState()
    data class Account(
        val type: AccountType,
        override val username: String,
        override val emailState: EmailState,
        val dateType: DateType,
        val dataLeft: String = ""
    ) : AccountState()
}

sealed class EmailState {
    object NoEmail : EmailState()
    data class UnconfirmedEmail(val email: String) : EmailState()
    data class Email(val email: String) : EmailState()
}

sealed class AccountGoTo {
    data class ManageAccount(val url: String) : AccountGoTo()
    data class Error(val message: String) : AccountGoTo()
    object None : AccountGoTo()
}

sealed class AlertState {
    object None : AlertState()
    data class Error(val message: ToastMessage) : AlertState()
    data class Success(val message: ToastMessage) : AlertState()
    object LazyLogin : AlertState()
    object VoucherCode : AlertState()
}

abstract class AccountViewModel : ViewModel() {
    abstract val showProgress: StateFlow<Boolean>
    abstract val accountState: StateFlow<AccountState>
    abstract val alertState: StateFlow<AlertState>
    abstract val isGhostAccount: StateFlow<Boolean>
    open val goTo: SharedFlow<AccountGoTo> = MutableSharedFlow(replay = 0)
    open fun onManageAccountClicked() {}
    open fun onLazyLoginClicked() {}
    open fun onVoucherCodeClicked() {}
    open fun onEnterLazyLoginCode(code: String) {}
    open fun onEnterVoucherCode(code: String) {}
    open fun onDialogDismiss() {}
}

class AccountViewModelImpl(
    val userRepository: UserRepository,
    val api: IApiCallManager,
    val workManager: WindScribeWorkManager
) :
    AccountViewModel() {
    private val _showProgress = MutableStateFlow(false)
    override val showProgress: StateFlow<Boolean> = _showProgress
    private val _accountState = MutableStateFlow<AccountState>(AccountState.Loading)
    override val accountState: StateFlow<AccountState> = _accountState
    private val _goTo: MutableSharedFlow<AccountGoTo> = MutableSharedFlow(replay = 0)
    override val goTo: SharedFlow<AccountGoTo> = _goTo
    private val logger = LoggerFactory.getLogger("basic")
    private val _isGhostAccount = MutableStateFlow(false)
    override val isGhostAccount: StateFlow<Boolean> = _isGhostAccount


    init {
        loadAccountInfo()
    }

    private fun loadAccountInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.userInfo.collect {
                _isGhostAccount.value = it.isGhost
                val emailState = when (it.emailStatus) {
                    User.EmailStatus.NoEmail -> NoEmail
                    User.EmailStatus.EmailProvided -> UnconfirmedEmail(it.email ?: "")
                    User.EmailStatus.Confirmed -> Email(it.email ?: "")
                }
                if (it.isGhost) {
                    it.maxData
                    _accountState.value = AccountState.Ghost
                } else if (it.isPro) {
                    _accountState.value = AccountState.Account(
                        AccountType.Pro,
                        it.userName,
                        emailState,
                        DateType.Expiry(it.expiryDate ?: "")
                    )
                } else if (it.isAlaCarteUnlimitedPlan) {
                    _accountState.value = AccountState.Account(
                        AccountType.Unlimited,
                        it.userName,
                        emailState,
                        DateType.Reset(it.resetDate ?: "")
                    )
                } else {
                    val dataLeft = maxOf(0, it.maxData - it.dataUsed)
                    val accountType = if (it.alcList != null) {
                        AccountType.AlcCustom("${it.maxData / UserStatusConstants.GB_DATA.toFloat()} GB")
                    } else {
                        AccountType.Free("${it.maxData / UserStatusConstants.GB_DATA.toFloat()} GB")
                    }
                    _accountState.value = AccountState.Account(
                        accountType,
                        it.userName,
                        emailState,
                        DateType.Reset(it.resetDate ?: ""),
                        String.format(
                            Locale.getDefault(),
                            "%.2f GB",
                            dataLeft.toDouble() / (1024 * 1024 * 1024)
                        )
                    )
                }
            }
        }
    }

    override fun onManageAccountClicked() {
        viewModelScope.launch(Dispatchers.IO) {
            _showProgress.value = true
            val result = api.getWebSession().result<WebSession>()
            when (result) {
                is CallResult.Error -> {
                    _showProgress.value = false
                    _goTo.emit(AccountGoTo.Error(result.errorMessage))
                }

                is CallResult.Success<WebSession> -> {
                    _showProgress.value = false
                    _goTo.emit(AccountGoTo.ManageAccount("${NetworkKeyConstants.URL_MY_ACCOUNT}${result.data.tempSession}"))
                }
            }
        }
    }

    override fun onLazyLoginClicked() {
        viewModelScope.launch {
            _alertState.emit(AlertState.LazyLogin)
        }
    }

    private val successMessage = "Sweet, you should be all good to go now."

    override fun onEnterLazyLoginCode(code: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _showProgress.value = true
            val result = api.verifyExpressLoginCode(code).result<VerifyExpressLoginResponse>()
            delay(3000)
            _showProgress.value = false
            when (result) {
                is CallResult.Error -> {
                    logger.error("Error verifying express login: {}", result.errorMessage)
                    _alertState.emit(AlertState.Error(ToastMessage.Raw(result.errorMessage)))
                }

                is CallResult.Success<VerifyExpressLoginResponse> -> {
                    logger.debug("Verified express login: {}", result.data)
                    _alertState.emit(AlertState.Success(ToastMessage.Raw(successMessage)))
                }
            }
        }
    }

    private val _alertState = MutableStateFlow<AlertState>(AlertState.None)
    override val alertState: StateFlow<AlertState> = _alertState
    override fun onVoucherCodeClicked() {
        viewModelScope.launch {
            _alertState.emit(AlertState.VoucherCode)
        }
    }

    override fun onEnterVoucherCode(code: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _showProgress.value = true
            val result = api.claimVoucherCode(code).result<ClaimVoucherCodeResponse>()
            delay(3000)
            _showProgress.value = false
            when (result) {
                is CallResult.Error -> {
                    _alertState.emit(AlertState.Error(ToastMessage.Raw(result.errorMessage)))
                }

                is CallResult.Success<ClaimVoucherCodeResponse> -> {
                    logger.debug("Claimed voucher code: {}", result.data)
                    if (result.data.isClaimed) {
                        _alertState.emit(AlertState.Success(ToastMessage.Localized(com.windscribe.vpn.R.string.voucher_code_is_applied)))
                        workManager.updateSession()
                    } else if (result.data.emailRequired == true) {
                        _alertState.emit(AlertState.Error(ToastMessage.Localized(com.windscribe.vpn.R.string.confirmed_email_required)))
                    } else if (result.data.isUsed) {
                        _alertState.emit(AlertState.Error(ToastMessage.Localized(com.windscribe.vpn.R.string.voucher_code_used_already)))
                    } else {
                        _alertState.emit(AlertState.Error(ToastMessage.Localized(com.windscribe.vpn.R.string.voucher_code_is_invalid)))
                    }
                }
            }
        }
    }

    override fun onDialogDismiss() {
        viewModelScope.launch {
            _alertState.emit(AlertState.None)
        }
    }
}