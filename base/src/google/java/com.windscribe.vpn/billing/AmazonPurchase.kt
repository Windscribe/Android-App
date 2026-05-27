/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.billing

class AmazonPurchase(
    val receiptId: String,
    val userId: String,
) {
    override fun toString(): String =
        "AmazonPurchase{" +
            "receiptId='" + receiptId + '\'' +
            ", userId='" + userId + '\'' +
            '}'
}
