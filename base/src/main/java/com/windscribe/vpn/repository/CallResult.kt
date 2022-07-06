package com.windscribe.vpn.repository

import java.io.Serializable

sealed class CallResult<out T> : Serializable {
    data class Error(val code: Int = -1, val errorMessage: String = "Unexpected error.") : CallResult<Nothing>()
    data class Success<out R>(val data: R) : CallResult<R>()
}
