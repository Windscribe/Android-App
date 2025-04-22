package com.windscribe.vpn.exceptions

class WSNetException(message: String, val code: Int): WindScribeException(message) {
    fun getType(): ApiFailure {
        return when(code) {
            1 -> ApiFailure.Network
            2 -> ApiFailure.NoNetwork
            3 -> ApiFailure.IncorrectJsonError
            else -> ApiFailure.AllFallbackFailed
        }
    }
}
sealed class ApiFailure {
    object Network : ApiFailure()
    object NoNetwork : ApiFailure()
    object IncorrectJsonError : ApiFailure()
    object AllFallbackFailed : ApiFailure()
}