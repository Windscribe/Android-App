/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.constants

object NetworkErrorCodes {
    const val ERROR_RESPONSE_CREDENTIAL_FAILURE = 702
    const val ERROR_RESPONSE_SESSION_INVALID = 701
    const val ERROR_ACCOUNT_DELETED = 6002
    const val ERROR_RESPONSE_ARGUMENT_INVALID = 502
    const val ERROR_USER_NAME_ALREADY_TAKEN = 600
    const val ERROR_USER_NAME_ALREADY_IN_USE = 503
    const val ERROR_EMAIL_ALREADY_EXISTS = 1338
    const val ERROR_DISPOSABLE_EMAIL = 1337
    const val ERROR_2FA_REQUIRED = 1340
    const val ERROR_INVALID_2FA = 1341
    const val ERROR_UNABLE_TO_GENERATE_CREDENTIALS = 1700
    const val ERROR_WG_UNABLE_TO_GENERATE_PSK = 1310
    const val ERROR_WG_INVALID_PUBLIC_KEY = 1311
    const val ERROR_UNABLE_TO_SELECT_WIRE_GUARD_IP = 1312
    const val ERROR_WG_KEY_LIMIT_EXCEEDED = 1313
    const val EXPIRED_OR_BANNED_ACCOUNT = 1700

    //Internal app error codes.
    const val ERROR_UNABLE_TO_REACH_API = 30001
    const val ERROR_UNEXPECTED_API_DATA = 30002
    const val ERROR_VALID_CONFIG_NOT_FOUND = 30003
    const val ERROR_INVALID_DNS_ADDRESS = 30004

    const val ERROR_PSK_FAILURE = 30005
}