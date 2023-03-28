package com.windscribe.vpn.repository

sealed class RepositoryState<T> {
    data class Loading<T>(val progress: Int = 0) : RepositoryState<T>()
    data class Success<T>(val data: T) : RepositoryState<T>()
    data class Error<T>(val error: String) : RepositoryState<T>()
}