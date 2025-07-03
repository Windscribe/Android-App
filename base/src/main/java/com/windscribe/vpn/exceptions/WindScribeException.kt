/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.exceptions

open class WindScribeException(
    override val message: String?
) : Exception() {
    override fun getLocalizedMessage(): String {
        return message ?: ""
    }
}
