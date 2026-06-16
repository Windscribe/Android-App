package com.windscribe.mobile.ui.preferences.ticket

import android.net.Uri
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.vpn.BuildConfig
import com.windscribe.vpn.api.response.QueryType
import com.windscribe.vpn.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import javax.inject.Inject

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

    abstract fun onGarryOpened()
}

sealed class SubmitTicketState {
    object Idle : SubmitTicketState()

    object Loading : SubmitTicketState()

    data class OpenGarry(
        val url: String,
    ) : SubmitTicketState()

    data class Error(
        val message: String,
    ) : SubmitTicketState()
}

@HiltViewModel
class TicketViewModelImpl
    @Inject
    constructor(
        val userRepository: UserRepository,
    ) : TicketViewModel() {
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
        private val logger = LoggerFactory.getLogger("basic")

        init {
            viewModelScope.launch {
                userRepository.user.filterNotNull().collect {
                    logger.info("User info: ${it.email}")
                    _email.value = it.email ?: ""
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
                _submitTicketState.emit(SubmitTicketState.OpenGarry(getKnowledgeBaseGarryUrl()))
            }
        }

        override fun onGarryOpened() {
            viewModelScope.launch {
                _submitTicketState.emit(SubmitTicketState.Idle)
            }
        }

        private fun getKnowledgeBaseGarryUrl(): String {
            val host = if (BuildConfig.DEV) "www-staging.windscribe.com" else "windscribe.com"
            return Uri
                .Builder()
                .scheme("https")
                .authority(host)
                .path("knowledge-base")
                .appendQueryParameter("yo_garry", message.value)
                .build()
                .toString()
        }

        private fun validEmail(email: String): Boolean = email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

        private fun validMessage(message: String): Boolean = message.isNotEmpty()

        private fun validSubject(subject: String): Boolean = subject.isNotEmpty()
    }
