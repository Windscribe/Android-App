package com.windscribe.vpn.exceptions

import com.windscribe.vpn.repository.CallResult

class InvalidVPNConfigException(val error: CallResult.Error) : Exception() {
    override val message: String
        get() = "Code: ${error.code} error: ${error.errorMessage}"
}