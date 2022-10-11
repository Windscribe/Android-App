/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.billing;

import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;

import java.util.List;

public class CustomProductDetails {

    private final BillingResult billingResult;

    private final List<ProductDetails> productDetails;

    CustomProductDetails(BillingResult billingResult, List<ProductDetails> productDetails) {
        this.billingResult = billingResult;
        this.productDetails = productDetails;
    }

    public BillingResult getBillingResult() {
        return billingResult;
    }

    public List<ProductDetails> getProductDetails() {
        return productDetails;
    }
}
