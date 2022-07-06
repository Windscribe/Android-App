/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.billing;

import androidx.annotation.Nullable;

import com.amazon.device.iap.model.Product;
import com.android.billingclient.api.SkuDetails;

public interface BillingFragmentCallback {

    void onContinuePlanClick(@Nullable SkuDetails selectedSku);

    void onContinuePlanClick(Product selectedSku);

    void onRestorePurchaseClick();

    void onTenGbFreeClick();

    void onTermPolicyClick();
}
