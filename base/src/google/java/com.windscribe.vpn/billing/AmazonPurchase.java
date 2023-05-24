/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.billing;

import androidx.annotation.NonNull;

public class AmazonPurchase {

    private final String receiptId;

    private final String userId;

    public AmazonPurchase(final String receiptId, final String userId) {
        this.receiptId = receiptId;
        this.userId = userId;
    }

    public String getReceiptId() {
        return receiptId;
    }

    public String getUserId() {
        return userId;
    }

    @NonNull
    @Override
    public String toString() {
        return "AmazonPurchase{" +
                "receiptId='" + receiptId + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}
