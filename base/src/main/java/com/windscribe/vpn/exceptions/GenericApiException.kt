/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.exceptions

import com.windscribe.vpn.api.response.ApiErrorResponse

class GenericApiException(apiErrorResponse: ApiErrorResponse?) :
        Exception(apiErrorResponse.toString())
