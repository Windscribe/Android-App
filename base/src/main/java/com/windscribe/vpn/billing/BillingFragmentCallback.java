/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.billing;

import androidx.annotation.Nullable;

import com.amazon.device.iap.model.Product;
import com.android.billingclient.api.ProductDetails;

public interface BillingFragmentCallback {

    void onContinuePlanClick(@Nullable ProductDetails productDetails, int selectedIndex);

    void onContinuePlanClick(Product selectedSku);

    void onRestorePurchaseClick();

    void onTenGbFreeClick();

    void onTermPolicyClick();
}