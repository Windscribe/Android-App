/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.billing;

import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.SkuDetails;

import java.util.List;

public class CustomSkuDetails {

    private final BillingResult billingResult;

    private final List<SkuDetails> skuDetails;

    CustomSkuDetails(BillingResult billingResult, List<SkuDetails> skuDetails) {
        this.billingResult = billingResult;
        this.skuDetails = skuDetails;
    }

    public BillingResult getBillingResult() {
        return billingResult;
    }

    public List<SkuDetails> getSkuDetails() {
        return skuDetails;
    }
}
