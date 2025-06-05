package com.windscribe.mobile.ui.auth

import androidx.annotation.StringRes

sealed class AuthInputFields {
    object Username : AuthInputFields()
    object Password : AuthInputFields()
    object Email : AuthInputFields()
    object TwoFactor : AuthInputFields()
}

sealed class AuthError {
    open val highlightedFields: List<AuthInputFields> = listOf()

    data class InputError(
        val error: String,
        override val highlightedFields: List<AuthInputFields> = listOf()
    ) : AuthError()

    data class LocalizedInputError(
        @StringRes val error: Int,
        override val highlightedFields: List<AuthInputFields> = listOf()
    ) : AuthError()
}