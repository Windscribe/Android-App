/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.billing;

import com.android.billingclient.api.Purchase;

public class CustomPurchase {

    private final Purchase purchase;

    private final int responseCode;

    CustomPurchase(int responseCode, Purchase purchase) {
        this.responseCode = responseCode;
        this.purchase = purchase;
    }

    public Purchase getPurchase() {
        return purchase;
    }

    public int getResponseCode() {
        return responseCode;
    }
}
