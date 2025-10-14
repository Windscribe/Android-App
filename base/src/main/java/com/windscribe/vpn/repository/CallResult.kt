package com.windscribe.vpn.repository

import com.windscribe.vpn.constants.NetworkErrorCodes
import com.windscribe.vpn.exceptions.ApiFailure
import java.io.Serializable

sealed class CallResult<out T> : Serializable {
    data class Error(val code: Int = -1, val errorMessage: String = "Unexpected error.") :
        CallResult<Nothing>()

    data class Success<out R>(val data: R) : CallResult<R>()
}

fun getNetworkError(code: Int): ApiFailure? {
    return when (code) {
        1, NetworkErrorCodes.ERROR_UNABLE_TO_REACH_API -> ApiFailure.Network
        2 -> ApiFailure.NoNetwork
        3 -> ApiFailure.IncorrectJsonError
        4 -> ApiFailure.AllFallbackFailed
        else -> null
    }
}
