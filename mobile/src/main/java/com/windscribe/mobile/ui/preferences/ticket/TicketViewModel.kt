package com.windscribe.mobile.ui.preferences.ticket

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.QueryType
import com.windscribe.vpn.api.response.TicketResponse
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.constants.NetworkErrorCodes
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

abstract class TicketViewModel : ViewModel() {
    abstract val buttonEnabled: StateFlow<Boolean>
    abstract val submitTicketState: StateFlow<SubmitTicketState>
    abstract val email: MutableStateFlow<String>
    abstract val subject: MutableStateFlow<String>
    abstract val message: MutableStateFlow<String>
    abstract val queryType: MutableStateFlow<QueryType>
    abstract fun onQueryTypeSelected(queryType: QueryType)
    abstract fun onEmailChanged(email: String)
    abstract fun onSubjectChanged(subject: String)
    abstract fun onMessageChanged(message: String)
    abstract fun onSendTicketClicked()
}

sealed class SubmitTicketState {
    object Idle : SubmitTicketState()
    object Loading : SubmitTicketState()
    data class Success(val message: String) : SubmitTicketState()
    data class Error(val message: String) : SubmitTicketState()
}

class TicketViewModelImpl(val userRepository: UserRepository, val api: IApiCallManager) :
    TicketViewModel() {
    private val _buttonEnabled = MutableStateFlow(false)
    override val buttonEnabled: StateFlow<Boolean> = _buttonEnabled
    private val _submitTicketState = MutableStateFlow<SubmitTicketState>(SubmitTicketState.Idle)
    override val submitTicketState: StateFlow<SubmitTicketState> = _submitTicketState
    private val _email = MutableStateFlow("")
    override val email: MutableStateFlow<String> = _email
    private val _subject = MutableStateFlow("")
    override val subject: MutableStateFlow<String> = _subject
    private val _message = MutableStateFlow("")
    override val message: MutableStateFlow<String> = _message
    private val _queryType = MutableStateFlow(QueryType.Account)
    override val queryType: MutableStateFlow<QueryType> = _queryType
    private var username = ""
    private val logger = LoggerFactory.getLogger("basic")


    init {
        viewModelScope.launch {
            userRepository.userInfo.collect {
                logger.info("User info: ${it.email}")
                _email.value = it.email ?: ""
                username = it.userName
                validateInput()
            }
        }
    }

    private fun validateInput() {
        _buttonEnabled.value = validEmail(email.value) &&
                validMessage(message.value) &&
                validSubject(subject.value)
    }

    override fun onQueryTypeSelected(queryType: QueryType) {
        this@TicketViewModelImpl.queryType.value = queryType
        validateInput()
    }

    override fun onEmailChanged(email: String) {
        this@TicketViewModelImpl.email.value = email
        validateInput()
    }

    override fun onSubjectChanged(subject: String) {
        this@TicketViewModelImpl.subject.value = subject
        validateInput()
    }

    override fun onMessageChanged(message: String) {
        this@TicketViewModelImpl.message.value = message
        validateInput()
    }

    override fun onSendTicketClicked() {
        viewModelScope.launch {
            _submitTicketState.emit(SubmitTicketState.Loading)
            val result = api.sendTicket(
                email.value,
                username,
                subject.value,
                message.value,
                queryType.value.toString(),
                queryType.value.name,
                "app_android"
            ).result<TicketResponse>()
            when (result) {
                is CallResult.Error -> {
                    if (result.code == NetworkErrorCodes.ERROR_UNEXPECTED_API_DATA || result.code == NetworkErrorCodes.ERROR_UNABLE_TO_REACH_API) {
                        _submitTicketState.emit(SubmitTicketState.Error("Failed to submit ticket. Check your network & try again."))
                    } else {
                        _submitTicketState.emit(SubmitTicketState.Error(result.errorMessage))
                    }
                }

                is CallResult.Success -> {
                    _submitTicketState.emit(SubmitTicketState.Success("Sweet, we’ll get back to you as soon as one of our agents is back from lunch."))
                }
            }
        }
    }

    private fun validEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun validMessage(message: String): Boolean {
        return message.isNotEmpty()
    }

    private fun validSubject(subject: String): Boolean {
        return subject.isNotEmpty()
    }
}