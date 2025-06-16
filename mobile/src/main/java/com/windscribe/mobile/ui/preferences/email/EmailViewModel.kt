package com.windscribe.mobile.ui.preferences.email

import android.util.Patterns
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.connection.ToastMessage
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.AddEmailResponse
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.workers.WindScribeWorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class EmailViewModel : ViewModel() {
    abstract val showProgress: StateFlow<Boolean>
    abstract val error: StateFlow<ToastMessage?>
    abstract var email: String
    abstract val exit: StateFlow<Boolean>
    abstract fun addEmail()
    abstract fun resendConfirmation()
    abstract fun onEmailChanged(email: String)
    abstract var emailAdded: Boolean
    abstract val pro: StateFlow<Boolean>
}

class EmailViewModelImpl(
    val api: IApiCallManager,
    val userRepository: UserRepository,
    val workManager: WindScribeWorkManager
) : EmailViewModel() {
    private val _showProgress = MutableStateFlow(false)
    override val showProgress: StateFlow<Boolean> = _showProgress
    private val _error = MutableStateFlow<ToastMessage?>(null)
    override val error: StateFlow<ToastMessage?> = _error
    override var email: String = ""
    private val _exit = MutableStateFlow(false)
    override val exit: StateFlow<Boolean> = _exit
    override var emailAdded: Boolean = false
    private val _pro = MutableStateFlow(false)
    override val pro: StateFlow<Boolean> = _pro


    init {
        viewModelScope.launch(Dispatchers.IO) {
            _pro.emit((userRepository.user.value?.isPro == true) or (userRepository.user.value?.isAlaCarteUnlimitedPlan == true))
        }
    }

    override fun addEmail() {
        viewModelScope.launch(Dispatchers.IO) {
            if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                _error.emit(ToastMessage.Localized(R.string.invalid_email_format))
                return@launch
            }
            _error.emit(null)
            _showProgress.value = true
            val result = api.addUserEmailAddress(email).result<AddEmailResponse>()
            _showProgress.value = false
            when (result) {
                is CallResult.Error -> {
                    _error.emit(ToastMessage.Raw(result.errorMessage))
                }

                is CallResult.Success<AddEmailResponse> -> {
                    workManager.updateSession()
                    _exit.emit(true)
                }
            }
        }
    }

    override fun onEmailChanged(email: String) {
        viewModelScope.launch {
            _error.emit(null)
            this@EmailViewModelImpl.email = email
        }
    }

    override fun resendConfirmation() {
        viewModelScope.launch(Dispatchers.IO) {
            _error.emit(null)
            _showProgress.value = true
            val result = api.resendUserEmailAddress().result<AddEmailResponse>()
            _showProgress.value = false
            when (result) {
                is CallResult.Error -> {
                    _error.emit(ToastMessage.Raw(result.errorMessage))
                }

                is CallResult.Success<AddEmailResponse> -> {
                    workManager.updateSession()
                    _exit.emit(true)
                }
            }
        }
    }
}