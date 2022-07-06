/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.exceptions

class TooHighLatencyError : Exception() {
    override val message: String
        get() = "Latency > 1000 retrying..."
}
