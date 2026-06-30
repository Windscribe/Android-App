package com.windscribe.mobile.ui.preferences.excluded_ips

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.vpn.backend.utils.ExcludedIpHolder
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.tables.ExcludedIpDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    abstract val toastMessage: StateFlow<String?>
    abstract val isRefreshing: StateFlow<Boolean>

    open fun onInputTextChange(text: String) {}

    open fun onAddEntry() {}

    open fun onDeleteEntry(entry: ExcludedIpDomain) {}

    open fun onDeleteAll() {}

    open fun onImportFromFile(uri: Uri) {}

    open fun onRefreshHostnames() {}

    open fun clearToastMessage() {}
}

@HiltViewModel
class ExcludedIpDomainViewModelImpl
    @Inject
    constructor(
        private val localDbInterface: LocalDbInterface,
        private val excludedIpHolder: ExcludedIpHolder,
        @ApplicationContext private val context: Context,
    ) : ExcludedIpDomainViewModel() {
        private val _excludedList = MutableStateFlow<List<ExcludedIpDomain>>(emptyList())
        override val excludedList: StateFlow<List<ExcludedIpDomain>> = _excludedList.asStateFlow()

        private val _inputText = MutableStateFlow("")
        override val inputText: StateFlow<String> = _inputText.asStateFlow()

        private val _errorMessage = MutableStateFlow<String?>(null)
        override val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

        private val _toastMessage = MutableStateFlow<String?>(null)
        override val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

        private val _isRefreshing = MutableStateFlow(false)
        override val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

        init {
            loadExcludedList()
        }

        private fun loadExcludedList() {
            viewModelScope.launch(Dispatchers.IO) {
                localDbInterface.getExcludedIpsDomainsFlow().collect { list ->
                    _excludedList.emit(list)
                }
            }
        }

        override fun onInputTextChange(text: String) {
            viewModelScope.launch {
                _inputText.emit(text)
            }
        }

        override fun onAddEntry() {
            viewModelScope.launch(Dispatchers.IO) {
                val value = _inputText.value.trim().lowercase()
                if (value.isEmpty()) {
                    _toastMessage.emit("Entry cannot be empty")
                    return@launch
                }

                // Check for duplicates
                if (localDbInterface.excludedIpDomainExists(value) > 0) {
                    _toastMessage.emit("Entry already exists")
                    return@launch
                }

                val type = detectEntryType(value)
                if (type == null) {
                    _toastMessage.emit("Invalid IP, IP range, or hostname")
                    return@launch
                }

                try {
                    val entry = ExcludedIpDomain(value = value, type = type)
                    val insertedId = localDbInterface.insertExcludedIpDomain(entry)
                    _inputText.emit("")
                    // Only resolve this new entry if it's a hostname
                    val insertedEntry = entry.copy(id = insertedId)
                    excludedIpHolder.resolveNewEntry(insertedEntry)
                } catch (e: Exception) {
                    _toastMessage.emit("Failed to add entry: ${e.message}")
                }
            }
        }

        override fun onDeleteEntry(entry: ExcludedIpDomain) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    localDbInterface.deleteExcludedIpDomain(entry)
                    // Just reload cache - no need to resolve anything
                    excludedIpHolder.loadCachedIps()
                } catch (e: Exception) {
                    _errorMessage.emit("Failed to delete entry: ${e.message}")
                }
            }
        }

        override fun onDeleteAll() {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    localDbInterface.deleteAllExcludedIpsDomains()
                    // Just reload cache (which will be empty) - no need to resolve anything
                    excludedIpHolder.loadCachedIps()
                } catch (e: Exception) {
                    _errorMessage.emit("Failed to delete all entries: ${e.message}")
                }
            }
        }

        override fun onImportFromFile(uri: Uri) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream == null) {
                        _toastMessage.emit("Failed to open file")
                        return@launch
                    }

                    val lines = inputStream.bufferedReader().use { it.readLines() }
                    var successCount = 0
                    var duplicateCount = 0
                    var failCount = 0
                    val newHostnames = mutableListOf<ExcludedIpDomain>()

                    lines.forEach { line ->
                        val value = line.trim().lowercase()
                        if (value.isEmpty() || value.startsWith("#")) {
                            // Skip empty lines and comments
                            return@forEach
                        }

                        // Check for duplicates
                        if (localDbInterface.excludedIpDomainExists(value) > 0) {
                            duplicateCount++
                            return@forEach
                        }

                        val type = detectEntryType(value)
                        if (type != null) {
                            try {
                                val entry = ExcludedIpDomain(value = value, type = type)
                                val insertedId = localDbInterface.insertExcludedIpDomain(entry)
                                if (insertedId > 0) {
                                    successCount++
                                    // Collect hostnames for resolution
                                    if (type == ExcludedIpDomain.EntryType.HOSTNAME) {
                                        newHostnames.add(entry.copy(id = insertedId))
                                    }
                                } else {
                                    duplicateCount++
                                }
                            } catch (e: Exception) {
                                failCount++
                            }
                        } else {
                            failCount++
                        }
                    }

                    // Resolve only new hostnames after import (batch operation)
                    if (newHostnames.isNotEmpty()) {
                        excludedIpHolder.resolveNewEntries(newHostnames)
                    } else {
                        // No hostnames to resolve, just reload cache
                        excludedIpHolder.loadCachedIps()
                    }

                    val result =
                        buildString {
                            append("Imported: $successCount")
                            if (duplicateCount > 0) {
                                append(", Skipped: $duplicateCount")
                            }
                            if (failCount > 0) {
                                append(", Failed: $failCount")
                            }
                        }
                    _toastMessage.emit(result)
                } catch (e: Exception) {
                    _toastMessage.emit("Failed to import file: ${e.message}")
                }
            }
        }

        override fun onRefreshHostnames() {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    _isRefreshing.emit(true)
                    excludedIpHolder.forceRefreshAll()
                    _toastMessage.emit("Hostnames refreshed successfully")
                } catch (e: Exception) {
                    _toastMessage.emit("Failed to refresh hostnames: ${e.message}")
                } finally {
                    _isRefreshing.emit(false)
                }
            }
        }

        override fun clearToastMessage() {
            viewModelScope.launch {
                _toastMessage.emit(null)
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
