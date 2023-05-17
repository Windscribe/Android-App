/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.exceptions

import com.windscribe.vpn.api.response.ApiErrorResponse

class GenericApiException(message: String) : Exception(message) {
    constructor(apiErrorResponse: ApiErrorResponse?) : this(apiErrorResponse.toString())
}