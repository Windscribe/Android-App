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
    abstract val dialogMessage: StateFlow<String?>
    abstract val isRefreshing: StateFlow<Boolean>

    open fun onInputTextChange(text: String) {}

    open fun onAddEntry() {}

    open fun onDeleteEntry(entry: ExcludedIpDomain) {}

    open fun onDeleteAll() {}

    open fun onImportFromFile(uri: Uri) {}

    open fun onRefreshHostnames() {}

    open fun clearToastMessage() {}

    open fun clearDialogMessage() {}
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

        private val _dialogMessage = MutableStateFlow<String?>(null)
        override val dialogMessage: StateFlow<String?> = _dialogMessage.asStateFlow()

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

                // Check if it's a non-canonical CIDR and provide helpful feedback
                val cidrCheck = checkNonCanonicalCidr(value)
                if (cidrCheck != null) {
                    val message =
                        buildString {
                            append("Invalid: $value is not a valid network address.\n\n")
                            append("Did you mean:\n")
                            append("• ${cidrCheck.ip}/32 (single host)\n")
                            append("• ${cidrCheck.canonicalIp}/${cidrCheck.prefix} (${cidrCheck.ipCount} IPs)")
                        }
                    _dialogMessage.emit(message)
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
                    val nonCanonicalCidrs = mutableListOf<Pair<String, NonCanonicalCidrInfo>>()

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

                        // Check for non-canonical CIDR before processing
                        val cidrCheck = checkNonCanonicalCidr(value)
                        if (cidrCheck != null) {
                            nonCanonicalCidrs.add(value to cidrCheck)
                            failCount++
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

                    // Build result message
                    if (nonCanonicalCidrs.isNotEmpty()) {
                        // Show detailed error dialog for non-canonical CIDRs
                        val message =
                            buildString {
                                append("Import Results:\n")
                                append("✓ Imported: $successCount\n")
                                if (duplicateCount > 0) {
                                    append("⊘ Skipped (duplicates): $duplicateCount\n")
                                }
                                append("✗ Failed: $failCount\n\n")
                                append("Invalid CIDR notations found:\n")
                                nonCanonicalCidrs.take(5).forEach { (cidr, info) ->
                                    append("\n• $cidr\n")
                                    append("  Should be: ${info.canonicalIp}/${info.prefix}\n")
                                }
                                if (nonCanonicalCidrs.size > 5) {
                                    append("\n...and ${nonCanonicalCidrs.size - 5} more")
                                }
                            }
                        _dialogMessage.emit(message)
                    } else {
                        // Show simple toast for successful import
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
                    }
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

        override fun clearDialogMessage() {
            viewModelScope.launch {
                _dialogMessage.emit(null)
            }
        }

        private data class NonCanonicalCidrInfo(
            val ip: String,
            val prefix: Int,
            val canonicalIp: String,
            val ipCount: String,
        )

        private fun checkNonCanonicalCidr(value: String): NonCanonicalCidrInfo? {
            if (!value.contains("/")) return null

            val parts = value.split("/")
            if (parts.size != 2) return null

            val ip = parts[0]
            val prefix = parts[1].toIntOrNull() ?: return null

            if (!isValidIpAddress(ip) || prefix !in 0..32) return null

            return if (!isCanonicalCidr(ip, prefix)) {
                NonCanonicalCidrInfo(
                    ip = ip,
                    prefix = prefix,
                    canonicalIp = getCanonicalNetworkAddress(ip, prefix),
                    ipCount = getIpCount(prefix),
                )
            } else {
                null
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

            // First check if it's a valid IP and prefix length
            if (!isValidIpAddress(ip) || prefix !in 0..32) return false

            // Check if the IP is canonical (network address) for the given prefix
            return isCanonicalCidr(ip, prefix)
        }

        private fun isCanonicalCidr(
            ip: String,
            prefix: Int,
        ): Boolean {
            // Convert IP to integer
            val ipParts = ip.split(".")
            var ipInt = 0L
            for (i in 0..3) {
                ipInt = (ipInt shl 8) or ipParts[i].toLong()
            }

            // Calculate network mask
            val mask = if (prefix == 0) 0L else (-1L shl (32 - prefix)) and 0xFFFFFFFFL

            // Calculate network address
            val networkAddress = ipInt and mask

            // Check if the given IP matches the network address
            return ipInt == networkAddress
        }

        private fun isValidHostname(value: String): Boolean {
            val hostnameRegex = "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$".toRegex()
            return hostnameRegex.matches(value)
        }

        private fun getCanonicalNetworkAddress(
            ip: String,
            prefix: Int,
        ): String {
            // Convert IP to integer
            val ipParts = ip.split(".")
            var ipInt = 0L
            for (i in 0..3) {
                ipInt = (ipInt shl 8) or ipParts[i].toLong()
            }

            // Calculate network mask
            val mask = if (prefix == 0) 0L else (-1L shl (32 - prefix)) and 0xFFFFFFFFL

            // Calculate network address
            val networkAddress = ipInt and mask

            // Convert back to string
            val octet1 = (networkAddress shr 24) and 0xFF
            val octet2 = (networkAddress shr 16) and 0xFF
            val octet3 = (networkAddress shr 8) and 0xFF
            val octet4 = networkAddress and 0xFF
            return "$octet1.$octet2.$octet3.$octet4"
        }

        private fun getIpCount(prefix: Int): String {
            val count = 1L shl (32 - prefix)
            return when {
                count >= 1_000_000_000 -> "%.1f billion".format(count / 1_000_000_000.0)
                count >= 1_000_000 -> "%.1f million".format(count / 1_000_000.0)
                count >= 1000 -> "%.1f thousand".format(count / 1000.0)
                else -> count.toString()
            }
        }
    }
