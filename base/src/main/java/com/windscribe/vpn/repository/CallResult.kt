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

/**
 * Transforms [CallResult] to extract data or throw exception on error.
 *
 * Usage: `val data = callResult.getOrElse { error -> return error }`
 */
inline fun <T> CallResult<T>.getOrElse(
    onError: (CallResult.Error) -> CallResult.Error
): T = when (this) {
    is CallResult.Success -> data
    is CallResult.Error -> throw GetOrElseException(onError(this))
}

/**
 * Folds [CallResult] into a single result type by applying transformation functions.
 *
 * Usage:
 * ```
 * result.fold(
 *     onSuccess = { data -> CallResult.Success(data.transformed()) },
 *     onError = { error -> error }
 * )
 * ```
 */
inline fun <T, R> CallResult<T>.fold(
    onSuccess: (T) -> CallResult<R>,
    onError: (CallResult.Error) -> CallResult<R>
): CallResult<R> = when (this) {
    is CallResult.Success -> onSuccess(data)
    is CallResult.Error -> onError(this)
}

/**
 * Exception for control flow in [getOrElse].
 * @suppress Not intended for direct use.
 */
class GetOrElseException(val error: CallResult.Error) : Exception()
