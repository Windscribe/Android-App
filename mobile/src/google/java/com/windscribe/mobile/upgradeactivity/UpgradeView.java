/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.upgradeactivity;

import androidx.annotation.Nullable;

import com.amazon.device.iap.model.Product;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.google.common.collect.ImmutableList;
import com.windscribe.vpn.billing.WindscribeInAppProduct;

import java.util.List;

public interface UpgradeView {

    UpgradeActivity.BillingType getBillingType();

    void getProducts(List<String> skuList);

    void goBackToMainActivity();

    void hideProgressBar();

    boolean isBillingProcessFinished();

    void onPurchaseCancelled();

    //Called after onPurchaseUpdate returns successful
    void onPurchaseSuccessful(@Nullable List<Purchase> purchases);

    void querySkuDetails(List<QueryProductDetailsParams.Product> products);

    void restorePurchase();

    void setBillingProcessStatus(boolean bProcessFinished);

    void setupPlans(final WindscribeInAppProduct windscribeInAppProduct);

    void showBillingError(String errorMessage);

    void showProgressBar(String message);

    void showToast(String toastText);

    void startPurchaseFlow(ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParams, String accountID);

    void startPurchaseFlow(Product product);

    void openUrlInBrowser(String url);

    void goToSuccessfulUpgrade(Boolean isGhostAccount);
}
