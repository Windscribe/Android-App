package com.windscribe.mobile.ui.preferences.excluded_ips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.vpn.localdatabase.ExcludedIpDomainDao
import com.windscribe.vpn.localdatabase.tables.ExcludedIpDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract class ExcludedIpDomainViewModel : ViewModel() {
    abstract val excludedList: StateFlow<List<ExcludedIpDomain>>
    abstract val inputText: StateFlow<String>
    abstract val errorMessage: StateFlow<String?>

    open fun onInputTextChange(text: String) {}

    open fun onAddEntry() {}

    open fun onDeleteEntry(entry: ExcludedIpDomain) {}
}

@HiltViewModel
class ExcludedIpDomainViewModelImpl
    @Inject
    constructor(
        private val excludedIpDomainDao: ExcludedIpDomainDao,
    ) : ExcludedIpDomainViewModel() {
        private val _excludedList = MutableStateFlow<List<ExcludedIpDomain>>(emptyList())
        override val excludedList: StateFlow<List<ExcludedIpDomain>> = _excludedList.asStateFlow()

        private val _inputText = MutableStateFlow("")
        override val inputText: StateFlow<String> = _inputText.asStateFlow()

        private val _errorMessage = MutableStateFlow<String?>(null)
        override val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

        init {
            loadExcludedList()
        }

        private fun loadExcludedList() {
            viewModelScope.launch(Dispatchers.IO) {
                excludedIpDomainDao.getAllFlow().collect { list ->
                    _excludedList.emit(list)
                }
            }
        }

        override fun onInputTextChange(text: String) {
            viewModelScope.launch {
                _inputText.emit(text)
                _errorMessage.emit(null)
            }
        }

        override fun onAddEntry() {
            viewModelScope.launch(Dispatchers.IO) {
                val value = _inputText.value.trim()
                if (value.isEmpty()) {
                    _errorMessage.emit("Entry cannot be empty")
                    return@launch
                }

                val type = detectEntryType(value)
                if (type == null) {
                    _errorMessage.emit("Invalid IP, IP range, or hostname")
                    return@launch
                }

                try {
                    val entry = ExcludedIpDomain(value = value, type = type)
                    excludedIpDomainDao.insert(entry)
                    _inputText.emit("")
                    _errorMessage.emit(null)
                } catch (e: Exception) {
                    _errorMessage.emit("Failed to add entry: ${e.message}")
                }
            }
        }

        override fun onDeleteEntry(entry: ExcludedIpDomain) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    excludedIpDomainDao.delete(entry)
                } catch (e: Exception) {
                    _errorMessage.emit("Failed to delete entry: ${e.message}")
                }
            }
        }

        private fun detectEntryType(value: String): ExcludedIpDomain.EntryType? =
            when {
                isValidIpAddress(value) -> ExcludedIpDomain.EntryType.IP
                isValidIpRange(value) -> ExcludedIpDomain.EntryType.IP_RANGE
                isValidHostname(value) -> ExcludedIpDomain.EntryType.HOSTNAME
                else -> null
            }

        private fun isValidIpAddress(value: String): Boolean {
            val parts = value.split(".")
            if (parts.size != 4) return false
            return parts.all { part ->
                part.toIntOrNull()?.let { it in 0..255 } ?: false
            }
        }

        private fun isValidIpRange(value: String): Boolean {
            if (!value.contains("/")) return false
            val parts = value.split("/")
            if (parts.size != 2) return false
            val ip = parts[0]
            val prefix = parts[1].toIntOrNull() ?: return false
            return isValidIpAddress(ip) && prefix in 0..32
        }

        private fun isValidHostname(value: String): Boolean {
            val hostnameRegex = "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$".toRegex()
            return hostnameRegex.matches(value)
        }
    }
