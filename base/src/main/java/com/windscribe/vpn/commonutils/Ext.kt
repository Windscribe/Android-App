package com.windscribe.vpn.commonutils

import com.windscribe.vpn.api.response.GenericResponseClass
import com.windscribe.vpn.constants.NetworkErrorCodes
import com.windscribe.vpn.repository.CallResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

object Ext {

    /**
     * Wraps a suspend API call to catch exceptions (like WindScribeException from checkSession)
     * and convert them to CallResult.Error.
     *
     * Usage: result { apiManager.someApiCall() }
     */
    suspend fun <T> result(block: suspend () -> GenericResponseClass<*, *>): CallResult<T> {
        return try {
            block().callResult()
        } catch (e: Exception) {
            CallResult.Error(
                NetworkErrorCodes.ERROR_UNEXPECTED_API_DATA,
                e.message ?: "Unknown error"
            )
        }
    }

    fun CoroutineScope.launchPeriodicAsync(
        repeatMillis: Long,
        action: () -> Unit
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
}
