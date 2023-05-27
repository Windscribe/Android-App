/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.billing;

import com.android.billingclient.api.Purchase;

import java.util.List;

public class CustomPurchases {

    private final List<Purchase> purchase;

    private final int responseCode;

    CustomPurchases(int responseCode, List<Purchase> purchase) {
        this.responseCode = responseCode;
        this.purchase = purchase;
    }

    public List<Purchase> getPurchase() {
        return purchase;
    }

    public int getResponseCode() {
        return responseCode;
    }
}
