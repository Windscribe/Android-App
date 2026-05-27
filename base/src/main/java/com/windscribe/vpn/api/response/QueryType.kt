/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response

import androidx.annotation.Keep

@Keep
enum class QueryType(
    val value: Int,
) {
    Account(1),
    Technical(2),
    Sales(3),
    Feedback(4),
    ;

    override fun toString(): String = name
}
