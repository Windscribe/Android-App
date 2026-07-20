package com.windscribe.vpn.commonutils

import com.windscribe.vpn.api.response.GenericResponseClass
import com.windscribe.vpn.constants.NetworkErrorCodes
import com.windscribe.vpn.constants.UserStatusConstants
import com.windscribe.vpn.repository.CallResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

object Ext {
    /**
     * Wraps a suspend API call to catch exceptions (like WindScribeException from checkSession)
     * and convert them to CallResult.Error.
     *
     * Usage: result { apiManager.someApiCall() }
     */
    suspend fun <T> result(block: suspend () -> GenericResponseClass<*, *>): CallResult<T> =
        try {
            block().callResult()
        } catch (e: Exception) {
            CallResult.Error(
                NetworkErrorCodes.ERROR_UNEXPECTED_API_DATA,
                e.message ?: "Unknown error",
            )
        }

    fun CoroutineScope.launchPeriodicAsync(
        repeatMillis: Long,
        action: () -> Unit,
    ) = this.async {
        if (repeatMillis > 0) {
            while (isActive) {
                action()
                delay(repeatMillis)
            }
        } else {
            action()
        }
    }

    fun Long.toLabel(): String =
        when {
            this >= UserStatusConstants.GB_DATA -> {
                String.format(
                    Locale.getDefault(),
                    "%.2f GB",
                    this.toDouble() / UserStatusConstants.GB_DATA,
                )
            }
            this >= UserStatusConstants.MB_DATA -> {
                String.format(
                    Locale.getDefault(),
                    "%.2f MB",
                    this.toDouble() / UserStatusConstants.MB_DATA,
                )
            }
            this >= UserStatusConstants.KB_DATA -> {
                String.format(
                    Locale.getDefault(),
                    "%.2f KB",
                    this.toDouble() / UserStatusConstants.KB_DATA,
                )
            }
            else -> "$this B"
        }

    fun String.maskMiddle(
        front: Int = 4,
        back: Int = 6,
    ): String {
        if (length <= front + back) return "***"
        return "${take(front)}***${takeLast(back)}"
    }
}
